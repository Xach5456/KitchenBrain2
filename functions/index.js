/**
 * Firebase Cloud Functions for KitchenBrain
 * 
 * BACKEND-DRIVEN ARCHITECTURE:
 * - Client emits events only
 * - Backend validates and executes
 * - Firestore is storage, not logic
 * 
 * SECURITY: All sensitive operations run server-side
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
admin.initializeApp();

const db = admin.firestore();

/**
 * 🔥 HELPER: Generate consistent chatId (server-side)
 */
function generateChatId(userA, userB) {
  return userA < userB 
    ? `${userA}_${userB}` 
    : `${userB}_${userA}`;
}

/**
 * 🔥 HELPER: Check if follow is mutual (server-side verification)
 */
async function checkMutualFollow(userId1, userId2) {
  const user1FollowsUser2 = await db
    .collection('following')
    .doc(userId1)
    .collection('userFollowing')
    .doc(userId2)
    .get();
  
  const user2FollowsUser1 = await db
    .collection('following')
    .doc(userId2)
    .collection('userFollowing')
    .doc(userId1)
    .get();
  
  return user1FollowsUser2.exists && user2FollowsUser1.exists;
}

/**
 * 🔥 CLOUD FUNCTION: Process MUTUAL_FOLLOW event (BACKEND AUTHORITY)
 * 
 * BACKEND-DRIVEN ARCHITECTURE:
 * 1. Client creates event document
 * 2. This function triggers
 * 3. Backend validates mutual follow
 * 4. Backend creates chat atomically
 * 5. Client observes result
 * 
 * This is the PROPER way - client NEVER creates chats directly
 */
exports.onMutualFollowEvent = functions.firestore
  .document('events/{eventId}')
  .onCreate(async (snap, context) => {
    const event = snap.data();
    const eventId = context.params.eventId;

    functions.logger.info(`🔔 Event created: ${eventId}`, { event });

    // Only process MUTUAL_FOLLOW events
    if (event.type !== 'MUTUAL_FOLLOW') {
      functions.logger.info(`⏳ Ignoring non-MUTUAL_FOLLOW event: ${event.type}`);
      return null;
    }

    const userA = event.fromUserId;
    const userB = event.toUserId;

    if (!userA || !userB) {
      functions.logger.error(`❌ Invalid event - missing userIds: ${eventId}`);
      await snap.ref.update({ 
        status: 'failed',
        reason: 'missing_user_ids'
      });
      return null;
    }

    try {
      // Step 1: Verify mutual follow (SERVER-SIDE)
      functions.logger.info(`🔍 Verifying mutual follow: ${userA} ↔ ${userB}`);
      const isMutual = await checkMutualFollow(userA, userB);

      if (!isMutual) {
        functions.logger.info(`⏳ Not mutual follow yet: ${userA} ↔ ${userB}`);
        await snap.ref.update({ 
          status: 'failed',
          reason: 'not_mutual'
        });
        return null;
      }

      functions.logger.info(`✅ Mutual follow confirmed: ${userA} ↔ ${userB}`);

      // Step 2: Generate stable chatId (SERVER-SIDE)
      const chatId = generateChatId(userA, userB);
      const chatRef = db.collection('chats').doc(chatId);

      // Step 3: Create chat atomically (TRANSACTION)
      await db.runTransaction(async (transaction) => {
        const chatDoc = await transaction.get(chatRef);

        if (chatDoc.exists) {
          functions.logger.info(`✅ Chat already exists: ${chatId}`);
          return;
        }

        functions.logger.info(`📝 Creating chat: ${chatId}`);

        const now = admin.firestore.FieldValue.serverTimestamp();

        transaction.set(chatRef, {
          chatId: chatId,
          participants: [userA, userB],
          createdAt: now,
          updatedAt: now,
          lastMessage: null,
          lastMessageTime: null,
          lastMessageSenderId: null,
          unreadCount: {
            [userA]: 0,
            [userB]: 0
          }
        });
      });

      // Step 4: Mark event as processed
      await snap.ref.update({
        status: 'processed',
        chatId: chatId,
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      functions.logger.info(`🎉 Event processed successfully: ${eventId} → chat: ${chatId}`);

      return { success: true, chatId };
    } catch (error) {
      functions.logger.error(`❌ Error processing event ${eventId}:`, error);
      
      await snap.ref.update({
        status: 'failed',
        reason: error.message || 'unknown_error'
      }).catch(e => {
        functions.logger.error(`❌ Failed to update event status:`, e);
      });

      throw new functions.https.HttpsError('internal', 'Failed to process mutual follow event');
    }
  });

/**
 * 🔥 CLOUD FUNCTION: Auto-create chat on mutual follow
 * 
 * Trigger: When a document is created in following/{userId}/userFollowing/{targetId}
 * Security: Server-side verification - client CANNOT fake this
 * 
 * Flow:
 * 1. User A follows User B
 * 2. This function triggers
 * 3. Check if User B already follows User A (mutual)
 * 4. If mutual → Create chat atomically
 * 5. Notify both users
 */
exports.onFollowCreated = functions.firestore
  .document('following/{userId}/userFollowing/{targetId}')
  .onCreate(async (snapshot, context) => {
    const userId = context.params.userId;
    const targetId = context.params.targetId;

    functions.logger.info(`🔔 Follow created: ${userId} → ${targetId}`);

    try {
      // Step 1: Check if target user already follows userId (mutual follow)
      const targetFollowsUserRef = db
        .collection('following')
        .doc(targetId)
        .collection('userFollowing')
        .doc(userId);

      const targetFollowsUserDoc = await targetFollowsUserRef.get();

      if (!targetFollowsUserDoc.exists) {
        functions.logger.info(`⏳ Not mutual yet - ${targetId} doesn't follow ${userId}`);
        return null;
      }

      functions.logger.info(`🎉 MUTUAL FOLLOW detected: ${userId} ↔ ${targetId}`);

      // Step 2: Generate consistent chat ID (alphabetically sorted)
      const chatId = userId < targetId 
        ? `${userId}_${targetId}` 
        : `${targetId}_${userId}`;

      // Step 3: Create chat using ATOMIC TRANSACTION
      const chatRef = db.collection('chats').doc(chatId);

      await db.runTransaction(async (transaction) => {
        const chatDoc = await transaction.get(chatRef);

        // Prevent duplicate chat creation
        if (chatDoc.exists) {
          functions.logger.info(`✅ Chat already exists: ${chatId}`);
          return;
        }

        functions.logger.info(`📝 Creating chat via transaction: ${chatId}`);

        const now = admin.firestore.FieldValue.serverTimestamp();

        const chatData = {
          chatId: chatId,
          participants: [userId, targetId],
          createdAt: now,
          updatedAt: now,
          lastMessage: '',
          lastMessageTime: now,
          lastMessageSender: null,
          unreadCount: {
            [userId]: 0,
            [targetId]: 0
          }
        };

        transaction.set(chatRef, chatData);
      });

      functions.logger.info(`✅ Chat created successfully: ${chatId}`);

      // Step 4: Notify both users (set flag for real-time listener)
      const notifyUser1 = db.collection('users').doc(userId).update({
        newMutualFollowUserId: targetId,
        lastMutualFollow: admin.firestore.FieldValue.serverTimestamp()
      });

      const notifyUser2 = db.collection('users').doc(targetId).update({
        newMutualFollowUserId: userId,
        lastMutualFollow: admin.firestore.FieldValue.serverTimestamp()
      });

      await Promise.all([notifyUser1, notifyUser2]);

      functions.logger.info(`✅ Both users notified of mutual follow`);

      return { success: true, chatId };
    } catch (error) {
      functions.logger.error(`❌ Error in onFollowCreated:`, error);
      throw new functions.https.HttpsError('internal', 'Failed to create chat on mutual follow');
    }
  });

/**
 * 🔥 CLOUD FUNCTION: Cleanup chat on mutual unfollow (optional)
 * 
 * Instagram keeps chats even after unfollow, but if you want to delete:
 * Uncomment this function and deploy
 */
/*
exports.onMutualUnfollow = functions.firestore
  .document('following/{userId}/userFollowing/{targetId}')
  .onDelete(async (snapshot, context) => {
    const userId = context.params.userId;
    const targetId = context.params.targetId;

    functions.logger.info(`👋 Unfollow: ${userId} → ${targetId}`);

    // Option 1: Keep chat (Instagram behavior) - DO NOTHING
    functions.logger.info(`📱 Keeping chat (Instagram behavior)`);

    // Option 2: Delete chat (comment out above, uncomment below)
    /*
    try {
      const chatId = userId < targetId 
        ? `${userId}_${targetId}` 
        : `${targetId}_${userId}`;

      const chatRef = db.collection('chats').doc(chatId);
      const chatDoc = await chatRef.get();

      if (chatDoc.exists) {
        // Delete all messages first
        const messagesRef = chatRef.collection('messages');
        const messagesSnapshot = await messagesRef.get();

        const batch = db.batch();
        messagesSnapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
        });

        await batch.commit();
        await chatRef.delete();

        functions.logger.info(`🗑️ Chat deleted: ${chatId}`);
      }
    } catch (error) {
      functions.logger.error(`❌ Error cleaning up chat:`, error);
    }
    * /

    return null;
  });
*/

/**
 * 🔥 HTTP CALLABLE: Create chat with server-side validation
 * 
 * This is a backup method if Cloud Function trigger fails
 * Can be called from client, but validates mutual follow server-side
 */
exports.createChatSecure = functions.https.onCall(async (data, context) => {
  // Authentication check
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  const userId = context.auth.uid;
  const targetId = data.targetId;

  if (!targetId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'targetId is required'
    );
  }

  if (userId === targetId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Cannot create chat with yourself'
    );
  }

  try {
    // SERVER-SIDE: Verify mutual follow
    const userFollowsTargetRef = db
      .collection('following')
      .doc(userId)
      .collection('userFollowing')
      .doc(targetId);

    const targetFollowsUserRef = db
      .collection('following')
      .doc(targetId)
      .collection('userFollowing')
      .doc(userId);

    const [userFollowsTargetDoc, targetFollowsUserDoc] = await Promise.all([
      userFollowsTargetRef.get(),
      targetFollowsUserRef.get()
    ]);

    if (!userFollowsTargetDoc.exists || !targetFollowsUserDoc.exists) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Chat requires mutual follow'
      );
    }

    // Generate chat ID
    const chatId = userId < targetId 
      ? `${userId}_${targetId}` 
      : `${targetId}_${userId}`;

    // Create chat with transaction
    const chatRef = db.collection('chats').doc(chatId);

    await db.runTransaction(async (transaction) => {
      const chatDoc = await transaction.get(chatRef);

      if (chatDoc.exists) {
        return { exists: true };
      }

      const now = admin.firestore.FieldValue.serverTimestamp();

      transaction.set(chatRef, {
        chatId: chatId,
        participants: [userId, targetId],
        createdAt: now,
        updatedAt: now,
        lastMessage: '',
        lastMessageTime: now,
        lastMessageSender: null,
        unreadCount: {
          [userId]: 0,
          [targetId]: 0
        }
      });
    });

    functions.logger.info(`✅ Secure chat created: ${chatId}`);

    return { 
      success: true, 
      chatId: chatId,
      message: 'Chat created successfully'
    };
  } catch (error) {
    functions.logger.error(`❌ Error in createChatSecure:`, error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to create chat'
    );
  }
});
