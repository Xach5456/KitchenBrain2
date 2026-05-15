package com.example.kitchenbrain.notifications;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationManager - Addiction mechanics for social cooking platform
 * 
 * 🔥 NOTIFICATION ARCHITECTURE:
 * - Real-time notifications for engagement
 * - Drives user retention and addiction
 * - Firestore path: notifications/{userId}/user_notifications
 */
public class NotificationManager {
    
    private static final String TAG = "NotificationManager";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_USER_NOTIFICATIONS = "user_notifications";
    private static final int NOTIFICATION_LIMIT = 50;
    
    private final FirebaseFirestore db;
    private final Map<String, ListenerRegistration> activeListeners = new HashMap<>();
    
    public NotificationManager() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Send notification when someone likes a recipe
     */
    public void sendLikeNotification(String recipeAuthorId, String likerUserId, 
                                   String likerName, String recipeTitle, String recipeId) {
        Log.d(TAG, "❤️ Sending like notification to: " + recipeAuthorId);
        
        if (recipeAuthorId.equals(likerUserId)) {
            return; // Don't send notification for self-like
        }
        
        Notification notification = new Notification(
            Notification.TYPE_LIKE,
            likerUserId,
            likerName,
            "🔥 " + likerName + " liked your " + recipeTitle,
            recipeId,
            System.currentTimeMillis()
        );
        
        sendNotification(recipeAuthorId, notification);
    }
    
    /**
     * Send notification when someone comments on a recipe
     */
    public void sendCommentNotification(String recipeAuthorId, String commenterUserId,
                                      String commenterName, String recipeTitle, String recipeId) {
        Log.d(TAG, "💬 Sending comment notification to: " + recipeAuthorId);
        
        if (recipeAuthorId.equals(commenterUserId)) {
            return; // Don't send notification for self-comment
        }
        
        Notification notification = new Notification(
            Notification.TYPE_COMMENT,
            commenterUserId,
            commenterName,
            "💬 " + commenterName + " commented on your " + recipeTitle,
            recipeId,
            System.currentTimeMillis()
        );
        
        sendNotification(recipeAuthorId, notification);
    }
    
    /**
     * Send notification when mutual follower uploads a new recipe
     */
    public void sendMutualFollowerRecipeNotification(String userId, String followerId,
                                                     String followerName, String recipeTitle, String recipeId) {
        Log.d(TAG, "🍳 Sending mutual follower recipe notification to: " + userId);
        
        Notification notification = new Notification(
            Notification.TYPE_MUTUAL_FOLLOWER_RECIPE,
            followerId,
            followerName,
            "🍳 " + followerName + " shared a new recipe: " + recipeTitle,
            recipeId,
            System.currentTimeMillis()
        );
        
        sendNotification(userId, notification);
    }
    
    /**
     * Send notification when someone starts following
     */
    public void sendFollowNotification(String userId, String followerId, String followerName) {
        Log.d(TAG, "👥 Sending follow notification to: " + userId);
        
        Notification notification = new Notification(
            Notification.TYPE_FOLLOW,
            followerId,
            followerName,
            "👥 " + followerName + " started following you",
            null,
            System.currentTimeMillis()
        );
        
        sendNotification(userId, notification);
    }
    
    /**
     * Send notification to a user
     */
    private void sendNotification(String userId, Notification notification) {
        CollectionReference userNotificationsRef = db.collection(COLLECTION_NOTIFICATIONS)
            .document(userId)
            .collection(COLLECTION_USER_NOTIFICATIONS);
        
        userNotificationsRef.add(notification)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "✅ Notification sent: " + notification.message);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to send notification", e);
            });
    }
    
    /**
     * Listen to real-time notifications for a user
     */
    public ListenerRegistration listenToNotifications(String userId, NotificationCallback callback) {
        Log.d(TAG, "👂 Listening to notifications for: " + userId);
        
        CollectionReference notificationsRef = db.collection(COLLECTION_NOTIFICATIONS)
            .document(userId)
            .collection(COLLECTION_USER_NOTIFICATIONS);
        
        ListenerRegistration listener = notificationsRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(NOTIFICATION_LIMIT)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "❌ Notifications listener error", error);
                    callback.onError(error.getMessage());
                    return;
                }
                
                List<Notification> notifications = new ArrayList<>();
                if (snapshots != null) {
                    for (var doc : snapshots.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.notificationId = doc.getId();
                            notifications.add(notification);
                        }
                    }
                }
                
                Log.d(TAG, "📥 Notifications updated: " + notifications.size());
                callback.onNotificationsUpdated(notifications);
            });
        
        // Store listener for cleanup
        activeListeners.put(userId, listener);
        
        return listener;
    }
    
    /**
     * Stop listening to notifications for a user
     */
    public void stopListeningToNotifications(String userId) {
        ListenerRegistration listener = activeListeners.remove(userId);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "🔇 Stopped listening to notifications for: " + userId);
        }
    }
    
    /**
     * Mark notifications as read
     */
    public void markNotificationsAsRead(String userId, List<String> notificationIds) {
        Log.d(TAG, "📖 Marking notifications as read: " + notificationIds.size());
        
        CollectionReference notificationsRef = db.collection(COLLECTION_NOTIFICATIONS)
            .document(userId)
            .collection(COLLECTION_USER_NOTIFICATIONS);
        
        for (String notificationId : notificationIds) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("read", true);
            updateData.put("readAt", System.currentTimeMillis());
            
            notificationsRef.document(notificationId)
                .update(updateData)
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Failed to mark notification as read: " + notificationId, e);
                });
        }
    }
    
    /**
     * Get unread notifications count
     */
    public void getUnreadNotificationsCount(String userId, CountCallback callback) {
        Log.d(TAG, "🔢 Getting unread notifications count for: " + userId);
        
        CollectionReference notificationsRef = db.collection(COLLECTION_NOTIFICATIONS)
            .document(userId)
            .collection(COLLECTION_USER_NOTIFICATIONS);
        
        notificationsRef.whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int count = querySnapshot.size();
                Log.d(TAG, "✅ Unread notifications count: " + count);
                callback.onSuccess(count);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get unread count", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Clear all notifications for a user
     */
    public void clearAllNotifications(String userId) {
        Log.d(TAG, "🗑️ Clearing all notifications for: " + userId);
        
        CollectionReference notificationsRef = db.collection(COLLECTION_NOTIFICATIONS)
            .document(userId)
            .collection(COLLECTION_USER_NOTIFICATIONS);
        
        notificationsRef.get()
            .addOnSuccessListener(querySnapshot -> {
                for (var doc : querySnapshot.getDocuments()) {
                    doc.getReference().delete();
                }
                Log.d(TAG, "✅ Cleared all notifications for: " + userId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to clear notifications", e);
            });
    }
    
    /**
     * Notification data model
     */
    public static class Notification {
        public static final String TYPE_LIKE = "like";
        public static final String TYPE_COMMENT = "comment";
        public static final String TYPE_FOLLOW = "follow";
        public static final String TYPE_MUTUAL_FOLLOWER_RECIPE = "mutual_follower_recipe";
        
        public String notificationId;
        public String type;
        public String actorUserId;
        public String actorName;
        public String message;
        public String recipeId;
        public long timestamp;
        public boolean read = false;
        public long readAt = 0;
        
        public Notification() {}
        
        public Notification(String type, String actorUserId, String actorName, 
                          String message, String recipeId, long timestamp) {
            this.type = type;
            this.actorUserId = actorUserId;
            this.actorName = actorName;
            this.message = message;
            this.recipeId = recipeId;
            this.timestamp = timestamp;
        }
        
        public String getIconEmoji() {
            switch (type) {
                case TYPE_LIKE: return "❤️";
                case TYPE_COMMENT: return "💬";
                case TYPE_FOLLOW: return "👥";
                case TYPE_MUTUAL_FOLLOWER_RECIPE: return "🍳";
                default: return "📢";
            }
        }
    }
    
    // Callback interfaces
    public interface NotificationCallback {
        void onNotificationsUpdated(List<Notification> notifications);
        void onError(String error);
    }
    
    public interface CountCallback {
        void onSuccess(int count);
        void onError(String error);
    }
}
