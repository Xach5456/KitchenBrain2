package com.example.kitchenbrain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Home data operations
 * Clean architecture - separates data logic from UI
 */
public class HomeRepository {
    
    private final FirebaseFirestore db;
    private static final String TAG = "HomeRepository";
    
    public HomeRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Get recipes with real-time listener
     * Supports pagination and sorting
     */
    public void getRecipes(int limit, RecipeLoadCallback callback) {
        db.collection("recipes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError("Failed to load recipes: " + error.getMessage());
                        return;
                    }
                    
                    List<Recipe> recipes = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            recipe.setId(doc.getId());
                            recipes.add(recipe);
                        }
                    }
                    callback.onSuccess(recipes);
                });
    }
    
    /**
     * Like a recipe
     */
    public void likeRecipe(String recipeId, String userId, boolean isLiked, ResultCallback callback) {
        db.collection("recipes").document(recipeId)
                .update("likesCount", isLiked ? 
                        com.google.firebase.firestore.FieldValue.increment(1) : 
                        com.google.firebase.firestore.FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    // Track user's like in subcollection
                    db.collection("recipes").document(recipeId)
                            .collection("likes").document(userId)
                            .set(isLiked ? 
                                    java.util.Collections.singletonMap("userId", userId) : 
                                    java.util.Collections.emptyMap())
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError("Failed to track like: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Failed to like recipe: " + e.getMessage()));
    }
    
    /**
     * Save/Bookmark a recipe
     */
    public void saveRecipe(String recipeId, String userId, boolean isSaved, ResultCallback callback) {
        if (isSaved) {
            // Save the recipe
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("userId", userId);
            data.put("savedAt", com.google.firebase.Timestamp.now());
            
            db.collection("users").document(userId)
                    .collection("savedRecipes").document(recipeId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to save recipe: " + e.getMessage()));
        } else {
            // Unsave the recipe
            db.collection("users").document(userId)
                    .collection("savedRecipes").document(recipeId)
                    .delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to unsave recipe: " + e.getMessage()));
        }
    }
    
    /**
     * Check if user has liked a recipe
     */
    public void isRecipeLiked(String recipeId, String userId, LikedCallback callback) {
        db.collection("recipes").document(recipeId)
                .collection("likes").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    callback.onLiked(doc.exists());
                })
                .addOnFailureListener(e -> {
                    callback.onError("Failed to check like status: " + e.getMessage());
                });
    }
    
    /**
     * Check if user has saved a recipe
     */
    public void isRecipeSaved(String recipeId, String userId, SavedCallback callback) {
        db.collection("users").document(userId)
                .collection("savedRecipes").document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    callback.onSaved(doc.exists());
                })
                .addOnFailureListener(e -> {
                    callback.onError("Failed to check save status: " + e.getMessage());
                });
    }
    
    // Callbacks
    public interface RecipeLoadCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
    }
    
    public interface ResultCallback {
        void onSuccess(Object result);
        void onError(String error);
    }
    
    public interface LikedCallback {
        void onLiked(boolean isLiked);
        void onError(String error);
    }
    
    public interface SavedCallback {
        void onSaved(boolean isSaved);
        void onError(String error);
    }
}
