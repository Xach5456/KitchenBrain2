package com.example.kitchenbrain;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Data Sanitizer
 * Ensures all Recipe objects have valid, non-null fields
 * Prevents NullPointerException crashes throughout the app
 */
public class RecipeSanitizer {
    
    private static final String TAG = "RecipeSanitizer";
    private static final String DEFAULT_IMAGE_URL = "";
    private static final String DEFAULT_NAME = "Untitled Recipe";
    private static final String DEFAULT_DESCRIPTION = "No description available";
    private static final String DEFAULT_USERNAME = "Unknown Chef";
    private static final String DEFAULT_AUTHOR_ID = "";
    private static final String DEFAULT_COOKING_INSTRUCTIONS = "";
    private static final int DEFAULT_COOKING_TIME = 0;
    private static final int DEFAULT_LIKES_COUNT = 0;

    /**
     * Sanitize a single recipe - ensure no null fields
     * CRITICAL: Logs ERROR for missing critical data to expose Firebase issues
     */
    public static Recipe sanitize(Recipe recipe) {
        if (recipe == null) {
            Log.w(TAG, "Received null recipe - creating empty recipe");
            return createEmptyRecipe();
        }

        // Fix null or empty image URL
        if (recipe.getImageUrl() == null || recipe.getImageUrl().trim().isEmpty()) {
            recipe.setImageUrl(DEFAULT_IMAGE_URL);
            Log.w(TAG, "⚠️ Missing imageUrl for recipe: " + recipe.getId() + " - UI will show placeholder");
        }

        // Fix null name - CRITICAL: This is a data quality issue!
        if (recipe.getName() == null || recipe.getName().trim().isEmpty()) {
            recipe.setName(DEFAULT_NAME);
            Log.e(TAG, "🔴 CRITICAL: Missing recipe name for ID: " + recipe.getId() + " - Firebase data may be corrupted");
        }

        // Fix null description
        if (recipe.getDescription() == null || recipe.getDescription().trim().isEmpty()) {
            recipe.setDescription(DEFAULT_DESCRIPTION);
            Log.w(TAG, "⚠️ Missing description for recipe: " + recipe.getId());
        }

        // Fix null username
        if (recipe.getUsername() == null || recipe.getUsername().trim().isEmpty()) {
            recipe.setUsername(DEFAULT_USERNAME);
            Log.w(TAG, "⚠️ Missing username for recipe: " + recipe.getId() + " (authorId: " + recipe.getAuthorId() + ")");
        }

        // Fix null authorId - CRITICAL for security/permissions
        if (recipe.getAuthorId() == null || recipe.getAuthorId().trim().isEmpty()) {
            recipe.setAuthorId(DEFAULT_AUTHOR_ID);
            Log.e(TAG, "🔴 CRITICAL: Missing authorId for recipe: " + recipe.getId() + " - permission checks may fail");
        }

        // Fix null cooking instructions
        if (recipe.getCookingInstructions() == null) {
            recipe.setCookingInstructions(DEFAULT_COOKING_INSTRUCTIONS);
            Log.w(TAG, "⚠️ Missing cooking instructions for recipe: " + recipe.getId());
        }

        // Fix null ingredients list
        if (recipe.getIngredients() == null) {
            recipe.setIngredients(new ArrayList<>());
            Log.w(TAG, "⚠️ Missing ingredients list for recipe: " + recipe.getId());
        }

        // Fix negative cooking time
        if (recipe.getCookingTime() < 0) {
            recipe.setCookingTime(DEFAULT_COOKING_TIME);
            Log.w(TAG, "⚠️ Invalid cooking time (" + recipe.getCookingTime() + ") for recipe: " + recipe.getId());
        }

        // Fix negative likes count
        if (recipe.getLikesCount() < 0) {
            recipe.setLikesCount(DEFAULT_LIKES_COUNT);
            Log.w(TAG, "⚠️ Invalid likes count (" + recipe.getLikesCount() + ") for recipe: " + recipe.getId());
        }

        return recipe;
    }

    /**
     * Sanitize a list of recipes
     */
    public static List<Recipe> sanitizeList(List<Recipe> recipes) {
        if (recipes == null) {
            Log.w(TAG, "Received null recipe list - returning empty list");
            return new ArrayList<>();
        }

        List<Recipe> sanitized = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe != null) {
                sanitized.add(sanitize(recipe));
            } else {
                Log.w(TAG, "Skipping null recipe in list");
            }
        }

        Log.d(TAG, "Sanitized " + sanitized.size() + " recipes from " + recipes.size() + " total");
        return sanitized;
    }

    /**
     * Create an empty recipe with all fields initialized
     */
    private static Recipe createEmptyRecipe() {
        Recipe recipe = new Recipe();
        recipe.setImageUrl(DEFAULT_IMAGE_URL);
        recipe.setName(DEFAULT_NAME);
        recipe.setDescription(DEFAULT_DESCRIPTION);
        recipe.setUsername(DEFAULT_USERNAME);
        recipe.setAuthorId(DEFAULT_AUTHOR_ID);
        recipe.setCookingInstructions(DEFAULT_COOKING_INSTRUCTIONS);
        recipe.setIngredients(new ArrayList<>());
        recipe.setCookingTime(DEFAULT_COOKING_TIME);
        recipe.setLikesCount(DEFAULT_LIKES_COUNT);
        return recipe;
    }

    /**
     * Validate if a recipe is safe to display
     */
    public static boolean isValid(Recipe recipe) {
        if (recipe == null) return false;
        if (recipe.getName() == null || recipe.getName().trim().isEmpty()) return false;
        return true;
    }
}
