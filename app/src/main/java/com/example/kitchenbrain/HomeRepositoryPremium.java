package com.example.kitchenbrain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Production-Grade Repository for Home Screen
 * Real Firestore integration with pagination and real-time updates
 */
public class HomeRepositoryPremium {
    
    private static final String TAG = "HomeRepository";
    private final FirebaseFirestore db;
    private final CollectionReference recipesCollection;
    private final ExecutorService executor;
    
    // Pagination
    private Recipe lastVisibleRecipe;
    private static final int PAGE_SIZE = 10;
    private boolean hasMoreData = true;
    
    public HomeRepositoryPremium() {
        this.db = FirebaseFirestore.getInstance();
        this.recipesCollection = db.collection("recipes");
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Load initial page of recipes (newest first)
     */
    public void loadFirstPage(RecipesCallback callback) {
        executor.execute(() -> {
            try {
                Query query = recipesCollection
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(PAGE_SIZE);
                
                query.get().addOnSuccessListener(querySnapshot -> {
                    List<Recipe> recipes = parseRecipes(querySnapshot);
                    
                    if (!recipes.isEmpty()) {
                        lastVisibleRecipe = recipes.get(recipes.size() - 1);
                        hasMoreData = recipes.size() == PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    
                    callback.onSuccess(recipes);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recipes", e);
                    callback.onError("Failed to load recipes: " + e.getMessage());
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in loadFirstPage", e);
                callback.onError("Database error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Load next page (pagination)
     */
    public void loadNextPage(RecipesCallback callback) {
        if (!hasMoreData || lastVisibleRecipe == null) {
            callback.onComplete();
            return;
        }
        
        executor.execute(() -> {
            try {
                Query query = recipesCollection
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .startAfter(lastVisibleRecipe.getCreatedAt())
                        .limit(PAGE_SIZE);
                
                query.get().addOnSuccessListener(querySnapshot -> {
                    List<Recipe> recipes = parseRecipes(querySnapshot);
                    
                    if (!recipes.isEmpty()) {
                        lastVisibleRecipe = recipes.get(recipes.size() - 1);
                        hasMoreData = recipes.size() == PAGE_SIZE;
                        callback.onSuccess(recipes);
                    } else {
                        hasMoreData = false;
                        callback.onComplete();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading next page", e);
                    callback.onError("Failed to load more: " + e.getMessage());
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in loadNextPage", e);
                callback.onError("Pagination error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Real-time listener for recipe updates
     */
    public ListenerRegistration addRecipesListener(RealTimeListenerCallback callback) {
        return recipesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    
                    if (snapshot != null) {
                        List<Recipe> recipes = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            recipe.setId(doc.getId());
                            recipes.add(recipe);
                        }
                        callback.onRecipesUpdated(recipes);
                    }
                });
    }
    
    /**
     * Like/Unlike recipe with atomic update
     */
    public Task<Void> toggleLike(String recipeId, String userId, boolean isLiked) {
        return Tasks.call(executor, (Callable<Void>) () -> {
            Map<String, Object> updates = new HashMap<>();
            
            // Update likes count atomically
            updates.put("likesCount", isLiked ? 
                com.google.firebase.firestore.FieldValue.increment(-1) : 
                com.google.firebase.firestore.FieldValue.increment(1));
            
            recipesCollection.document(recipeId).update(updates);
            return null;
        });
    }
    
    /**
     * Save/Bookmark recipe
     */
    public Task<Void> saveRecipe(String recipeId, String userId, boolean isSaved) {
        if (isSaved) {
            // Remove from saved
            return db.collection("users").document(userId)
                    .collection("savedRecipes")
                    .document(recipeId)
                    .delete();
        } else {
            // Add to saved
            Map<String, Object> data = new HashMap<>();
            data.put("recipeId", recipeId);
            data.put("userId", userId);
            data.put("savedAt", com.google.firebase.Timestamp.now());
            
            return db.collection("users").document(userId)
                    .collection("savedRecipes")
                    .document(recipeId)
                    .set(data);
        }
    }
    
    /**
     * Check if user liked recipe
     */
    public Task<Boolean> isRecipeLiked(String recipeId, String userId) {
        return Tasks.call(executor, () -> {
            DocumentSnapshot doc = db.collection("recipes")
                    .document(recipeId)
                    .collection("likes")
                    .document(userId)
                    .get()
                    .getResult();
            return doc.exists();
        });
    }
    
    /**
     * Check if user saved recipe
     */
    public Task<Boolean> isRecipeSaved(String recipeId, String userId) {
        return Tasks.call(executor, () -> {
            DocumentSnapshot doc = db.collection("users")
                    .document(userId)
                    .collection("savedRecipes")
                    .document(recipeId)
                    .get()
                    .getResult();
            return doc.exists();
        });
    }
    
    /**
     * Parse recipes from Firestore snapshot
     */
    private List<Recipe> parseRecipes(QuerySnapshot snapshot) {
        List<Recipe> recipes = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            try {
                Recipe recipe = doc.toObject(Recipe.class);
                recipe.setId(doc.getId());
                recipes.add(recipe);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing recipe", e);
            }
        }
        return recipes;
    }
    
    /**
     * Reset pagination
     */
    public void resetPagination() {
        lastVisibleRecipe = null;
        hasMoreData = true;
    }
    
    // Callbacks
    public interface RecipesCallback {
        void onSuccess(List<Recipe> recipes);
        void onError(String error);
        void onComplete();
    }
    
    public interface RealTimeListenerCallback {
        void onRecipesUpdated(List<Recipe> recipes);
        void onError(String error);
    }
}
