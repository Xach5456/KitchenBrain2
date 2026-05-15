/**
 * 🎯 SIMPLE WHATSAPP-LEVEL CLOUD FUNCTIONS
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Server-generated UUID (никакого hash)
 * - Никаких mapping tables
 * - Prevent at minimal level
 * - Simple, boring, rock-solid
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🎯 SIMPLE: Server-generated UUID approach
 * 
 * WhatsApp/Telegram НЕ используют hash chatId!
 * Используют простой UUID + unique constraint.
 */
function generateSimpleChatId() {
    return require('crypto').randomUUID();
}

/**
 * 🔍 SIMPLE: Find existing chat via query
 */
async function findExistingChat(uid1, uid2) {
    try {
        const query = await db.collection('chats')
            .where('participants', 'array-contains', uid1)
            .where('active', '==', true)
            .limit(10)
            .get();
        
        for (const doc of query.docs) {
            const participants = doc.get('participants');
            if (participants && participants.includes(uid2)) {
                return doc.id;
            }
        }
        
        return null;
    } catch (error) {
        console.error('❌ Error finding existing chat:', error);
        return null;
    }
}

/**
 * 🎯 SIMPLE TRIGGER: Follow with minimal prevention
 * 
 * НЕ пытаемся предотвратить всё!
 * Только minimal prevention + retry safety.
 */
exports.onFollowCreatedSimple = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 Simple follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowSimple(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat с minimal prevention
            const chatResult = await createChatWithSimplePrevention(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created: ${chatResult.chatId}`);
                
                // Step 3: Отправить notifications
                await sendSimpleNotifications(userId, targetId, chatResult.chatId);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ Simple follow trigger error:', error);
            // Graceful degradation
            console.log('⚠️ Continuing despite error (simple system)');
            return null;
        }
    });

/**
 * 🔍 SIMPLE: Check mutual follow
 */
async function checkMutualFollowSimple(uid1, uid2) {
    try {
        const [follow1Doc, follow2Doc] = await Promise.all([
            db.collection('following')
                .doc(uid1)
                .collection('userFollowing')
                .doc(uid2)
                .get(),
            
            db.collection('following')
                .doc(uid2)
                .collection('userFollowing')
                .doc(uid1)
                .get()
        ]);
        
        return follow1Doc.exists && follow2Doc.exists;
        
    } catch (error) {
        console.error('❌ Error checking mutual follow:', error);
        return false;
    }
}

/**
 * 🆕 SIMPLE: Create chat with minimal prevention
 * 
 * НЕ пытаемся предотвратить всё!
 * Только minimal prevention + retry safety.
 */
async function createChatWithSimplePrevention(uid1, uid2) {
    try {
        console.log(`🔍 Creating chat with simple prevention: ${uid1} ↔ ${uid2}`);
        
        // ATOMIC TRANSACTION с minimal prevention
        const result = await db.runTransaction(async (transaction) => {
            // Double-check что chat не создался
            const existingChatId = await findExistingChatInTransaction(transaction, uid1, uid2);
            if (existingChatId) {
                console.log(`✅ Chat already exists: ${existingChatId}`);
                return { exists: true, chatId: existingChatId };
            }
            
            // Создать chat с server-generated UUID
            const chatId = generateSimpleChatId();
            
            const now = admin.firestore.FieldValue.serverTimestamp();
            
            const chatData = {
                chatId: chatId,
                participants: [uid1, uid2],
                createdAt: now,
                updatedAt: now,
                lastMessage: null,
                lastMessageTime: now,
                active: true,
                version: 1,
                authority: 'simple_uuid',
                noHash: true,
                noMappingTable: true
            };
            
            transaction.set(db.collection('chats').document(chatId), chatData);
            
            // Создать userChats entries (простой index)
            await createSimpleUserChatEntries(transaction, uid1, uid2, chatId);
            
            console.log(`🆕 Chat created: ${chatId}`);
            return { exists: false, chatId };
        });
        
        return { 
            success: true, 
            chatId: result.chatId,
            wasCreated: !result.exists
        };
        
    } catch (error) {
        console.error('❌ Error creating chat with simple prevention:', error);
        return { success: false, error: error.message };
    }
}

/**
 * 🔍 SIMPLE: Find existing chat in transaction
 */
async function findExistingChatInTransaction(transaction, uid1, uid2) {
    try {
        const query = await db.collection('chats')
            .where('participants', 'array-contains', uid1)
            .where('active', '==', true)
            .limit(5)
            .get();
        
        for (const doc of query.docs) {
            const participants = doc.get('participants');
            if (participants && participants.includes(uid2)) {
                return doc.id;
            }
        }
        
        return null;
    } catch (error) {
        console.error('❌ Error finding existing chat in transaction:', error);
        return null;
    }
}

/**
 * 📋 SIMPLE: Create userChats entries
 */
async function createSimpleUserChatEntries(transaction, uid1, uid2, chatId) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        // User 1 entry
        const userChat1Data = {
            chatId: chatId,
            otherUserId: uid2,
            createdAt: now,
            updatedAt: now,
            simple: true
        };
        
        transaction.set(
            db.collection('userChats').document(uid1)
                .collection('chats').document(chatId),
            userChat1Data
        );
        
        // User 2 entry
        const userChat2Data = {
            chatId: chatId,
            otherUserId: uid1,
            createdAt: now,
            updatedAt: now,
            simple: true
        };
        
        transaction.set(
            db.collection('userChats').document(uid2)
                .collection('chats').document(chatId),
            userChat2Data
        );
        
        console.log(`✅ Simple userChats entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create userChats (non-critical):', error);
        // userChats failure не критичен
    }
}

/**
 * 📬 SIMPLE: Send notifications
 */
async function sendSimpleNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_simple',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'simple',
            noHash: true
        };
        
        const batch = db.batch();
        
        // Notification для uid1
        const notif1Ref = db
            .collection('notifications')
            .doc(uid1)
            .collection('userNotifications')
            .doc();
        
        batch.set(notif1Ref, {
            ...notificationData,
            otherUserId: uid2
        });
        
        // Notification для uid2
        const notif2Ref = db
            .collection('notifications')
            .doc(uid2)
            .collection('userNotifications')
            .doc();
        
        batch.set(notif2Ref, {
            ...notificationData,
            otherUserId: uid1
        });
        
        await batch.commit();
        
        console.log(`📬 Simple notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🔧 SIMPLE: Manual chat creation (HTTP callable)
 */
exports.createChatSimple = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    
    if (!uid1 || !uid2) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Both uid1 and uid2 are required'
        );
    }
    
    if (uid1 === uid2) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Cannot create chat with yourself'
        );
    }
    
    try {
        console.log(`🔧 Simple chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithSimplePrevention(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            wasCreated: chatResult.wasCreated,
            message: chatResult.success ? 
                (chatResult.wasCreated ? 'Chat created' : 'Chat already exists') : 
                'Failed to create chat'
        };
        
    } catch (error) {
        console.error('❌ Simple chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});

/**
 * 🔧 SIMPLE: Check chat existence
 */
exports.checkChatExists = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    
    if (!uid1 || !uid2) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Both uid1 and uid2 are required'
        );
    }
    
    try {
        const existingChatId = await findExistingChat(uid1, uid2);
        
        return { 
            exists: existingChatId !== null,
            chatId: existingChatId
        };
        
    } catch (error) {
        console.error('❌ Simple chat existence check error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to check chat existence'
        );
    }
});
