package com.example.kitchenbrain.social;

import android.util.Log;
import com.example.kitchenbrain.model.Comment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommentsManager - Instagram-style Real-time Comments System for Social Recipes
 * 
 * 🔥 REALTIME COMMENTS ARCHITECTURE:
 * - Live comment updates with snapshot listeners
 * - Automatic comment count updates
 * - Optimized for social recipes
 * - Firestore path: recipes/{recipeId}/comments
 */
public class CommentsManager {
    
    private static final String TAG = "CommentsManager";
    private static final String COLLECTION_RECIPES = "recipes";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final int COMMENTS_LIMIT = 50; // Limit for performance
    
    private final FirebaseFirestore db;
    
    public CommentsManager() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Add comment to a recipe with automatic count update
     * @param recipeId The recipe ID
     * @param userId Current user ID
     * @param userName User display name
     * @param userAvatar User avatar URL
     * @param text Comment text
     * @param callback Result callback
     */
    public void addComment(String recipeId, String userId, String userName, String userAvatar, String text, CommentCallback callback) {
        Log.d(TAG, "💬 Add comment: recipeId=" + recipeId + ", userId=" + userId);
        
        // Create comment with full user info
        Comment comment = new Comment(userId, userName, userAvatar, text, System.currentTimeMillis());
        
        DocumentReference recipeRef = db.collection(COLLECTION_RECIPES).document(recipeId);
        
        // Use transaction to add comment and update count atomically
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 🔥 CRITICAL: READS MUST COME BEFORE WRITES in Firestore Transactions
            
            // 1. READ recipe doc first
            DocumentSnapshot recipeDoc = transaction.get(recipeRef);
            
            // 2. Perform WRITES
            CollectionReference commentsRef = recipeRef.collection(COLLECTION_COMMENTS);
            DocumentReference commentRef = commentsRef.document();
            transaction.set(commentRef, comment);
            
            // Update recipe comment count
            if (recipeDoc.exists()) {
                Long currentCount = recipeDoc.getLong("comments");
                long newCount = (currentCount != null ? currentCount : 0) + 1;
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("comments", newCount);
                updateData.put("updatedAt", FieldValue.serverTimestamp());
                
                transaction.set(recipeRef, updateData, SetOptions.merge());
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "✅ Comment added and count updated");
            comment.commentId = recipeId + "_" + System.currentTimeMillis(); // Generate client-side ID
            callback.onSuccess(comment);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Failed to add comment", e);
            callback.onError(e.getMessage());
        });
    }
    
    /**
     * Listen to real-time comments for a recipe with performance optimizations
     * @param recipeId The recipe ID
     * @param callback Comments update callback
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToComments(String recipeId, CommentsUpdateCallback callback) {
        Log.d(TAG, "👂 Listen to comments: recipeId=" + recipeId);
        
        CollectionReference commentsRef = db.collection(COLLECTION_RECIPES)
            .document(recipeId)
            .collection(COLLECTION_COMMENTS);
        
        return commentsRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(COMMENTS_LIMIT)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "❌ Comments listener error", error);
                    callback.onError(error.getMessage());
                    return;
                }
                
                List<Comment> comments = new ArrayList<>();
                if (snapshots != null) {
                    for (var doc : snapshots.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.commentId = doc.getId(); // Use document ID as comment ID
                            comments.add(comment);
                        }
                    }
                }
                
                Log.d(TAG, "📥 Comments updated: " + comments.size() + " comments");
                callback.onCommentsUpdated(comments);
            });
    }
    
    /**
     * Get comments count for a recipe (from recipe document, faster)
     * @param recipeId The recipe ID
     * @param callback Count callback
     */
    public void getCommentsCount(String recipeId, CountCallback callback) {
        Log.d(TAG, "🔢 Get comments count: recipeId=" + recipeId);
        
        DocumentReference recipeRef = db.collection(COLLECTION_RECIPES).document(recipeId);
        
        recipeRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long count = documentSnapshot.getLong("comments");
                    int commentCount = count != null ? count.intValue() : 0;
                    Log.d(TAG, "✅ Comments count: " + commentCount);
                    callback.onSuccess(commentCount);
                } else {
                    Log.w(TAG, "⚠️ Recipe not found: " + recipeId);
                    callback.onSuccess(0);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get comments count", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Delete a comment with automatic count update
     * @param recipeId The recipe ID
     * @param commentId The comment ID
     * @param callback Result callback
     */
    public void deleteComment(String recipeId, String commentId, CommentCallback callback) {
        Log.d(TAG, "🗑️ Delete comment: recipeId=" + recipeId + ", commentId=" + commentId);
        
        DocumentReference recipeRef = db.collection(COLLECTION_RECIPES).document(recipeId);
        
        // Use transaction to delete comment and update count
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 🔥 CRITICAL: READS MUST COME BEFORE WRITES
            
            // 1. READ recipe doc first
            DocumentSnapshot recipeDoc = transaction.get(recipeRef);
            
            // 2. Perform WRITES
            DocumentReference commentRef = recipeRef.collection(COLLECTION_COMMENTS).document(commentId);
            transaction.delete(commentRef);
            
            // Update recipe comment count
            if (recipeDoc.exists()) {
                Long currentCount = recipeDoc.getLong("comments");
                long newCount = (currentCount != null ? currentCount : 0) - 1;
                newCount = Math.max(0, newCount); // Don't go below 0
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("comments", newCount);
                updateData.put("updatedAt", FieldValue.serverTimestamp());
                
                transaction.set(recipeRef, updateData, SetOptions.merge());
            }
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "✅ Comment deleted and count updated");
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Failed to delete comment", e);
            callback.onError(e.getMessage());
        });
    }
    
    /**
     * Get paginated comments for performance
     * @param recipeId The recipe ID
     * @param lastTimestamp Last comment timestamp for pagination (null for first page)
     * @param callback Comments callback
     */
    public void getPaginatedComments(String recipeId, Long lastTimestamp, CommentsUpdateCallback callback) {
        Log.d(TAG, "📄 Get paginated comments: recipeId=" + recipeId);
        
        CollectionReference commentsRef = db.collection(COLLECTION_RECIPES)
            .document(recipeId)
            .collection(COLLECTION_COMMENTS);
        
        Query query = commentsRef.orderBy("timestamp", Query.Direction.ASCENDING).limit(20);
        
        if (lastTimestamp != null) {
            query = query.startAfter(lastTimestamp);
        }
        
        query.get()
            .addOnSuccessListener(querySnapshot -> {
                List<Comment> comments = new ArrayList<>();
                for (var doc : querySnapshot.getDocuments()) {
                    Comment comment = doc.toObject(Comment.class);
                    if (comment != null) {
                        comment.commentId = doc.getId();
                        comments.add(comment);
                    }
                }
                
                Log.d(TAG, "✅ Paginated comments loaded: " + comments.size());
                callback.onCommentsUpdated(comments);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get paginated comments", e);
                callback.onError(e.getMessage());
            });
    }
    
    // Callbacks
    public interface CommentCallback {
        void onSuccess(Comment comment);
        void onError(String error);
    }
    
    public interface CommentsUpdateCallback {
        void onCommentsUpdated(List<Comment> comments);
        void onError(String error);
    }
    
    public interface CountCallback {
        void onSuccess(int count);
        void onError(String error);
    }
}
