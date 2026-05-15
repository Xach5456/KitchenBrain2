/**
 * 🎯 WHATSAPP-LEVEL CLOUD FUNCTIONS - Minimal logic, maximum reliability
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Single write path
 * - Deterministic pairKey
 * - Simple transaction
 * - Client-side tolerant UI
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔨 Deterministic pairKey (единственная логика)
 */
function generateDeterministicPairKey(uid1, uid2) {
    const sorted = [uid1, uid2].sort();
    return `${sorted[0]}_${sorted[1]}`;
}

/**
 * 🔍 WHATSAPP-LEVEL: Check mutual follow
 */
async function checkMutualFollowWhatsApp(uid1, uid2) {
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
 * 🎯 WHATSAPP-LEVEL TRIGGER: Single write path
 * 
 * Никаких deduplication, reconciliation, idempotent retries!
 * Только простая deterministic логика.
 */
exports.onFollowCreatedWhatsApp = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 WhatsApp-level follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowWhatsApp(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat (single write path)
            const chatResult = await createChatWithSinglePath(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created: ${chatResult.chatId}`);
                
                // Step 3: Отправить notifications
                await sendWhatsAppNotifications(userId, targetId, chatResult.chatId);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ WhatsApp-level follow trigger error:', error);
            // Graceful degradation
            console.log('⚠️ Continuing despite error (WhatsApp-level system)');
            return null;
        }
    });

/**
 * 🆕 WHATSAPP-LEVEL: Create chat with single write path
 * 
 * Никаких deduplication, reconciliation, idempotent retries!
 * Только простая deterministic логика.
 */
async function createChatWithSinglePath(uid1, uid2) {
    try {
        console.log(`🔍 Creating chat with single path: ${uid1} ↔ ${uid2}`);
        
        // ✅ SINGLE write path: deterministic pairKey + transaction
        const chatId = generateDeterministicPairKey(uid1, uid2);
        
        const result = await db.runTransaction(async (transaction) => {
            const chatRef = db.collection('chats').document(chatId);
            const snapshot = await transaction.get(chatRef);
            
            if (!snapshot.exists) {
                // Создать chat atomically
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
                    authority: 'whatsapp_level',
                    minimal: true
                };
                
                transaction.set(chatRef, chatData);
                
                // Простые userChats entries
                await createSimpleUserChatEntries(transaction, uid1, uid2, chatId);
                
                console.log(`🆕 Chat created: ${chatId}`);
                return { exists: false, chatId };
            } else {
                // Валидировать существующий chat
                const participants = snapshot.get('participants');
                const participantsSet = new Set(participants);
                const expectedSet = new Set([uid1, uid2]);
                
                if (participantsSet && participantsSet.size === 2 && 
                    participantsSet.has(uid1) && participantsSet.has(uid2)) {
                    console.log(`✅ Chat exists: ${chatId}`);
                    return { exists: true, chatId };
                } else {
                    throw new Error('Corrupted chat data');
                }
            }
        });
        
        return { 
            success: true, 
            chatId: result.chatId, 
            wasCreated: !result.exists
        };
        
    } catch (error) {
        console.error('❌ Error creating chat with single path:', error);
        return { success: false, error: error.message };
    }
}

/**
 * 📋 WHATSAPP-LEVEL: Simple userChats entries
 */
async function createSimpleUserChatEntries(transaction, uid1, uid2, chatId) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        // User 1 entry
        transaction.set(
            db.collection('userChats')
                .document(uid1)
                .collection('chats')
                .document(chatId),
            {
                chatId: chatId,
                otherUserId: uid2,
                createdAt: now,
                updatedAt: now
            }
        );
        
        // User 2 entry
        transaction.set(
            db.collection('userChats')
                .document(uid2)
                .collection('chats')
                .document(chatId),
            {
                chatId: chatId,
                otherUserId: uid1,
                createdAt: now,
                updatedAt: now
            }
        );
        
        console.log(`✅ Simple userChats entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create userChats (non-critical):', error);
        // userChats failure не критичен
    }
}

/**
 * 📬 WHATSAPP-LEVEL: Send notifications
 */
async function sendWhatsAppNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_whatsapp',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'whatsapp_level',
            minimal: true
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
        
        console.log(`📬 WhatsApp notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🔧 WHATSAPP-LEVEL: Manual chat creation (HTTP callable)
 */
exports.createChatWhatsApp = functions.https.onCall(async (data, context) => {
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
        console.log(`🔧 WhatsApp-level chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithSinglePath(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            wasCreated: chatResult.wasCreated,
            message: chatResult.success ? 
                (chatResult.wasCreated ? 'Chat created' : 'Chat already exists') : 
                'Failed to create chat'
        };
        
    } catch (error) {
        console.error('❌ WhatsApp-level chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});

/**
 * 🔧 WHATSAPP-LEVEL: Get chat ID
 */
exports.getChatId = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    
    if (!uid1 || !uid2) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Both uid1 and uid2 are required'
        );
    }
    
    try {
        const chatId = generateDeterministicPairKey(uid1, uid2);
        
        return { 
            chatId: chatId,
            whatsapp_level: true,
            minimal: true
        };
        
    } catch (error) {
        console.error('❌ WhatsApp-level get chat ID error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to generate chat ID'
        );
    }
});

/**
 * 🔧 WHATSAPP-LEVEL: Get chat participants
 */
exports.getChatParticipants = functions.https.onCall(async (data, context) => {
    const { chatId } = data;
    
    if (!chatId) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'chatId is required'
        );
    }
    
    try {
        const doc = await db.collection('chats').document(chatId).get();
        
        if (!doc.exists) {
            return { exists: false };
        }
        
        const participants = doc.get('participants');
        
        if (participants && participants.length === 2) {
            return { 
                exists: true,
                participants: participants,
                uid1: participants[0],
                uid2: participants[1]
            };
        } else {
            return { 
                exists: true,
                error: 'Invalid participants data'
            };
        }
        
    } catch (error) {
        console.error('❌ WhatsApp-level get chat participants error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to get chat participants'
        );
    }
});
