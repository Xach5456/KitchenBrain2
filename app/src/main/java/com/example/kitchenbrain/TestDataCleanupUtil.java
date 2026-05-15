/**
 * Utility class to clean up test/mock data from Firestore
 * 
 * INSTRUCTIONS FOR USE:
 * 1. Run this from an Android device/emulator with Firebase connected
 * 2. Or execute these operations manually in Firebase Console
 * 3. Or create a one-time admin script on Firebase Cloud Functions
 * 
 * This class provides methods to:
 * - Delete specific test recipes by name
 * - Validate recipe ownership
 * - Clean up orphaned recipes
 */

package com.example.kitchenbrain;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TestDataCleanupUtil {
    
    private static final String TAG = "TestDataCleanup";
    
    // Test recipe names to delete
    private static final List<String> TEST_RECIPE_NAMES = Arrays.asList(
        "esim inch",
        "Test Recipe"
    );
    
    /**
     * Delete test recipes from Firestore
     * Call this once to clean up test data
     */
    public static void deleteTestRecipes(Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Log.d(TAG, "Starting test recipe cleanup...");
        
        db.collection("recipes")
            .whereIn("name", TEST_RECIPE_NAMES)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int[] deletedCount = {0}; // Use array to allow modification in lambda
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String recipeName = document.getString("name");
                    String recipeId = document.getId();
                    
                    Log.d(TAG, "Found test recipe: " + recipeName + " (ID: " + recipeId + ")");
                    
                    db.collection("recipes").document(recipeId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            deletedCount[0]++;
                            Log.d(TAG, "✓ Deleted test recipe: " + recipeName);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "✗ Failed to delete: " + recipeName, e);
                        });
                }
                
                Log.d(TAG, "Cleanup complete. Deleted " + deletedCount[0] + " test recipes.");
                if (onComplete != null) {
                    onComplete.run();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding test recipes", e);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
    }
    
    /**
     * Validate and fix recipe ownership
     * Ensures all recipes have proper authorId matching their creator
     */
    public static void validateRecipeOwnership(String currentUserId, Runnable onComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        if (currentUserId == null) {
            Log.e(TAG, "Cannot validate - user not authenticated");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        Log.d(TAG, "Validating recipe ownership for user: " + currentUserId);
        
        db.collection("recipes")
            .whereEqualTo("authorId", currentUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int[] fixedCount = {0}; // Use array to allow modification in lambda
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Recipe recipe = document.toObject(Recipe.class);
                    
                    if (recipe == null || recipe.getAuthorId() == null) {
                        Log.w(TAG, "Recipe " + document.getId() + " has no authorId - skipping");
                        continue;
                    }
                    
                    // Check if authorId matches current user
                    if (!currentUserId.equals(recipe.getAuthorId())) {
                        Log.w(TAG, "Recipe " + document.getId() + " has mismatched authorId: " + recipe.getAuthorId());
                        
                        // Fix the authorId
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("authorId", currentUserId);
                        
                        db.collection("recipes").document(document.getId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                fixedCount[0]++;
                                Log.d(TAG, "✓ Fixed authorId for recipe: " + recipe.getName());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "✗ Failed to fix authorId for: " + recipe.getName(), e);
                            });
                    }
                }
                
                Log.d(TAG, "Validation complete. Fixed " + fixedCount[0] + " recipes.");
                if (onComplete != null) onComplete.run();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error validating recipe ownership", e);
                if (onComplete != null) onComplete.run();
            });
    }
    
    /**
     * Add production flag to prevent test data in production
     */
    public static boolean isProductionBuild() {
        // This should be configured in your build.gradle
        // For now, we'll use BuildConfig.DEBUG which is auto-generated
        try {
            Class<?> buildConfig = Class.forName("com.example.kitchenbrain.BuildConfig");
            Boolean isDebug = (Boolean) buildConfig.getField("DEBUG").get(null);
            return !isDebug;
        } catch (Exception e) {
            Log.e(TAG, "Error accessing BuildConfig", e);
            return true; // Assume production if we can't determine
        }
    }
}
