/**
 * 🎯 PRODUCTION WHATSAPP-LEVEL CLOUD FUNCTIONS
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Никаких hash truncation (collision risk)
 * - Простая детерминированная генерация
 * - Server-side authority только
 * - Idempotency по design
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔥 PRODUCTION: Simple deterministic chatId (NO HASH TRUNCATION)
 * 
 * WhatsApp НЕ использует hash truncation!
 * Использует простую строковую конкатенацию.
 */
function generateProductionChatId(uid1, uid2) {
    // 1. Сортировать для консистентности
    const sorted = [uid1, uid2].sort();
    
    // 2. Простая конкатенация (как WhatsApp)
    // НЕ использовать hash truncation!
    return `${sorted[0]}_${sorted[1]}`;
}

/**
 * 🔥 PRODUCTION: Idempotency key
 * 
 * Гарантирует что операция выполнится только один раз
 */
function createIdempotencyKey(uid1, uid2, operation) {
    const chatId = generateProductionChatId(uid1, uid2);
    return `${operation}_${chatId}`;
}

/**
 * 🎯 PRODUCTION TRIGGER: Follow with server-side authority
 * 
 * SINGLE WRITER RULE: Только Cloud Functions создают чаты!
 */
exports.onFollowCreatedProduction = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 Production follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowProduction(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat с idempotency
            const chatResult = await createChatWithIdempotency(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created: ${chatResult.chatId}`);
                
                // Step 3: Отправить notifications
                await sendProductionNotifications(userId, targetId, chatResult.chatId);
            } else {
                console.log(`⚠️ Chat already exists: ${chatResult.chatId}`);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ Production follow trigger error:', error);
            throw error;
        }
    });

/**
 * 🔍 PRODUCTION: Check mutual follow
 */
async function checkMutualFollowProduction(uid1, uid2) {
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
 * 🆕 PRODUCTION: Create chat with idempotency
 */
async function createChatWithIdempotency(uid1, uid2) {
    const chatId = generateProductionChatId(uid1, uid2);
    const idempotencyKey = createIdempotencyKey(uid1, uid2, 'chat_creation');
    
    console.log(`🔍 Creating chat with idempotency: ${chatId}`);
    
    try {
        // ATOMIC TRANSACTION с idempotency check
        const result = await db.runTransaction(async (transaction) => {
            const chatRef = db.collection('chats').doc(chatId);
            const chatDoc = await transaction.get(chatRef);
            
            // IDEMPOTENCY: Если chat существует, вернуть существующий
            if (chatDoc.exists) {
                console.log(`✅ Chat already exists: ${chatId}`);
                return { exists: true, chatId };
            }
            
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
                authority: 'cloud_function_production',
                idempotencyKey: idempotencyKey
            };
            
            transaction.set(chatRef, chatData);
            
            // Создать userChats entries (cache)
            await createUserChatsEntries(transaction, uid1, uid2, chatId);
            
            console.log(`🆕 Chat created atomically: ${chatId}`);
            return { exists: false, chatId };
        });
        
        return { 
            success: !result.exists, 
            chatId: result.chatId 
        };
        
    } catch (error) {
        console.error('❌ Error creating chat with idempotency:', error);
        throw error;
    }
}

/**
 * 📋 PRODUCTION: Create userChats entries
 */
async function createUserChatsEntries(transaction, uid1, uid2, chatId) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        // User 1 cache entry
        const userChat1Ref = db
            .collection('userChats')
            .doc(uid1)
            .collection('chats')
            .doc(chatId);
        
        const userChat1Data = {
            chatId: chatId,
            otherUserId: uid2,
            createdAt: now,
            updatedAt: now,
            cacheType: 'server_production'
        };
        
        transaction.set(userChat1Ref, userChat1Data);
        
        // User 2 cache entry
        const userChat2Ref = db
            .collection('userChats')
            .doc(uid2)
            .collection('chats')
            .doc(chatId);
        
        const userChat2Data = {
            chatId: chatId,
            otherUserId: uid1,
            createdAt: now,
            updatedAt: now,
            cacheType: 'server_production'
        };
        
        transaction.set(userChat2Ref, userChat2Data);
        
        console.log(`✅ UserChats entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create userChats (non-critical):', error);
        // userChats failure не критичен
    }
}

/**
 * 📬 PRODUCTION: Send notifications
 */
async function sendProductionNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_production',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'production'
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
        
        console.log(`📬 Production notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🔧 PRODUCTION: Manual chat creation (HTTP callable)
 * 
 * Fallback method с теми же гарантиями
 */
exports.createChatProduction = functions.https.onCall(async (data, context) => {
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
        console.log(`🔧 Manual chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithIdempotency(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            message: chatResult.success ? 'Chat created' : 'Chat already exists'
        };
        
    } catch (error) {
        console.error('❌ Manual chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});

/**
 * 🔍 PRODUCTION: Chat existence check
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
        const chatId = generateProductionChatId(uid1, uid2);
        const chatDoc = await db.collection('chats').doc(chatId).get();
        
        return { 
            exists: chatDoc.exists,
            chatId: chatId
        };
        
    } catch (error) {
        console.error('❌ Chat existence check error:', error);
        throw new functions.https.HttpsError(
            'internal',
            'Failed to check chat existence'
        );
    }
});
