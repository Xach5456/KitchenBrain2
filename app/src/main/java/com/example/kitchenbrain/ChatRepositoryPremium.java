package com.example.kitchenbrain;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.kitchenbrain.utils.ChatIdGenerator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Production-Grade Repository for Chat System
 * Real Firestore integration with real-time updates
 * 
 * ✅ FIXED: All Task operations now use proper async handling
 * ✅ FIXED: No more IllegalStateException from calling getResult() on incomplete tasks
 * ✅ FIXED: Proper callback-based architecture for all Firestore operations
 */
public class ChatRepositoryPremium {
    
    private static final String TAG = "ChatRepositoryPremium";
    private final FirebaseFirestore db;
    private final CollectionReference chatsCollection;
    private final ExecutorService executor;
    private final Handler mainHandler; // For posting results to main thread
    
    public ChatRepositoryPremium() {
        this.db = FirebaseFirestore.getInstance();
        this.chatsCollection = db.collection("chats");
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get or create chat room between two users
     * Chat ID format: userId1_userId2 (alphabetically sorted)
     * 
     * ✅ FIXED: Now uses proper callback-based async pattern instead of blocking getResult()
     */
    public void getOrCreateChatRoom(String userId1, String userId2, ChatRoomCallback callback) {
        executor.execute(() -> {
            try {
                // 🔥 CRITICAL: Use centralized ChatIdGenerator
                String chatId = ChatIdGenerator.generate(userId1, userId2);
                
                DocumentReference chatRef = chatsCollection.document(chatId);
                
                // ✅ SAFE: Using Tasks.await() on background thread - it returns the Task result directly
                DocumentSnapshot snapshot = Tasks.await(chatRef.get());
                
                if (!snapshot.exists()) {
                    // Create new chat room
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("userId1", userId1);
                    chatData.put("userId2", userId2);
                    chatData.put("createdAt", Timestamp.now());
                    chatData.put("lastMessageTime", Timestamp.now());
                    chatData.put("participantIds", List.of(userId1, userId2));
                    
                    // ✅ SAFE: Await task completion before proceeding
                    Tasks.await(chatRef.set(chatData));
                    Log.d(TAG, "Created new chat room: " + chatId);
                } else {
                    Log.d(TAG, "Using existing chat room: " + chatId);
                }
                
                // Post result to main thread
                final String finalChatId = chatId;
                mainHandler.post(() -> callback.onSuccess(finalChatId));
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting/creating chat room", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Alternative method using callback chaining (no await)
     * Demonstrates both patterns for educational purposes
     */
    public Task<String> getOrCreateChatRoomAsync(String userId1, String userId2) {
        // 🔥 CRITICAL: Use centralized ChatIdGenerator
        String chatId = ChatIdGenerator.generate(userId1, userId2);
        
        DocumentReference chatRef = chatsCollection.document(chatId);
        
        return chatRef.get()
            .continueWithTask(task -> {
                DocumentSnapshot snapshot = task.getResult();
                if (!snapshot.exists()) {
                    // Create new chat room
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("userId1", userId1);
                    chatData.put("userId2", userId2);
                    chatData.put("createdAt", Timestamp.now());
                    chatData.put("lastMessageTime", Timestamp.now());
                    chatData.put("participantIds", List.of(userId1, userId2));
                    
                    return chatRef.set(chatData).continueWith(setTask -> chatId);
                } else {
                    return Tasks.forResult(chatId);
                }
            });
    }
    
    /**
     * Send a message
     * 
     * ✅ FIXED: Proper async handling with Tasks.await() on background thread
     */
    public void sendMessage(String chatId, String senderId, String receiverId, String text, MessageCallback callback) {
        executor.execute(() -> {
            try {
                if (text == null || text.trim().isEmpty()) {
                    throw new IllegalArgumentException("Message text cannot be empty");
                }
                
                CollectionReference messagesRef = chatsCollection.document(chatId)
                        .collection("messages");
                
                // Create message
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("senderId", senderId);
                messageData.put("receiverId", receiverId);
                messageData.put("text", text.trim());
                messageData.put("timestamp", Timestamp.now());
                messageData.put("messageStatus", "sent"); // sent, delivered, read
                
                // ✅ SAFE: Add message and await completion
                DocumentReference messageRef = Tasks.await(messagesRef.add(messageData));
                
                // ✅ CRITICAL: Update chat's last message and updatedAt for sorting
                Map<String, Object> chatUpdates = new HashMap<>();
                chatUpdates.put("lastMessage", text.trim().length() > 50 ? text.trim().substring(0, 50) + "..." : text.trim());
                chatUpdates.put("lastMessageTime", Timestamp.now());
                chatUpdates.put("lastMessageSender", senderId);
                chatUpdates.put("updatedAt", Timestamp.now());
                
                Tasks.await(chatsCollection.document(chatId).update(chatUpdates));
                
                Log.d(TAG, "Message sent: " + messageRef.getId());
                
                // Post result to main thread
                mainHandler.post(() -> callback.onSuccess(messageRef.getId()));
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Alternative send message returning Task for chaining
     */
    public Task<String> sendMessageAsync(String chatId, String senderId, String receiverId, String text) {
        if (text == null || text.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Message text cannot be empty"));
        }
        
        CollectionReference messagesRef = chatsCollection.document(chatId)
                .collection("messages");
        
        // Create message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", senderId);
        messageData.put("receiverId", receiverId);
        messageData.put("text", text.trim());
        messageData.put("timestamp", Timestamp.now());
        messageData.put("messageStatus", "sent");
        
        return messagesRef.add(messageData)
            .continueWithTask(task -> {
                DocumentReference messageRef = task.getResult();
                // Update last message time
                return chatsCollection.document(chatId)
                    .update("lastMessageTime", Timestamp.now())
                    .continueWith(updateTask -> messageRef.getId());
            });
    }
    
    /**
     * Mark message as read
     * 
     * ✅ FIXED: Safe async operation
     */
    public void markMessageAsRead(String chatId, String messageId, OperationCallback callback) {
        executor.execute(() -> {
            try {
                Tasks.await(
                    chatsCollection.document(chatId)
                        .collection("messages")
                        .document(messageId)
                        .update("messageStatus", "read")
                );
                Log.d(TAG, "Message marked as read: " + messageId);
                mainHandler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "Error marking message as read", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Mark all messages from sender as read
     * 
     * ✅ FIXED: Proper batch update with error handling
     */
    public void markAllMessagesAsRead(String chatId, String senderId, OperationCallback callback) {
        executor.execute(() -> {
            try {
                var snapshot = Tasks.await(
                    chatsCollection.document(chatId)
                        .collection("messages")
                        .whereEqualTo("senderId", senderId)
                        .whereEqualTo("messageStatus", "delivered")
                        .get()
                );
                
                List<Task<Void>> updateTasks = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    updateTasks.add(
                        chatsCollection.document(chatId)
                                .collection("messages")
                                .document(doc.getId())
                                .update("messageStatus", "read")
                    );
                }
                
                if (!updateTasks.isEmpty()) {
                    Tasks.await(Tasks.whenAll(updateTasks));
                }
                
                Log.d(TAG, "Marked " + snapshot.size() + " messages as read");
                mainHandler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "Error marking all messages as read", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Get messages with real-time listener
     */
    public ListenerRegistration addMessagesListener(String chatId, MessagesCallback callback) {
        return chatsCollection.document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(50) // Load last 50 messages
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages", error);
                        callback.onError(error.getMessage());
                        return;
                    }
                    
                    if (snapshot != null) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                ChatMessage message = doc.toObject(ChatMessage.class);
                                message.setMessageId(doc.getId());
                                messages.add(message);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing message", e);
                            }
                        }
                        callback.onMessagesLoaded(messages);
                    }
                });
    }
    
    /**
     * Update user's online status
     * 
     * ✅ FIXED: Safe async operation
     */
    public void updateUserOnlineStatus(String userId, boolean isOnline, OperationCallback callback) {
        executor.execute(() -> {
            try {
                Tasks.await(
                    db.collection("users").document(userId)
                        .update("isOnline", isOnline)
                );
                Log.d(TAG, "Updated online status for user: " + userId);
                mainHandler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "Error updating online status", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Set typing indicator
     * 
     * ✅ FIXED: Safe async operation
     */
    public void setTypingIndicator(String chatId, String userId, boolean isTyping, OperationCallback callback) {
        executor.execute(() -> {
            try {
                Map<String, Object> typingData = new HashMap<>();
                typingData.put("typingUserId", isTyping ? userId : null);
                typingData.put("typingAt", isTyping ? Timestamp.now() : null);
                
                Tasks.await(
                    chatsCollection.document(chatId)
                        .update(typingData)
                );
                Log.d(TAG, "Typing indicator set: " + isTyping);
                mainHandler.post(() -> callback.onSuccess());
            } catch (Exception e) {
                Log.e(TAG, "Error setting typing indicator", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }
    
    /**
     * Get user's online status
     */
    public ListenerRegistration getUserOnlineStatus(String userId, OnlineStatusCallback callback) {
        return db.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Boolean isOnline = snapshot.getBoolean("isOnline");
                        callback.onOnlineStatusChanged(isOnline != null && isOnline);
                    }
                });
    }
    
    // Callbacks
    public interface MessagesCallback {
        void onMessagesLoaded(List<ChatMessage> messages);
        void onError(String error);
    }
    
    public interface OnlineStatusCallback {
        void onOnlineStatusChanged(boolean isOnline);
        void onError(String error);
    }
    
    // ✅ NEW: Additional callbacks for async operations
    public interface ChatRoomCallback {
        void onSuccess(String chatRoomId);
        void onError(Exception e);
    }
    
    public interface MessageCallback {
        void onSuccess(String messageId);
        void onError(Exception e);
    }
    
    public interface OperationCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
