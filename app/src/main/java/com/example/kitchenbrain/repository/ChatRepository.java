package com.example.kitchenbrain.repository;

import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.ChatRoom;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.Filter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔥 COMPREHENSIVE CHAT REPOSITORY
 * Unified data layer for all messaging collections and relationship types.
 */
public class ChatRepository {
    
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore db;
    
    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * 🚀 SEND MESSAGE - Firebase Write Operation
     */
    public void sendMessage(String chatId, ChatMessage message, FirebaseCallback callback) {
        Log.d(TAG, "📡 Sending message to Firebase: " + message.getMessageId());
        
        DocumentReference messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document((message.getMessageId() != null && !message.getMessageId().isEmpty()) ? 
                        message.getMessageId() : db.collection("chats").document().getId());
        
        message.setMessageId(messageRef.getId());
        
        messageRef.set(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firebase write successful: " + message.getMessageId());
                    updateSummaries(chatId, message);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase write failed for " + message.getMessageId(), e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    private void updateSummaries(String chatId, ChatMessage message) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastMessageText", message.getText());
        update.put("lastMessage", message.getText());
        update.put("lastMessageAt", System.currentTimeMillis());
        update.put("lastMessageTime", com.google.firebase.Timestamp.now());
        update.put("isActive", true);
        
        if (ChatIdProvider.isValidChatId(chatId)) {
            String[] ids = ChatIdProvider.extractUserIds(chatId);
            if (ids != null && ids.length == 2) {
                String u1 = ids[0].compareTo(ids[1]) < 0 ? ids[0] : ids[1];
                String u2 = ids[0].compareTo(ids[1]) < 0 ? ids[1] : ids[0];
                update.put("user1Id", u1);
                update.put("user2Id", u2);
                update.put("userId1", u1);
                update.put("userId2", u2);
                
                List<String> p = List.of(u1, u2);
                update.put("participants", p);
                update.put("participantIds", p);
                update.put("roomId", chatId);
            }
        }
        
        db.collection("chat_rooms").document(chatId).set(update, SetOptions.merge());
        db.collection("chats").document(chatId).set(update, SetOptions.merge());
    }
    
    /**
     * 👂 LISTEN TO MESSAGES - Real-time updates
     */
    public ListenerRegistration listenMessages(String chatId, MessageListener listener) {
        if (chatId == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("chatId cannot be null"));
            return null;
        }
        
        Query query = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30);
        
        return query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                if (listener != null) listener.onError(e);
                return;
            }
            if (snapshots == null) return;
            
            List<ChatMessage> messages = new ArrayList<>();
            for (var doc : snapshots.getDocuments()) {
                try {
                    ChatMessage msg = doc.toObject(ChatMessage.class);
                    if (msg != null) {
                        if (msg.getMessageId() == null || msg.getMessageId().isEmpty()) {
                            msg.setMessageId(doc.getId());
                        }
                        messages.add(msg);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing message: " + doc.getId(), ex);
                }
            }
            if (listener != null) listener.onMessages(messages);
        });
    }
    
    /**
     * 👂 LISTEN TO CHAT ROOMS - Active conversations
     */
    public ListenerRegistration listenChatRooms(String userId, ChatRoomsListener listener) {
        final Map<String, ChatRoom> roomsMap = new ConcurrentHashMap<>();
        List<ListenerRegistration> regs = new ArrayList<>();
        
        Runnable notify = () -> {
            if (listener != null) listener.onChatRooms(new ArrayList<>(roomsMap.values()));
        };

        Filter equalityFilter = Filter.or(
                Filter.equalTo("user1Id", userId),
                Filter.equalTo("user2Id", userId),
                Filter.equalTo("userId1", userId),
                Filter.equalTo("userId2", userId)
        );

        String[] collections = {"chat_rooms", "chats"};

        for (String coll : collections) {
            regs.add(db.collection(coll).where(equalityFilter).addSnapshotListener((snap, e) -> {
                if (e != null) { if (listener != null) listener.onError(e); return; }
                if (snap != null) {
                    for (var doc : snap.getDocuments()) {
                        try {
                            ChatRoom r = doc.toObject(ChatRoom.class);
                            if (r != null) { 
                                r.setRoomId(doc.getId()); 
                                roomsMap.put(doc.getId(), r); 
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deserializing ChatRoom: " + doc.getId(), ex);
                        }
                    }
                    notify.run();
                }
            }));

            regs.add(db.collection(coll).whereArrayContains("participants", userId).addSnapshotListener((snap, e) -> {
                if (e != null) { if (listener != null) listener.onError(e); return; }
                if (snap != null) {
                    for (var doc : snap.getDocuments()) {
                        try {
                            ChatRoom r = doc.toObject(ChatRoom.class);
                            if (r != null) { 
                                r.setRoomId(doc.getId()); 
                                roomsMap.put(doc.getId(), r); 
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deserializing ChatRoom: " + doc.getId(), ex);
                        }
                    }
                    notify.run();
                }
            }));

            regs.add(db.collection(coll).whereArrayContains("participantIds", userId).addSnapshotListener((snap, e) -> {
                if (e != null) { if (listener != null) listener.onError(e); return; }
                if (snap != null) {
                    for (var doc : snap.getDocuments()) {
                        try {
                            ChatRoom r = doc.toObject(ChatRoom.class);
                            if (r != null) { 
                                r.setRoomId(doc.getId()); 
                                roomsMap.put(doc.getId(), r); 
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deserializing ChatRoom: " + doc.getId(), ex);
                        }
                    }
                    notify.run();
                }
            }));
        }

        return () -> { for (ListenerRegistration r : regs) r.remove(); };
    }
    
    /**
     * ✏️ EDIT MESSAGE
     */
    public void editMessage(String chatId, String messageId, String newText, FirebaseCallback callback) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("text", newText, "edited", true)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
    
    /**
     * 🗑️ DELETE MESSAGE
     */
    public void deleteMessage(String chatId, String messageId, FirebaseCallback callback) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
    
    /**
     * MARK MESSAGES AS READ
     */
    public void markMessagesAsRead(String chatId, String currentUserId, String otherUserId, FirebaseCallback callback) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", otherUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        if (callback != null) callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (var doc : query.getDocuments()) {
                        batch.update(doc.getReference(), "isRead", true);
                        batch.update(doc.getReference(), "readAt", System.currentTimeMillis());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(); })
                            .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }
    
    /**
     * 👂 LISTEN TO READ RECEIPT UPDATES
     */
    public ListenerRegistration listenToReadReceipts(String chatId, String currentUserId, ReadReceiptListener listener) {
        return db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { if (listener != null) listener.onError(e); return; }
                    if (snapshots == null) return;
                    for (var doc : snapshots.getDocumentChanges()) {
                        if (doc.getType() == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                            String messageId = doc.getDocument().getId();
                            Boolean isRead = doc.getDocument().getBoolean("isRead");
                            if (isRead != null && listener != null) {
                                listener.onMessageReadStatusChanged(messageId, isRead);
                            }
                        }
                    }
                });
    }
    
    /**
     * ✏️ SET TYPING STATUS
     */
    public void setTyping(String chatId, String userId, boolean isTyping) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> typing = new HashMap<>();
        typing.put(userId, isTyping);
        data.put("typing", typing);
        db.collection("chats").document(chatId).set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to set typing status", e));
    }

    /**
     * 👂 LISTEN TO TYPING STATUS
     */
    public ListenerRegistration listenToTyping(String chatId, TypingListener listener) {
        return db.collection("chats")
                .document(chatId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { if (listener != null) listener.onError(e); return; }
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Boolean> typingMap = (Map<String, Boolean>) snapshot.get("typing");
                        if (typingMap != null && listener != null) {
                            listener.onTypingChanged(typingMap);
                        }
                    }
                });
    }

    // Callbacks & Listeners
    public interface FirebaseCallback {
        void onSuccess();
        void onFailure(Exception error);
    }
    
    public interface MessageListener {
        void onMessages(List<ChatMessage> messages);
        void onError(Exception error);
    }
    
    public interface ChatRoomsListener {
        void onChatRooms(List<ChatRoom> rooms);
        void onError(Exception error);
    }

    public interface ReadReceiptListener {
        void onMessageReadStatusChanged(String messageId, boolean isRead);
        void onError(Exception error);
    }

    public interface TypingListener {
        void onTypingChanged(Map<String, Boolean> typingStates);
        void onError(Exception error);
    }
}
