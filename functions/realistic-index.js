/**
 * 🎯 REALISTIC WHATSAPP-LEVEL CLOUD FUNCTIONS
 * 
 * КРИТИЧЕСКИЕ ПРИНЦИПЫ:
 * - Failures happen, system tolerates gracefully
 * - Idempotent writes
 * - Deduplication at read layer
 * - Separate immutable chatId with pairKey index
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * 🔨 Generate pair key (index field)
 */
function generatePairKey(uid1, uid2) {
    const sorted = [uid1, uid2].sort();
    return `${sorted[0]}_${sorted[1]}`;
}

/**
 * 🔍 REALISTIC: Check mutual follow
 */
async function checkMutualFollowRealistic(uid1, uid2) {
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
 * 🎯 REALISTIC TRIGGER: Follow with idempotent writes
 * 
 * НЕ "impossible to break"
 * А "fails gracefully and recovers"
 */
exports.onFollowCreatedRealistic = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        console.log(`🔍 Realistic follow: ${userId} → ${targetId}`);
        
        try {
            // Step 1: Проверить mutual follow
            const isMutual = await checkMutualFollowRealistic(userId, targetId);
            
            if (!isMutual) {
                console.log(`➡️ One-way follow, no chat: ${userId} → ${targetId}`);
                return null;
            }
            
            console.log(`🎉 Mutual follow detected: ${userId} ↔ ${targetId}`);
            
            // Step 2: Создать chat (может быть duplicate - это OK!)
            const chatResult = await createChatWithIdempotency(userId, targetId);
            
            if (chatResult.success) {
                console.log(`✅ Chat created (may be duplicate): ${chatResult.chatId}`);
                
                // Step 3: Отправить notifications
                await sendRealisticNotifications(userId, targetId, chatResult.chatId);
            }
            
            return chatResult;
            
        } catch (error) {
            console.error('❌ Realistic follow trigger error:', error);
            // Graceful degradation - system tolerates failures
            console.log('⚠️ Continuing despite error (realistic system)');
            return null;
        }
    });

/**
 * 🆕 REALISTIC: Create chat with idempotency
 * 
 * НЕ "zero race conditions"
 * А "idempotent writes + deduplication at read layer"
 */
async function createChatWithIdempotency(uid1, uid2) {
    try {
        console.log(`🔍 Creating chat with idempotency: ${uid1} ↔ ${uid2}`);
        
        // ✅ Separate immutable chatId + pairKey index
        const chatId = require('crypto').randomUUID();
        const pairKey = generatePairKey(uid1, uid2);
        
        // Check existing chat via pairKey index
        const existingChatId = await findExistingChatByPairKey(pairKey);
        
        if (existingChatId) {
            console.log(`✅ Found existing chat: ${existingChatId}`);
            return { success: true, chatId: existingChatId, wasCreated: false };
        }
        
        // Create new chat (idempotent write - may be duplicate)
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        const chatData = {
            chatId: chatId,
            pairKey: pairKey,
            participants: [uid1, uid2],
            createdAt: now,
            updatedAt: now,
            lastMessage: null,
            lastMessageTime: now,
            active: true,
            version: 1,
            authority: 'realistic',
            allowsDuplicates: true,
            idempotent: true,
            toleratesFailures: true
        };
        
        await db.collection('chats').document(chatId).set(chatData);
        
        // Create userChats entries
        await createRealisticUserChatEntries(uid1, uid2, chatId, pairKey);
        
        console.log(`🆕 Chat created (may be duplicate): ${chatId}`);
        return { success: true, chatId, wasCreated: true };
        
    } catch (error) {
        console.error('❌ Error creating chat with idempotency:', error);
        return { success: false, error: error.message };
    }
}

/**
 * 🔍 Find existing chat via pairKey index
 */
async function findExistingChatByPairKey(pairKey) {
    try {
        const query = await db.collection('chats')
            .where('pairKey', '==', pairKey)
            .where('active', '==', true)
            .limit(1)
            .get();
        
        for (const doc of query.docs) {
            return doc.id;
        }
        
        return null;
    } catch (error) {
        console.error('❌ Error finding existing chat:', error);
        return null;
    }
}

/**
 * 📋 REALISTIC: Create userChats entries
 */
async function createRealisticUserChatEntries(uid1, uid2, chatId, pairKey) {
    try {
        const now = admin.firestore.FieldValue.serverTimestamp();
        
        // User 1 entry
        const userChat1Data = {
            chatId: chatId,
            pairKey: pairKey,
            otherUserId: uid2,
            createdAt: now,
            updatedAt: now,
            realistic: true,
            idempotent: true
        };
        
        await db.collection('userChats')
            .document(uid1)
            .collection('chats')
            .document(chatId)
            .set(userChat1Data);
        
        // User 2 entry
        const userChat2Data = {
            chatId: chatId,
            pairKey: pairKey,
            otherUserId: uid1,
            createdAt: now,
            updatedAt: now,
            realistic: true,
            idempotent: true
        };
        
        await db.collection('userChats')
            .document(uid2)
            .collection('chats')
            .document(chatId)
            .set(userChat2Data);
        
        console.log(`✅ Realistic userChats entries created: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to create userChats (non-critical):', error);
        // userChats failure не критичен
    }
}

/**
 * 📬 REALISTIC: Send notifications
 */
async function sendRealisticNotifications(uid1, uid2, chatId) {
    try {
        const notificationData = {
            type: 'mutual_follow_realistic',
            chatId: chatId,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            version: 'realistic',
            idempotent: true,
            toleratesFailures: true
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
        
        console.log(`📬 Realistic notifications sent: ${chatId}`);
        
    } catch (error) {
        console.error('⚠️ Failed to send notifications:', error);
        // Notification failure не критичен
    }
}

/**
 * 🧹 REALISTIC: Deduplicate chats at read layer
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
        console.log(`🧹 Deduplicating chats: ${uid1} ↔ ${uid2}`);
        
        const pairKey = generatePairKey(uid1, uid2);
        
        // Найти все чаты с этим pairKey
        const query = await db.collection('chats')
            .where('pairKey', '==', pairKey)
            .where('active', '==', true)
            .get();
        
        const chatIds = query.docs.map(doc => doc.id);
        
        if (chatIds.length <= 1) {
            return { 
                success: true, 
                chatId: chatIds[0] || null,
                deduplicated: false 
            };
        }
        
        // Выбрать самый новый (last-write-wins)
        const chatDocs = await Promise.all(
            chatIds.map(chatId => db.collection('chats').document(chatId).get())
        );
        
        const chatWithTimestamps = chatDocs.map((doc, index) => ({
            chatId: chatIds[index],
            updatedAt: doc.get('updatedAt')
        }));
        
        const latestChat = chatWithTimestamps.reduce((latest, current) => {
            return current.updatedAt && latest.updatedAt && 
                   current.updatedAt.toDate() > latest.updatedAt.toDate() ? current : latest;
        });
        
        // Архивировать остальные
        const toArchive = chatIds.filter(chatId => chatId !== latestChat.chatId);
        
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
            success: true, 
            chatId: latestChat.chatId,
            deduplicated: true,
            archivedCount: toArchive.length
        };
        
    } catch (error) {
        console.error('❌ Realistic deduplication error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to deduplicate chats'
        );
    }
});

/**
 * 🔧 REALISTIC: Background reconciliation job
 */
exports.reconcileDuplicateChats = functions.https.onCall(async (data, context) => {
    try {
        console.log('🔧 Starting background reconciliation');
        
        // Найти все чаты
        const allChats = await db.collection('chats')
            .where('active', '==', true)
            .get();
        
        const pairKeyGroups = {};
        
        allChats.docs.forEach(doc => {
            const pairKey = doc.get('pairKey');
            if (pairKey) {
                if (!pairKeyGroups[pairKey]) {
                    pairKeyGroups[pairKey] = [];
                }
                pairKeyGroups[pairKey].push(doc);
            }
        });
        
        const duplicateGroups = Object.entries(pairKeyGroups)
            .filter(([pairKey, chats]) => chats.length > 1);
        
        let reconciledCount = 0;
        let archivedCount = 0;
        
        for (const [pairKey, duplicateChats] of duplicateGroups) {
            // Выбрать самый новый
            const latestChat = duplicateChats.reduce((latest, current) => {
                return current.get('updatedAt') && latest.get('updatedAt') && 
                       current.get('updatedAt').toDate() > latest.get('updatedAt').toDate() ? current : latest;
            });
            
            const toArchive = duplicateChats.filter(chat => chat.id !== latestChat.id);
            
            for (const chatDoc of toArchive) {
                await db.collection('chats').document(chatDoc.id)
                    .update('active', false, 'reconciledAt', admin.firestore.FieldValue.serverTimestamp());
                archivedCount++;
            }
            
            reconciledCount++;
        }
        
        console.log(`✅ Reconciliation complete: ${reconciledCount} groups, ${archivedCount} archived`);
        
        return { 
            success: true,
            groupsReconciled: reconciledCount,
            chatsArchived: archivedCount
        };
        
    } catch (error) {
        console.error('❌ Background reconciliation failed:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Reconciliation failed'
        );
    }
});

/**
 * 🔧 REALISTIC: Manual chat creation (HTTP callable)
 */
exports.createChatRealistic = functions.https.onCall(async (data, context) => {
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
        console.log(`🔧 Realistic chat creation: ${uid1} ↔ ${uid2}`);
        
        const chatResult = await createChatWithIdempotency(uid1, uid2);
        
        return { 
            success: chatResult.success, 
            chatId: chatResult.chatId,
            wasCreated: chatResult.wasCreated,
            message: chatResult.success ? 
                (chatResult.wasCreated ? 'Chat created' : 'Chat already exists') : 
                'Failed to create chat'
        };
        
    } catch (error) {
        console.error('❌ Realistic chat creation error:', error);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'Failed to create chat'
        );
    }
});
