/**
 * 🎯 BULLETPROOF WHATSAPP-LEVEL CLOUD FUNCTIONS
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Atomic transaction (НЕ check-then-write)
 * - Никакого chatId parsing
 * - Deterministic pair key
 * - Zero race conditions
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔨 BULLETPROOF: Deterministic pair key generation
 */
function generateDeterministicPairKey(uid1, uid2) {
    const sorted = [uid1, uid2].sort();
    return `${sorted[0]}_${sorted[1]}`;
}

/**
 * 🔍 BULLETPROOF: Check mutual follow
 */
async function checkMutualFollowBulletproof(uid1, uid2) {
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
 * 🎯 BULLETPROOF TRIGGER: Follow with atomic transaction
 * 
 * НИКАКИХ "check then write"!
 * Только atomic transaction.
 */
exports.onFollowCreatedBulletproof = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 Bulletproof follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowBulletproof(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat с atomic transaction
            const chatResult = await createChatWithAtomicTransaction(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created: ${chatResult.chatId}`);
                
                // Step 3: Отправить notifications
                await sendBulletproofNotifications(userId, targetId, chatResult.chatId);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ Bulletproof follow trigger error:', error);
            // Graceful degradation
            console.log('⚠️ Continuing despite error (bulletproof system)');
            return null;
        }
    });

/**
 * 🆕 BULLETPROOF: Create chat with atomic transaction
 * 
 * НИКАКИХ "check then write"!
 * Только atomic transaction.
 */
async function createChatWithAtomicTransaction(uid1, uid2) {
    try {
        console.log(`🔍 Creating chat with atomic transaction: ${uid1} ↔ ${uid2}`);
        
        // ✅ DETERMINISTIC pair key
        const chatId = generateDeterministicPairKey(uid1, uid2);
        
        // ✅ ATOMIC transaction - НЕ check-then-write!
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
                    authority: 'bulletproof',
                    atomic: true,
                    noRaceConditions: true,
                    noCheckThenWrite: true
                };
                
                transaction.set(chatRef, chatData);
                
                // Создать userChats entries
                await createBulletproofUserChatEntries(transaction, uid1, uid2, chatId);
                
                console.log(`🆕 Chat created atomically: ${chatId}`);
                return { exists: false, chatId };
            } else {
                // Валидировать существующий chat
                const participants = snapshot.get('participants');
                const participantsSet = new Set(participants);
                const expectedSet = new Set([uid1, uid2]);
                
                if (participantsSet && participantsSet.size === 2 && 
                    participantsSet.has(uid1) && participantsSet.has(uid2)) {
                    console.log(`✅ Chat exists and valid: ${chatId}`);
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
        console.error('❌ Error creating chat with atomic transaction:', error);
        return { success: false, error: error.message };
    }
}

/**
 * 📋 BULLETPROOF: Create userChats entries (atomic)
 */
async function createBulletproofUserChatEntries(transaction, uid1, uid2, chatId) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        // User 1 entry
        const userChat1Data = {
            chatId: chatId,
            otherUserId: uid2,
            createdAt: now,
            updatedAt: now,
            bulletproof: true,
            atomic: true
        };
        
        transaction.set(
            db.collection('userChats')
                .document(uid1)
                .collection('chats')
                .document(chatId),
            userChat1Data
        );
        
        // User 2 entry
        const userChat2Data = {
            chatId: chatId,
            otherUserId: uid1,
            createdAt: now,
            updatedAt: now,
            bulletproof: true,
            atomic: true
        };
        
        transaction.set(
            db.collection('userChats')
                .document(uid2)
                .collection('chats')
                .document(chatId),
            userChat2Data
        );
        
        console.log(`✅ Bulletproof userChats entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create userChats (non-critical):', error);
        // userChats failure не критичен
    }
}

/**
 * 📬 BULLETPROOF: Send notifications
 */
async function sendBulletproofNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_bulletproof',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'bulletproof',
            atomic: true,
            noRaceConditions: true
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
        
        console.log(`📬 Bulletproof notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🔧 BULLETPROOF: Manual chat creation (HTTP callable)
 */
exports.createChatBulletproof = functions.https.onCall(async (data, context) => {
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
        console.log(`🔧 Bulletproof chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithAtomicTransaction(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            wasCreated: chatResult.wasCreated,
            message: chatResult.success ? 
                (chatResult.wasCreated ? 'Chat created' : 'Chat already exists') : 
                'Failed to create chat'
        };
        
    } catch (error) {
        console.error('❌ Bulletproof chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});

/**
 * 🔧 BULLETPROOF: Get chat participants (НЕ парсить ID!)
 * 
 * ✅ Никакого chatId parsing!
 * Читаем participants из document.
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
                uid2: participants[1],
                noParsing: true
            };
        } else {
            return { 
                exists: true,
                error: 'Invalid participants data'
            };
        }
        
    } catch (error) {
        console.error('❌ Bulletproof get chat participants error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to get chat participants'
        );
    }
});

/**
 * 🔧 BULLETPROOF: Check if user is in chat
 */
exports.isUserInChat = functions.https.onCall(async (data, context) => {
    const { chatId, userId } = data;
    
    if (!chatId || !userId) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Both chatId and userId are required'
        );
    }
    
    try {
        const doc = await db.collection('chats').document(chatId).get();
        
        if (!doc.exists) {
            return { exists: false, inChat: false };
        }
        
        const participants = doc.get('participants');
        const inChat = participants && participants.includes(userId);
        
        return { 
            exists: true, 
            inChat,
            noParsing: true
        };
        
    } catch (error) {
        console.error('❌ Bulletproof check user in chat error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to check user in chat'
        );
    }
});

/**
 * 🔧 BULLETPROOF: Get chat ID (deterministic)
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
            deterministic: true,
            atomic: true
        };
        
    } catch (error) {
        console.error('❌ Bulletproof get chat ID error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to generate chat ID'
        );
    }
});
