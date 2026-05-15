/**
 * No architecture, just code
 * 
 * chatId = sorted uid1 + uid2
 * document = chats/chatId
 * create if not exists (transaction)
 * read document for UI
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

/**
 * chatId = sorted uid1 + uid2
 */
function chatId(uid1, uid2) {
    return [uid1, uid2].sort().join('_');
}

/**
 * create if not exists (transaction)
 */
exports.createChat = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    const id = chatId(uid1, uid2);
    
    await db.runTransaction(async (tx) => {
        const doc = await tx.get(db.collection('chats').document(id));
        
        if (!doc.exists) {
            tx.set(db.collection('chats').document(id), {
                participants: [uid1, uid2],
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                active: true
            });
        }
    });
    
    return { chatId: id };
});

/**
 * read document for UI
 */
exports.readChat = functions.https.onCall(async (data, context) => {
    const { uid1, uid2 } = data;
    const doc = await db.collection('chats').document(chatId(uid1, uid2)).get();
    return doc.exists ? doc.data() : null;
});

/**
 * send message
 */
exports.sendMessage = functions.https.onCall(async (data, context) => {
    const { chatId, senderId, text } = data;
    const messageId = require('crypto').randomUUID();
    
    await db.collection('chats').document(chatId)
        .collection('messages').document(messageId)
        .set({
            senderId: senderId,
            text: text,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
    
    await db.collection('chats').document(chatId)
        .update({
            lastMessage: text,
            lastMessageTime: admin.firestore.FieldValue.serverTimestamp()
        });
    
    return { messageId: messageId };
});

/**
 * follow trigger
 */
exports.onFollow = functions.firestore
    .document('following/{userId}/userFollowing/{targetId}')
    .onCreate(async (snapshot, context) => {
        const userId = context.params.userId;
        const targetId = context.params.targetId;
        
        // check mutual
        const mutualFollow = await db.collection('following')
            .document(targetId)
            .collection('userFollowing')
            .document(userId)
            .get().then(doc => doc.exists);
        
        if (mutualFollow) {
            await exports.createChat({ uid1: userId, uid2: targetId });
        }
    });
