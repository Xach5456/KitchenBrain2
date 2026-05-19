package com.example.kitchenbrain.repository;

import android.util.Log;

import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FollowRepository {
    private static final String TAG = "FollowRepository";
    private final FirebaseFirestore db;
    
    private final Set<String> pendingOperations = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> completedOperations = new ConcurrentHashMap<>();
    private static final long OPERATION_TTL_MS = 5000;

    public FollowRepository() {
        db = FirebaseFirestore.getInstance();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::cleanupOldOperations, OPERATION_TTL_MS);
    }
    
    private boolean isDuplicateOperation(String operationId) {
        return pendingOperations.contains(operationId) || completedOperations.containsKey(operationId);
    }
    
    private void markOperationStarted(String operationId) { pendingOperations.add(operationId); }
    private void markOperationCompleted(String operationId) {
        pendingOperations.remove(operationId);
        completedOperations.put(operationId, System.currentTimeMillis());
    }
    private void markOperationFailed(String operationId) { pendingOperations.remove(operationId); }
    
    private void cleanupOldOperations() {
        long now = System.currentTimeMillis();
        completedOperations.entrySet().removeIf(entry -> (now - entry.getValue()) > OPERATION_TTL_MS);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::cleanupOldOperations, OPERATION_TTL_MS);
    }

    public void followUser(String fromUserId, String toUserId, Callback<Boolean> callback) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            if (callback != null) callback.onResult(false, "Invalid user IDs");
            return;
        }
        
        String operationKey = "follow_" + fromUserId + "_" + toUserId;
        if (isDuplicateOperation(operationKey)) return;

        markOperationStarted(operationKey);
        FollowGraphRepository.getInstance().updateFollowGraph(toUserId, true);

        com.google.firebase.firestore.DocumentReference followingRef = db.collection("following").document(fromUserId).collection("userFollowing").document(toUserId);
        com.google.firebase.firestore.DocumentReference followersRef = db.collection("followers").document(toUserId).collection("userFollowers").document(fromUserId);
        
        db.runTransaction(transaction -> {
            if (transaction.get(followingRef).exists()) {
                throw new RuntimeException("Already following");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("followedAt", com.google.firebase.Timestamp.now());
            data.put("followingId", toUserId);
            data.put("followerId", fromUserId);

            transaction.set(followingRef, data);
            transaction.set(followersRef, data);
            transaction.update(db.collection("users").document(fromUserId), "followingCount", com.google.firebase.firestore.FieldValue.increment(1));
            transaction.update(db.collection("users").document(toUserId), "followersCount", com.google.firebase.firestore.FieldValue.increment(1));
            return true;
        })
        .addOnSuccessListener(result -> {
            markOperationCompleted(operationKey);
            FollowGraphRepository.getInstance().refresh();
            
            // 🔥 CRITICAL: Trigger Notification & Mutual Follow Check
            createFollowNotification(fromUserId, toUserId);
            checkAndHandleMutualFollow(fromUserId, toUserId);
            
            if (callback != null) callback.onResult(true, null);
        })
        .addOnFailureListener(e -> {
            FollowGraphRepository.getInstance().updateFollowGraph(toUserId, false);
            markOperationFailed(operationKey);
            if (callback != null) callback.onResult(false, e.getMessage());
        });
    }

    private void createFollowNotification(String fromUserId, String toUserId) {
        db.collection("users").document(fromUserId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            
            String username = doc.getString("username");
            String nickname = doc.getString("nickname");
            String avatar = doc.getString("avatarUrl");
            String displayName = (nickname != null && !nickname.isEmpty()) ? nickname : username;

            Map<String, Object> notification = new HashMap<>();
            notification.put("receiverId", toUserId);
            notification.put("senderId", fromUserId);
            notification.put("senderUsername", username);
            notification.put("senderNickname", nickname);
            notification.put("senderAvatarUrl", avatar != null ? avatar : "");
            notification.put("type", "follow");
            notification.put("message", displayName + " followed you");
            notification.put("isRead", false);
            notification.put("createdAt", System.currentTimeMillis());

            // 🔥 RESTORED: Back to the top-level collection as in original version
            db.collection("notifications").add(notification);
        });
    }

    private void checkAndHandleMutualFollow(String userA, String userB) {
        db.collection("following").document(userB).collection("userFollowing").document(userA).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d(TAG, "✅ Mutual Follow Detected! Triggering auto-open chat.");
                    
                    // Create Chat Room record so it appears in ChatList
                    createChatRoomForMutualFollowers(userA, userB);
                    
                    // Notify User A to open chat with B
                    db.collection("users").document(userA).update("newMutualFollowUserId", userB);
                    // Notify User B to open chat with A
                    db.collection("users").document(userB).update("newMutualFollowUserId", userA);
                    
                    // Create a special notification
                    createMutualNotification(userA, userB);
                }
            });
    }
    
    private void createChatRoomForMutualFollowers(String userId1, String userId2) {
        String roomId = userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
        
        db.collection("chat_rooms").document(roomId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) return;
            
            Map<String, Object> chatRoom = new HashMap<>();
            chatRoom.put("roomId", roomId);
            chatRoom.put("user1Id", userId1.compareTo(userId2) < 0 ? userId1 : userId2);
            chatRoom.put("user2Id", userId1.compareTo(userId2) < 0 ? userId2 : userId1);
            chatRoom.put("lastMessageAt", System.currentTimeMillis());
            chatRoom.put("lastMessageText", "Say hi to your new friend!");
            chatRoom.put("unreadCount", 0);
            chatRoom.put("isActive", true);
            chatRoom.put("createdAt", System.currentTimeMillis());
            
            db.collection("chat_rooms").document(roomId).set(chatRoom);
        });
    }

    private void createMutualNotification(String fromUserId, String toUserId) {
        db.collection("users").document(fromUserId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Map<String, Object> notif = new HashMap<>();
            notif.put("receiverId", toUserId);
            notif.put("senderId", fromUserId);
            notif.put("senderUsername", doc.getString("username"));
            notif.put("type", "mutual_follow");
            notif.put("message", "🎉 You and " + doc.getString("username") + " are now mutual followers!");
            notif.put("isRead", false);
            notif.put("createdAt", System.currentTimeMillis());
            
            // 🔥 RESTORED: Back to the top-level collection as in original version
            db.collection("notifications").add(notif);
        });
    }

    public void unfollowUser(String fromUserId, String toUserId, Callback<Boolean> callback) {
        String operationKey = "unfollow_" + fromUserId + "_" + toUserId;
        if (isDuplicateOperation(operationKey)) return;
        markOperationStarted(operationKey);
        FollowGraphRepository.getInstance().updateFollowGraph(toUserId, false);

        db.runTransaction(transaction -> {
            transaction.delete(db.collection("following").document(fromUserId).collection("userFollowing").document(toUserId));
            transaction.delete(db.collection("followers").document(toUserId).collection("userFollowers").document(fromUserId));
            transaction.update(db.collection("users").document(fromUserId), "followingCount", com.google.firebase.firestore.FieldValue.increment(-1));
            transaction.update(db.collection("users").document(toUserId), "followersCount", com.google.firebase.firestore.FieldValue.increment(-1));
            return true;
        })
        .addOnSuccessListener(res -> {
            markOperationCompleted(operationKey);
            FollowGraphRepository.getInstance().refresh();
            if (callback != null) callback.onResult(true, null);
        })
        .addOnFailureListener(e -> {
            FollowGraphRepository.getInstance().updateFollowGraph(toUserId, true);
            markOperationFailed(operationKey);
            if (callback != null) callback.onResult(false, e.getMessage());
        });
    }

    public interface Callback<T> { void onResult(T result, String error); }
}
