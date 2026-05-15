/**
 * 🔥 SERVER-AUTHORITATIVE CHAT FUNCTIONS (Level 3 Production)
 * 
 * ARCHITECTURE PRINCIPLE:
 * - Client requests actions ONLY
 * - Server validates EVERYTHING
 * - Server decides business logic
 * - Server writes data atomically
 * - Client observes state
 * 
 * This is WhatsApp/Instagram level architecture
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔥 HELPER: Generate consistent chatId
 */
function generateChatId(userA, userB) {
  return userA < userB 
    ? `${userA}_${userB}` 
    : `${userB}_${userA}`;
}

/**
 * 🔥 HELPER: Check if follow is mutual
 */
async function checkMutualFollow(userId1, userId2) {
  const [user1FollowsUser2, user2FollowsUser1] = await Promise.all([
    db.collection('following')
      .doc(userId1)
      .collection('userFollowing')
      .doc(userId2)
      .get(),
    
    db.collection('following')
      .doc(userId2)
      .collection('userFollowing')
      .doc(userId1)
      .get()
  ]);
  
  return user1FollowsUser2.exists && user2FollowsUser1.exists;
}

/**
 * 🔥 SERVER-AUTHORITATIVE: Follow User + Auto-Create Chat if Mutual
 * 
 * THIS IS THE ONLY ENTRY POINT for follow + chat creation
 * Client CANNOT bypass this logic
 * 
 * Flow:
 * 1. Client calls this function
 * 2. Server validates authentication
 * 3. Server creates follow relationship
 * 4. Server checks if mutual
 * 5. If mutual → Server creates chat atomically
 * 6. Server returns result
 * 
 * @param {string} targetUserId - User to follow
 * @returns {object} { status: "followed" | "mutual", chatId?: string }
 */
exports.followUser = functions.https.onCall(async (data, context) => {
  // 1. VERIFY AUTHENTICATION (server-side)
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated to follow'
    );
  }
  
  const userId = context.auth.uid;
  const targetUserId = data.targetUserId;
  
  if (!targetUserId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'targetUserId is required'
    );
  }
  
  if (userId === targetUserId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Cannot follow yourself'
    );
  }
  
  functions.logger.info(`📤 Follow request: ${userId} → ${targetUserId}`);
  
  try {
    // 2. CREATE FOLLOW RELATIONSHIP (server-side)
    const batch = db.batch();
    
    // User follows target
    const followingRef = db
      .collection('following')
      .doc(userId)
      .collection('userFollowing')
      .doc(targetUserId);
    
    batch.set(followingRef, {
      userId: targetUserId,
      followedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Target has user as follower
    const followersRef = db
      .collection('following')
      .doc(targetUserId)
      .collection('userFollowers')
      .doc(userId);
    
    batch.set(followersRef, {
      userId: userId,
      followedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Commit follow relationship
    await batch.commit();
    
    functions.logger.info(`✅ Follow created: ${userId} → ${targetUserId}`);
    
    // 3. CHECK IF MUTUAL FOLLOW (server-side verification)
    const isMutual = await checkMutualFollow(userId, targetUserId);
    
    if (!isMutual) {
      functions.logger.info(`⏳ Not mutual yet: ${targetUserId} doesn't follow ${userId}`);
      
      return {
        status: 'followed',
        mutual: false,
        message: 'Follow successful'
      };
    }
    
    functions.logger.info(`🎉 Mutual follow detected: ${userId} ↔ ${targetUserId}`);
    
    // 4. CREATE CHAT (server authority - atomic transaction)
    const chatId = generateChatId(userId, targetUserId);
    const chatRef = db.collection('chats').doc(chatId);
    
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
        participants: [userId, targetUserId],
        createdAt: now,
        updatedAt: now,
        lastMessage: null,
        lastMessageTime: null,
        lastMessageSenderId: null,
        unreadCount: {
          [userId]: 0,
          [targetUserId]: 0
        }
      });
    });
    
    functions.logger.info(`🎉 Chat created: ${chatId}`);
    
    // 5. RETURN RESULT (deterministic)
    return {
      status: 'mutual',
      mutual: true,
      chatId: chatId,
      message: 'Mutual follow - chat created'
    };
    
  } catch (error) {
    functions.logger.error(`❌ Error in followUser:`, error);
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to process follow request'
    );
  }
});

/**
 * 🔥 SERVER-AUTHORITATIVE: Unfollow User
 * 
 * @param {string} targetUserId - User to unfollow
 * @returns {object} { status: "unfollowed" }
 */
exports.unfollowUser = functions.https.onCall(async (data, context) => {
  // 1. VERIFY AUTHENTICATION
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }
  
  const userId = context.auth.uid;
  const targetUserId = data.targetUserId;
  
  if (!targetUserId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'targetUserId is required'
    );
  }
  
  try {
    // 2. DELETE FOLLOW RELATIONSHIP
    const batch = db.batch();
    
    const followingRef = db
      .collection('following')
      .doc(userId)
      .collection('userFollowing')
      .doc(targetUserId);
    
    const followersRef = db
      .collection('following')
      .doc(targetUserId)
      .collection('userFollowers')
      .doc(userId);
    
    batch.delete(followingRef);
    batch.delete(followersRef);
    
    await batch.commit();
    
    functions.logger.info(`✅ Unfollowed: ${userId} → ${targetUserId}`);
    
    // NOTE: Chat is NOT deleted (Instagram/WhatsApp behavior)
    // Users can still see message history
    
    return {
      status: 'unfollowed',
      message: 'Unfollow successful'
    };
    
  } catch (error) {
    functions.logger.error(`❌ Error in unfollowUser:`, error);
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to process unfollow request'
    );
  }
});

/**
 * 🔥 SERVER-AUTHORITATIVE: Check Follow Status
 * 
 * @param {string} targetUserId - User to check
 * @returns {object} { following: boolean, followers: boolean, mutual: boolean }
 */
exports.checkFollowStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }
  
  const userId = context.auth.uid;
  const targetUserId = data.targetUserId;
  
  if (!targetUserId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'targetUserId is required'
    );
  }
  
  try {
    const [iFollowThem, theyFollowMe] = await Promise.all([
      db.collection('following')
        .doc(userId)
        .collection('userFollowing')
        .doc(targetUserId)
        .get(),
      
      db.collection('following')
        .doc(userId)
        .collection('userFollowers')
        .doc(targetUserId)
        .get()
    ]);
    
    return {
      following: iFollowThem.exists,
      followers: theyFollowMe.exists,
      mutual: iFollowThem.exists && theyFollowMe.exists
    };
    
  } catch (error) {
    functions.logger.error(`❌ Error in checkFollowStatus:`, error);
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to check follow status'
    );
  }
});

/**
 * 🔥 SERVER-AUTHORITATIVE: Create Chat (Manual - for admin/testing)
 * 
 * @param {string} targetUserId - Other participant
 * @returns {object} { chatId: string }
 */
exports.createChat = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }
  
  const userId = context.auth.uid;
  const targetUserId = data.targetUserId;
  
  if (!targetUserId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'targetUserId is required'
    );
  }
  
  // VERIFY MUTUAL FOLLOW (server authority)
  const isMutual = await checkMutualFollow(userId, targetUserId);
  
  if (!isMutual) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Chat requires mutual follow'
    );
  }
  
  const chatId = generateChatId(userId, targetUserId);
  const chatRef = db.collection('chats').doc(chatId);
  
  await db.runTransaction(async (transaction) => {
    const chatDoc = await transaction.get(chatRef);
    
    if (chatDoc.exists) {
      return { exists: true };
    }
    
    const now = admin.firestore.FieldValue.serverTimestamp();
    
    transaction.set(chatRef, {
      chatId: chatId,
      participants: [userId, targetUserId],
      createdAt: now,
      updatedAt: now,
      lastMessage: null,
      lastMessageTime: null,
      lastMessageSenderId: null
    });
  });
  
  return { chatId };
});
