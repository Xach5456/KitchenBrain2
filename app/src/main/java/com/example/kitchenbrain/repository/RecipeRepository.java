package com.example.kitchenbrain.repository;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.example.kitchenbrain.model.SocialRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecipeRepository - Firestore operations for social recipes
 * 
 * 🔥 SOCIAL COOKING ARCHITECTURE:
 * - User-generated recipes with social features
 * - Priority ranking: mutual followers > followers > everyone
 * - Atomic operations for likes/comments/saves
 */
public class RecipeRepository {
    
    private static final String TAG = "RecipeRepository";
    
    // Firestore Collections
    private static final String COLLECTION_RECIPES = "recipes";
    private static final String COLLECTION_RECIPE_LIKES = "recipe_likes";
    private static final String COLLECTION_RECIPE_COMMENTS = "recipe_comments";
    private static final String COLLECTION_SAVED_RECIPES = "saved_recipes";
    
    private final FirebaseFirestore db;
    private final CollectionReference recipesCollection;
    
    public RecipeRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.recipesCollection = db.collection(COLLECTION_RECIPES);
    }
    
    /**
     * Create a new recipe
     */
    public void createRecipe(SocialRecipe recipe, RecipeCallback<String> callback) {
        Log.d(TAG, "🔥 Creating recipe: " + recipe.getTitle());
        
        // Generate unique recipe ID
        DocumentReference recipeRef = recipesCollection.document();
        recipe.setRecipeId(recipeRef.getId());
        
        recipeRef.set(recipe)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "✅ Recipe created successfully: " + recipe.getRecipeId());
                callback.onSuccess(recipe.getRecipeId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to create recipe", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Search recipes with social priority
     * 🔥 PRIORITY: mutual followers > followers > global
     */
    public void searchRecipes(String currentUserId, List<String> userIngredients, 
                           List<String> mutualFollowers, List<String> followers,
                           RecipeCallback<List<SocialRecipe>> callback) {
        
        Log.d(TAG, "🔥 Searching recipes with ingredients: " + userIngredients);
        
        // Create queries for different priority levels
        List<Query> queries = new ArrayList<>();
        
        // 1. Mutual followers recipes (highest priority)
        if (!mutualFollowers.isEmpty()) {
            Query mutualQuery = recipesCollection
                .whereIn("authorId", mutualFollowers)
                .whereArrayContainsAny("ingredients", userIngredients)
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(10);
            queries.add(mutualQuery);
        }
        
        // 2. Followers recipes (medium priority)
        if (!followers.isEmpty()) {
            Query followersQuery = recipesCollection
                .whereIn("authorId", followers)
                .whereArrayContainsAny("ingredients", userIngredients)
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(20);
            queries.add(followersQuery);
        }
        
        // 3. Global recipes (lowest priority)
        Query globalQuery = recipesCollection
            .whereArrayContainsAny("ingredients", userIngredients)
            .orderBy("likes", Query.Direction.DESCENDING)
            .limit(50);
        queries.add(globalQuery);
        
        // Execute all queries and merge results
        executeMultipleQueries(queries, callback);
    }
    
    /**
     * Get recipe by ID
     */
    public void getRecipe(String recipeId, RecipeCallback<SocialRecipe> callback) {
        Log.d(TAG, "🔥 Getting recipe: " + recipeId);
        
        recipesCollection.document(recipeId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    SocialRecipe recipe = documentSnapshot.toObject(SocialRecipe.class);
                    Log.d(TAG, "✅ Recipe loaded: " + recipe.getTitle());
                    callback.onSuccess(recipe);
                } else {
                    Log.w(TAG, "⚠️ Recipe not found: " + recipeId);
                    callback.onError("Recipe not found");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to load recipe", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Toggle like on recipe with atomic transaction
     */
    public void toggleLike(String recipeId, String userId, RecipeCallback<Boolean> callback) {
        Log.d(TAG, "🔥 Toggling like: recipe=" + recipeId + ", user=" + userId);
        
        DocumentReference recipeRef = recipesCollection.document(recipeId);
        
        db.runTransaction(transaction -> {
            SocialRecipe recipe = transaction.get(recipeRef).toObject(SocialRecipe.class);
            
            if (recipe == null) {
                throw new RuntimeException("Recipe not found");
            }
            
            Map<String, Boolean> likedBy = recipe.getLikedBy();
            if (likedBy == null) {
                likedBy = new HashMap<>();
            }
            
            boolean isLiked = likedBy.containsKey(userId);
            long likes = recipe.getLikes();
            
            if (isLiked) {
                // Remove like
                likedBy.remove(userId);
                likes--;
            } else {
                // Add like
                likedBy.put(userId, true);
                likes++;
            }
            
            // Update with merge to prevent "document doesn't exist" errors
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("likedBy", likedBy);
            updateData.put("likes", likes);
            updateData.put("updatedAt", FieldValue.serverTimestamp());
            
            transaction.set(recipeRef, updateData, SetOptions.merge());
            
            return !isLiked; // Return new like status
        }).addOnSuccessListener(isLiked -> {
            Log.d(TAG, "✅ Like toggled successfully: " + isLiked);
            callback.onSuccess(isLiked);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Failed to toggle like", e);
            callback.onError(e.getMessage());
        });
    }
    
    /**
     * Toggle save on recipe
     */
    public void toggleSave(String recipeId, String userId, RecipeCallback<Boolean> callback) {
        Log.d(TAG, "🔥 Toggling save: recipe=" + recipeId + ", user=" + userId);
        
        DocumentReference recipeRef = recipesCollection.document(recipeId);
        
        db.runTransaction(transaction -> {
            SocialRecipe recipe = transaction.get(recipeRef).toObject(SocialRecipe.class);
            
            if (recipe == null) {
                throw new RuntimeException("Recipe not found");
            }
            
            Map<String, Boolean> savedBy = recipe.getSavedBy();
            if (savedBy == null) {
                savedBy = new HashMap<>();
            }
            
            boolean isSaved = savedBy.containsKey(userId);
            long saves = recipe.getSaves();
            
            if (isSaved) {
                // Remove save
                savedBy.remove(userId);
                saves--;
            } else {
                // Add save
                savedBy.put(userId, true);
                saves++;
            }
            
            // Update with merge
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("savedBy", savedBy);
            updateData.put("saves", saves);
            updateData.put("updatedAt", FieldValue.serverTimestamp());
            
            transaction.set(recipeRef, updateData, SetOptions.merge());
            
            return !isSaved; // Return new save status
        }).addOnSuccessListener(isSaved -> {
            Log.d(TAG, "✅ Save toggled successfully: " + isSaved);
            callback.onSuccess(isSaved);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Failed to toggle save", e);
            callback.onError(e.getMessage());
        });
    }
    
    /**
     * Get user's created recipes
     */
    public void getUserRecipes(String userId, RecipeCallback<List<SocialRecipe>> callback) {
        Log.d(TAG, "🔥 Getting user recipes: " + userId);
        
        recipesCollection
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SocialRecipe> recipes = new ArrayList<>();
                for (var doc : querySnapshot.getDocuments()) {
                    SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                    recipes.add(recipe);
                }
                Log.d(TAG, "✅ Loaded " + recipes.size() + " user recipes");
                callback.onSuccess(recipes);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to load user recipes", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Get user's saved recipes
     */
    public void getSavedRecipes(String userId, RecipeCallback<List<SocialRecipe>> callback) {
        Log.d(TAG, "🔥 Getting saved recipes: " + userId);
        
        recipesCollection
            .whereEqualTo("savedBy." + userId, true)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SocialRecipe> recipes = new ArrayList<>();
                for (var doc : querySnapshot.getDocuments()) {
                    SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                    recipes.add(recipe);
                }
                Log.d(TAG, "✅ Loaded " + recipes.size() + " saved recipes");
                callback.onSuccess(recipes);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to load saved recipes", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Execute multiple queries and merge results
     */
    private void executeMultipleQueries(List<Query> queries, RecipeCallback<List<SocialRecipe>> callback) {
        List<SocialRecipe> allRecipes = new ArrayList<>();
        int[] completedQueries = {0};
        
        for (Query query : queries) {
            query.get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                        if (recipe != null && !allRecipes.contains(recipe)) {
                            allRecipes.add(recipe);
                        }
                    }
                    
                    completedQueries[0]++;
                    if (completedQueries[0] == queries.size()) {
                        Log.d(TAG, "✅ Search completed: " + allRecipes.size() + " recipes found");
                        callback.onSuccess(allRecipes);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Query failed", e);
                    callback.onError(e.getMessage());
                });
        }
    }
    
    // Callback interface
    public interface RecipeCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
