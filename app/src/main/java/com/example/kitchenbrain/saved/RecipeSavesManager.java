package com.example.kitchenbrain.saved;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecipeSavesManager - Save recipes with collections and cook later features
 * 
 * 🔥 SAVED RECIPES ARCHITECTURE:
 * - Collections for organization (Breakfast, Dinner, etc.)
 * - Cook Later queue for meal planning
 * - Social proof of saved recipes
 * - Firestore path: saved_recipes/{userId}/collections
 */
public class RecipeSavesManager {
    
    private static final String TAG = "RecipeSavesManager";
    private static final String COLLECTION_SAVED_RECIPES = "saved_recipes";
    private static final String COLLECTION_COLLECTIONS = "collections";
    private static final String COLLECTION_COOK_LATER = "cook_later";
    private static final String DEFAULT_COLLECTION = "All Recipes";
    
    private final FirebaseFirestore db;
    
    public RecipeSavesManager() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Save recipe to collection
     */
    public void saveRecipe(String userId, String recipeId, String collectionName, SaveCallback callback) {
        Log.d(TAG, "💾 Saving recipe: " + recipeId + " to collection: " + collectionName);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        // Create saved recipe entry
        SavedRecipe savedRecipe = new SavedRecipe(
            recipeId,
            collectionName,
            System.currentTimeMillis(),
            false // not in cook later queue by default
        );
        
        // Add to user's saved recipes
        userRef.collection("user_saves")
            .document(recipeId)
            .set(savedRecipe)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Recipe saved successfully");
                
                // Update recipe's saved count
                updateRecipeSavedCount(recipeId, true);
                
                callback.onSuccess(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to save recipe", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Remove recipe from saved
     */
    public void unsaveRecipe(String userId, String recipeId, SaveCallback callback) {
        Log.d(TAG, "🗑️ Unsaving recipe: " + recipeId);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        userRef.collection("user_saves")
            .document(recipeId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Recipe unsaved successfully");
                
                // Update recipe's saved count
                updateRecipeSavedCount(recipeId, false);
                
                // Remove from cook later if present
                removeFromCookLater(userId, recipeId);
                
                callback.onSuccess(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to unsave recipe", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Add recipe to cook later queue
     */
    public void addToCookLater(String userId, String recipeId, String mealPlan, SaveCallback callback) {
        Log.d(TAG, "⏰ Adding to cook later: " + recipeId + " for " + mealPlan);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        CookLaterItem cookLaterItem = new CookLaterItem(
            recipeId,
            mealPlan,
            System.currentTimeMillis(),
            false // not cooked yet
        );
        
        userRef.collection(COLLECTION_COOK_LATER)
            .document(recipeId)
            .set(cookLaterItem)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Added to cook later");
                
                // Update saved recipe to mark as in cook later
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("inCookLater", true);
                updateData.put("cookLaterAt", System.currentTimeMillis());
                
                userRef.collection("user_saves")
                    .document(recipeId)
                    .update(updateData);
                
                callback.onSuccess(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to add to cook later", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Remove from cook later queue
     */
    private void removeFromCookLater(String userId, String recipeId) {
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        userRef.collection(COLLECTION_COOK_LATER)
            .document(recipeId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Removed from cook later");
                
                // Update saved recipe
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("inCookLater", false);
                
                userRef.collection("user_saves")
                    .document(recipeId)
                    .update(updateData);
            });
    }
    
    /**
     * Mark recipe as cooked
     */
    public void markAsCooked(String userId, String recipeId, SaveCallback callback) {
        Log.d(TAG, "✅ Marking as cooked: " + recipeId);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("cooked", true);
        updateData.put("cookedAt", System.currentTimeMillis());
        
        userRef.collection(COLLECTION_COOK_LATER)
            .document(recipeId)
            .update(updateData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Recipe marked as cooked");
                callback.onSuccess(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to mark as cooked", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Create custom collection
     */
    public void createCollection(String userId, String collectionName, String description, CollectionCallback callback) {
        Log.d(TAG, "📁 Creating collection: " + collectionName);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        RecipeCollection collection = new RecipeCollection(
            collectionName,
            description,
            System.currentTimeMillis(),
            0 // start with 0 recipes
        );
        
        userRef.collection(COLLECTION_COLLECTIONS)
            .document(collectionName)
            .set(collection)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Collection created");
                callback.onSuccess(collection);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to create collection", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Get user's saved recipes
     */
    public void getSavedRecipes(String userId, String collectionName, SavedRecipesCallback callback) {
        Log.d(TAG, "📚 Getting saved recipes from collection: " + collectionName);
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        Query query = userRef.collection("user_saves");
        if (!collectionName.equals("All")) {
            query = query.whereEqualTo("collectionName", collectionName);
        }
        
        query.orderBy("savedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SavedRecipe> savedRecipes = new ArrayList<>();
                for (var doc : querySnapshot.getDocuments()) {
                    SavedRecipe savedRecipe = doc.toObject(SavedRecipe.class);
                    if (savedRecipe != null) {
                        savedRecipe.recipeId = doc.getId();
                        savedRecipes.add(savedRecipe);
                    }
                }
                
                Log.d(TAG, "✅ Loaded " + savedRecipes.size() + " saved recipes");
                callback.onSuccess(savedRecipes);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get saved recipes", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Get cook later queue
     */
    public void getCookLaterQueue(String userId, CookLaterCallback callback) {
        Log.d(TAG, "⏰ Getting cook later queue");
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        userRef.collection(COLLECTION_COOK_LATER)
            .whereEqualTo("cooked", false)
            .orderBy("addedAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<CookLaterItem> cookLaterItems = new ArrayList<>();
                for (var doc : querySnapshot.getDocuments()) {
                    CookLaterItem item = doc.toObject(CookLaterItem.class);
                    if (item != null) {
                        item.recipeId = doc.getId();
                        cookLaterItems.add(item);
                    }
                }
                
                Log.d(TAG, "✅ Loaded " + cookLaterItems.size() + " cook later items");
                callback.onSuccess(cookLaterItems);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get cook later queue", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Get user's collections
     */
    public void getUserCollections(String userId, CollectionsCallback callback) {
        Log.d(TAG, "📁 Getting user collections");
        
        DocumentReference userRef = db.collection(COLLECTION_SAVED_RECIPES).document(userId);
        
        userRef.collection(COLLECTION_COLLECTIONS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<RecipeCollection> collections = new ArrayList<>();
                
                // Always include "All Recipes"
                collections.add(new RecipeCollection("All", "All your saved recipes", 0, 0));
                
                for (var doc : querySnapshot.getDocuments()) {
                    RecipeCollection collection = doc.toObject(RecipeCollection.class);
                    if (collection != null) {
                        collections.add(collection);
                    }
                }
                
                Log.d(TAG, "✅ Loaded " + collections.size() + " collections");
                callback.onSuccess(collections);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get collections", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Update recipe's saved count
     */
    private void updateRecipeSavedCount(String recipeId, boolean increment) {
        // This would update the recipe document's saved count
        // Implementation depends on recipe repository structure
        Log.d(TAG, "📊 Updating recipe saved count: " + recipeId + " increment: " + increment);
    }
    
    /**
     * Data models
     */
    public static class SavedRecipe {
        public String recipeId;
        public String collectionName;
        public long savedAt;
        public boolean inCookLater = false;
        public long cookLaterAt = 0;
        
        public SavedRecipe() {}
        
        public SavedRecipe(String recipeId, String collectionName, long savedAt, boolean inCookLater) {
            this.recipeId = recipeId;
            this.collectionName = collectionName;
            this.savedAt = savedAt;
            this.inCookLater = inCookLater;
        }
    }
    
    public static class CookLaterItem {
        public String recipeId;
        public String mealPlan;
        public long addedAt;
        public boolean cooked = false;
        public long cookedAt = 0;
        
        public CookLaterItem() {}
        
        public CookLaterItem(String recipeId, String mealPlan, long addedAt, boolean cooked) {
            this.recipeId = recipeId;
            this.mealPlan = mealPlan;
            this.addedAt = addedAt;
            this.cooked = cooked;
        }
    }
    
    public static class RecipeCollection {
        public String name;
        public String description;
        public long createdAt;
        public int recipeCount;
        
        public RecipeCollection() {}
        
        public RecipeCollection(String name, String description, long createdAt, int recipeCount) {
            this.name = name;
            this.description = description;
            this.createdAt = createdAt;
            this.recipeCount = recipeCount;
        }
    }
    
    // Callback interfaces
    public interface SaveCallback {
        void onSuccess(boolean saved);
        void onError(String error);
    }
    
    public interface SavedRecipesCallback {
        void onSuccess(List<SavedRecipe> recipes);
        void onError(String error);
    }
    
    public interface CookLaterCallback {
        void onSuccess(List<CookLaterItem> items);
        void onError(String error);
    }
    
    public interface CollectionsCallback {
        void onSuccess(List<RecipeCollection> collections);
        void onError(String error);
    }
    
    public interface CollectionCallback {
        void onSuccess(RecipeCollection collection);
        void onError(String error);
    }
}
