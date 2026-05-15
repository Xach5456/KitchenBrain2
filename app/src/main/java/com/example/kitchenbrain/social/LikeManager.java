package com.example.kitchenbrain.social;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LikeManager - Instagram-style Like System with Firestore Transactions
 * 
 * ✅ INSTAGRAM ARCHITECTURE:
 * - Atomic like/unlike operations
 * - Real-time updates
 * - Prevents race conditions
 */
public class LikeManager {
    
    private static final String TAG = "LikeManager";
    private static final String COLLECTION_POSTS = "posts";
    
    private final FirebaseFirestore db;
    
    public LikeManager() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Toggle like status with atomic transaction (FIXED)
     * @param postId The post ID
     * @param userId Current user ID
     * @param callback Result callback
     */
    public void toggleLike(String postId, String userId, LikeCallback callback) {
        Log.d(TAG, "🔥 Toggle like: postId=" + postId + ", userId=" + userId);
        
        DocumentReference postRef = db.collection(COLLECTION_POSTS).document(postId);
        
        db.runTransaction(transaction -> {
            Log.d(TAG, "📝 Transaction started for postId: " + postId);
            
            DocumentSnapshot snapshot = transaction.get(postRef);
            
            List<String> likedBy;
            long likeCount;
            
            // 🔥 CRITICAL FIX: All reads must be before all writes.
            // We handle the non-existent document case by initializing local variables instead of doing a write-then-read.
            if (snapshot.exists()) {
                likedBy = (List<String>) snapshot.get("likedBy");
                Long count = snapshot.getLong("likeCount");
                
                if (likedBy == null) likedBy = new ArrayList<>();
                likeCount = (count != null) ? count : 0L;
            } else {
                Log.d(TAG, "📝 Document doesn't exist, will create it during commit");
                likedBy = new ArrayList<>();
                likeCount = 0L;
            }
            
            boolean wasLiked = likedBy.contains(userId);
            boolean isNowLiked = !wasLiked;
            
            if (isNowLiked) {
                // Add like
                likedBy.add(userId);
                likeCount++;
                Log.d(TAG, "❤️ Added like: new count=" + likeCount);
            } else {
                // Remove like
                likedBy.remove(userId);
                likeCount = Math.max(0, likeCount - 1);
                Log.d(TAG, "💔 Removed like: new count=" + likeCount);
            }
            
            // Update document atomically with merge to handle both creation and updates.
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("likedBy", likedBy);
            updateData.put("likeCount", likeCount);
            
            // Add metadata only if it's a new document
            if (!snapshot.exists()) {
                updateData.put("createdAt", System.currentTimeMillis());
            }
            
            transaction.set(postRef, updateData, SetOptions.merge());
            
            Log.d(TAG, "✅ Transaction update prepared: isLiked=" + isNowLiked + ", count=" + likeCount);
            
            return new LikeResult(isNowLiked, (int) likeCount);
        }).addOnSuccessListener(result -> {
            Log.d(TAG, "🎉 Like toggle success: " + result.toString());
            callback.onSuccess(result);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Like toggle failed", e);
            callback.onError(e.getMessage());
        });
    }
    
    /**
     * Check if user has liked a post
     * @param postId The post ID
     * @param userId Current user ID
     * @param callback Result callback
     */
    public void checkLikeStatus(String postId, String userId, LikeStatusCallback callback) {
        Log.d(TAG, "🔍 Check like status: postId=" + postId + ", userId=" + userId);
        
        db.collection(COLLECTION_POSTS).document(postId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    List<String> likedBy = (List<String>) snapshot.get("likedBy");
                    boolean isLiked = likedBy != null && likedBy.contains(userId);
                    Long likeCount = snapshot.getLong("likeCount");
                    
                    LikeResult result = new LikeResult(isLiked, likeCount != null ? likeCount.intValue() : 0);
                    Log.d(TAG, "✅ Like status checked: " + result.toString());
                    callback.onSuccess(result);
                } else {
                    Log.w(TAG, "⚠️ Post not found: " + postId);
                    callback.onError("Post not found");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to check like status", e);
                callback.onError(e.getMessage());
            });
    }
    
    // Result data class
    public static class LikeResult {
        public final boolean isLiked;
        public final int likeCount;
        
        public LikeResult(boolean isLiked, int likeCount) {
            this.isLiked = isLiked;
            this.likeCount = likeCount;
        }
        
        @Override
        public String toString() {
            return "LikeResult{isLiked=" + isLiked + ", likeCount=" + likeCount + "}";
        }
    }
    
    // Callbacks
    public interface LikeCallback {
        void onSuccess(LikeResult result);
        void onError(String error);
    }
    
    public interface LikeStatusCallback {
        void onSuccess(LikeResult result);
        void onError(String error);
    }
}
