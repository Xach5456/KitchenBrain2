/**
 * 🎯 REAL-WORLD WHATSAPP-LEVEL CLOUD FUNCTIONS
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Duplicates are detectable and harmless
 * - NOT "no duplicates possible"
 * - Hash-based chatId with mapping table
 * - Real-world failure handling
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔥 REAL-WORLD: Hash-based chatId with mapping table
 * 
 * WhatsApp НЕ использует deterministic string IDs!
 * Использует hash + mapping для flexibility.
 */
function generateRealWorldChatId(uid1, uid2) {
    // 1. Конкатенация с separator
    const pairKey = `${uid1}:${uid2}`;
    
    // 2. SHA-256 hash (полный, без truncation)
    const crypto = require('crypto');
    const hash = crypto.createHash('sha256').update(pairKey).digest('base64');
    
    // 3. Очистить (но не обрезать!)
    return hash.replace(/[\/+=]/g, '');
}

/**
 * 🎯 REAL-WORLD TRIGGER: Follow with deduplication strategy
 * 
 * НЕ пытаемся предотвратить duplicates!
 * Дuplicates are detectable and harmless.
 */
exports.onFollowCreatedRealWorld = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 Real-world follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowRealWorld(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat (duplicates OK!)
            const chatResult = await createChatWithDeduplication(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created (may be duplicate): ${chatResult.chatId}`);
                
                // Step 3: Deduplicate если нужно
                if (chatResult.deduplicated) {
                    console.log(`🧹 Deduplicated: ${chatResult.archivedCount} chats archived`);
                }
                
                // Step 4: Отправить notifications
                await sendRealWorldNotifications(userId, targetId, chatResult.chatId);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ Real-world follow trigger error:', error);
            // НЕ бросаем ошибку - это real-world system
            console.log('⚠️ Continuing despite error (duplicates are harmless)');
            return null;
        }
    });

/**
 * 🔍 REAL-WORLD: Check mutual follow
 */
async function checkMutualFollowRealWorld(uid1, uid2) {
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
 * 🆕 REAL-WORLD: Create chat with deduplication strategy
 * 
 * НЕ пытаемся предотвратить duplicates!
 * Создаем chat, потом deduplicate если нужно.
 */
async function createChatWithDeduplication(uid1, uid2) {
    const chatId = generateRealWorldChatId(uid1, uid2);
    
    console.log(`🔍 Creating chat with deduplication: ${chatId}`);
    
    try {
        // Step 1: Проверить mapping table
        const mappingResult = await checkMappingTable(uid1, uid2);
        
        if (mappingResult) {
            console.log(`✅ Found in mapping table: ${mappingResult.chatId}`);
            return { success: true, chatId: mappingResult.chatId, deduplicated: false };
        }
        
        // Step 2: Создать chat (duplicates OK!)
        const chatRef = db.collection('chats').doc(chatId);
        const chatDoc = await chatRef.get();
        
        let wasCreated = false;
        
        if (!chatDoc.exists) {
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
                authority: 'real_world',
                hashBased: true,
                allowsDuplicates: true
            };
            
            await chatRef.set(chatData);
            wasCreated = true;
            
            console.log(`🆕 Chat created: ${chatId}`);
        } else {
            console.log(`✅ Chat already exists: ${chatId}`);
        }
        
        // Step 3: Создать mapping entry
        await createMappingEntry(uid1, uid2, chatId);
        
        // Step 4: Deduplicate если нужно
        const deduplicationResult = await deduplicateChats(uid1, uid2);
        
        let deduplicated = false;
        let archivedCount = 0;
        
        if (deduplicationResult.type === 'deduplicated') {
            deduplicated = true;
            archivedCount = deduplicationResult.archivedChatIds.length;
        }
        
        return { 
            success: true, 
            chatId: chatId, 
            wasCreated,
            deduplicated,
            archivedCount
        };
        
    } catch (error) {
        console.error('❌ Error creating chat with deduplication:', error);
        // НЕ бросаем ошибку - возвращаем failure но не crash
        return { success: false, error: error.message };
    }
}

/**
 * 🔍 REAL-WORLD: Check mapping table
 */
async function checkMappingTable(uid1, uid2) {
    try {
        const mappingDoc = await db
            .collection('chatMappings')
            .document(uid1)
            .collection('userMappings')
            .document(uid2)
            .get();
        
        if (mappingDoc.exists()) {
            const chatId = mappingDoc.get('chatId');
            if (chatId) {
                return { chatId, createdAt: mappingDoc.get('createdAt') };
            }
        }
        
        return null;
        
    } catch (error) {
        console.error('❌ Error checking mapping table:', error);
        return null;
    }
}

/**
 * 📦 REAL-WORLD: Create mapping entry
 */
async function createMappingEntry(uid1, uid2, chatId) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        const mappingData = {
            chatId: chatId,
            otherUserId: uid2,
            createdAt: now,
            hashBased: true,
            allowsDuplicates: true
        };
        
        await db
            .collection('chatMappings')
            .document(uid1)
            .collection('userMappings')
            .document(uid2)
            .set(mappingData);
        
        // Mapping для uid2
        const mappingData2 = {
            ...mappingData,
            otherUserId: uid1
        };
        
        await db
            .collection('chatMappings')
            .document(uid2)
            .collection('userMappings')
            .document(uid1)
            .set(mappingData2);
        
        console.log(`✅ Mapping entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create mapping (non-critical):', error);
        // Mapping failure не критичен
    }
}

/**
 * 🧹 REAL-WORLD: Deduplicate chats
 * 
 * НЕ предотвращаем duplicates!
 * Deduplicate после создания.
 */
async function deduplicateChats(uid1, uid2) {
    try {
        console.log(`🧹 Deduplicating chats: ${uid1} ↔ ${uid2}`);
        
        // Найти все чаты между этими пользователями
        const chatId1 = generateRealWorldChatId(uid1, uid2);
        const chatId2 = generateRealWorldChatId(uid2, uid1);
        
        const [chat1Doc, chat2Doc] = await Promise.all([
            db.collection('chats').document(chatId1).get(),
            db.collection('chats').document(chatId2).get()
        ]);
        
        const existingChats = [];
        
        if (chat1Doc.exists) existingChats.push(chatId1);
        if (chat2Doc.exists && chatId2 !== chatId1) existingChats.push(chatId2);
        
        if (existingChats.length <= 1) {
            return { type: 'single_chat', chatId: existingChats[0] || null };
        }
        
        // Multiple chats - выбрать последний по времени создания
        const chatDocs = await Promise.all(
            existingChats.map(chatId => db.collection('chats').document(chatId).get())
        );
        
        const chatWithTimestamps = chatDocs.map((doc, index) => ({
            chatId: existingChats[index],
            createdAt: doc.get('createdAt')
        }));
        
        const latestChat = chatWithTimestamps.reduce((latest, current) => {
            return current.createdAt && latest.createdAt && 
                   current.createdAt.toDate() > latest.createdAt.toDate() ? current : latest;
        });
        
        // Архивировать остальные
        const toArchive = existingChats.filter(chatId => chatId !== latestChat.chatId);
        
        if (toArchive.length > 0) {
            const batch = db.batch();
            
            for (const chatId of toArchive) {
                batch.update(db.collection('chats').document(chatId), {
                    active: false,
                    deduplicatedAt: admin.firestore.FieldValue.serverTimestamp(),
                    deduplicatedTo: latestChat.chatId
                });
            }
            
            await batch.commit();
            console.log(`🗑️ Archived ${toArchive.length} duplicate chats`);
        }
        
        return { 
            type: 'deduplicated', 
            primaryChatId: latestChat.chatId, 
            archivedChatIds: toArchive 
        };
        
    } catch (error) {
        console.error('❌ Error deduplicating chats:', error);
        return { type: 'error', error: error.message };
    }
}

/**
 * 📬 REAL-WORLD: Send notifications
 */
async function sendRealWorldNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_realworld',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'realworld',
            allowsDuplicates: true
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
        
        console.log(`📬 Real-world notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🔧 REAL-WORLD: Manual deduplication (HTTP callable)
 */
exports.deduplicateChats = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    
    if (!uid1 || !uid2) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Both uid1 and uid2 are required'
        );
    }
    
    try {
        console.log(`🔧 Manual deduplication: ${uid1} ↔ ${uid2}`);
        
        const result = await deduplicateChats(uid1, uid2);
        
        return { 
            success: true,
            result: result
        };
        
    } catch (error) {
        console.error('❌ Manual deduplication error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to deduplicate chats'
        );
    }
});

/**
 * 🔧 REAL-WORLD: Create chat with duplicates allowed
 */
exports.createChatRealWorld = functions.https.onCall(async (data, context) => {
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
        console.log(`🔧 Real-world chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithDeduplication(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            wasCreated: chatResult.wasCreated,
            deduplicated: chatResult.deduplicated,
            archivedCount: chatResult.archivedCount || 0,
            message: chatResult.success ? 
                (chatResult.wasCreated ? 'Chat created' : 'Chat already exists') : 
                'Failed to create chat'
        };
        
    } catch (error) {
        console.error('❌ Real-world chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});
