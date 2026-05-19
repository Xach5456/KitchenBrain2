package com.example.kitchenbrain.repository;

import android.util.Log;
import android.text.TextUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.example.kitchenbrain.model.SocialRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecipeRepository - Firestore operations for social recipes
 */
public class RecipeRepository {
    
    private static final String TAG = "RecipeRepository";
    
    private static final String COLLECTION_RECIPES = "recipes";
    
    private final FirebaseFirestore db;
    private final CollectionReference recipesCollection;
    
    public RecipeRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.recipesCollection = db.collection(COLLECTION_RECIPES);
    }
    
    public void createRecipe(SocialRecipe recipe, RecipeCallback<String> callback) {
        DocumentReference recipeRef = recipesCollection.document();
        recipe.setRecipeId(recipeRef.getId());
        
        recipeRef.set(recipe)
            .addOnSuccessListener(documentReference -> callback.onSuccess(recipe.getRecipeId()))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateRecipe(SocialRecipe recipe, RecipeCallback<Void> callback) {
        if (recipe.getRecipeId() == null || recipe.getRecipeId().isEmpty()) {
            callback.onError("Recipe ID is missing");
            return;
        }
        recipe.setAuthorId(FirebaseAuth.getInstance().getUid());

        runOwnerValidatedWrite(recipe.getRecipeId(), callback, documentReference ->
            documentReference
                .set(recipe, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()))
        );
    }

    public void updateRecipeFields(String recipeId, Map<String, Object> updates, RecipeCallback<Void> callback) {
        if (recipeId == null || recipeId.isEmpty()) {
            callback.onError("Recipe ID is missing");
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onError("No recipe changes to save");
            return;
        }
        updates.remove("authorId");
        updates.remove("createdBy");

        runOwnerValidatedWrite(recipeId, callback, documentReference ->
            documentReference
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()))
        );
    }

    public void deleteRecipe(String recipeId, RecipeCallback<Void> callback) {
        if (recipeId == null || recipeId.isEmpty()) {
            callback.onError("Recipe ID is missing");
            return;
        }

        runOwnerValidatedWrite(recipeId, callback, documentReference ->
            documentReference
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()))
        );
    }

    private void runOwnerValidatedWrite(
            String recipeId,
            RecipeCallback<Void> callback,
            OwnerValidatedWrite write
    ) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (TextUtils.isEmpty(currentUserId)) {
            callback.onError("You must be signed in to modify recipes");
            return;
        }

        DocumentReference recipeRef = recipesCollection.document(recipeId);
        recipeRef.get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    callback.onError("Recipe not found");
                    return;
                }

                String ownerId = getRecipeOwnerId(snapshot);
                if (TextUtils.isEmpty(ownerId) || !currentUserId.equals(ownerId)) {
                    callback.onError("You can only modify recipes you created");
                    return;
                }

                write.execute(recipeRef);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private String getRecipeOwnerId(DocumentSnapshot snapshot) {
        String authorId = snapshot.getString("authorId");
        if (!TextUtils.isEmpty(authorId)) return authorId;
        return snapshot.getString("createdBy");
    }

    private interface OwnerValidatedWrite {
        void execute(DocumentReference documentReference);
    }
    
    public void getRecipe(String recipeId, RecipeCallback<SocialRecipe> callback) {
        recipesCollection.document(recipeId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    SocialRecipe recipe = documentSnapshot.toObject(SocialRecipe.class);
                    if (recipe != null) recipe.setId(documentSnapshot.getId());
                    callback.onSuccess(recipe);
                } else {
                    callback.onError("Recipe not found");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Search recipes based on ingredients and social context.
     */
    public void searchRecipes(String currentUserId, List<String> ingredients, List<String> mutualFollowers, List<String> followers, RecipeCallback<List<SocialRecipe>> callback) {
        Query query = recipesCollection;
        
        // Basic filtering by ingredients
        if (ingredients != null && !ingredients.isEmpty()) {
            // Firestore array-contains-any can take up to 10 items
            List<String> limitedIngredients = ingredients.subList(0, Math.min(ingredients.size(), 10));
            query = query.whereArrayContainsAny("ingredients", limitedIngredients);
        }
        
        // Filter by authors if provided
        List<String> authors = new ArrayList<>();
        if (mutualFollowers != null && !mutualFollowers.isEmpty()) authors.addAll(mutualFollowers);
        if (followers != null && !followers.isEmpty()) authors.addAll(followers);
        
        if (!authors.isEmpty()) {
            // Firestore 'in' query can take up to 10 items
            query = query.whereIn("authorId", authors.subList(0, Math.min(authors.size(), 10)));
        }

        query.limit(50).get()
            .addOnSuccessListener(querySnapshot -> {
                List<SocialRecipe> recipes = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                    if (recipe != null) {
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }
                }
                callback.onSuccess(recipes);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    /**
     * 🔥 SCHEMA-AGNOSTIC TOGGLE LIKE
     * Handles both List<String> and Map<String, Boolean> formats for likedBy field.
     */
    public void toggleLike(String recipeId, String userId, RecipeCallback<Boolean> callback) {
        Log.d(TAG, "🔥 Toggling like: recipe=" + recipeId + ", user=" + userId);
        
        DocumentReference recipeRef = recipesCollection.document(recipeId);
        
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(recipeRef);
            
            if (!snapshot.exists()) {
                throw new RuntimeException("Recipe not found");
            }
            
            Object likedByObj = snapshot.get("likedBy");
            long likes = 0;
            if (snapshot.contains("likes")) {
                likes = snapshot.getLong("likes");
            } else if (snapshot.contains("likesCount")) {
                likes = snapshot.getLong("likesCount");
            }
            
            boolean isLiked;
            Map<String, Object> updates = new HashMap<>();
            
            if (likedByObj instanceof List) {
                List<String> likedByList = new ArrayList<>((List<String>) likedByObj);
                isLiked = likedByList.contains(userId);
                if (isLiked) {
                    likedByList.remove(userId);
                    likes--;
                } else {
                    likedByList.add(userId);
                    likes++;
                }
                updates.put("likedBy", likedByList);
            } else {
                // Map format or empty
                Map<String, Boolean> likedByMap;
                if (likedByObj instanceof Map) {
                    likedByMap = new HashMap<>((Map<String, Boolean>) likedByObj);
                } else {
                    likedByMap = new HashMap<>();
                }
                
                isLiked = likedByMap.containsKey(userId);
                if (isLiked) {
                    likedByMap.remove(userId);
                    likes--;
                } else {
                    likedByMap.put(userId, true);
                    likes++;
                }
                updates.put("likedBy", likedByMap);
            }
            
            updates.put("likes", likes);
            updates.put("likesCount", (int)likes);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            
            transaction.update(recipeRef, updates);
            
            return !isLiked; // New status
        }).addOnSuccessListener(callback::onSuccess)
          .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    public void getUserRecipes(String userId, RecipeCallback<List<SocialRecipe>> callback) {
        recipesCollection
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<SocialRecipe> recipes = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    SocialRecipe recipe = doc.toObject(SocialRecipe.class);
                    if (recipe != null) {
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }
                }
                callback.onSuccess(recipes);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    public interface RecipeCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}
