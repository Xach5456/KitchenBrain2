package com.example.kitchenbrain.util;

import android.text.TextUtils;

import com.example.kitchenbrain.Recipe;
import com.example.kitchenbrain.model.SocialRecipe;

public final class RecipeOwnershipUtils {
    private RecipeOwnershipUtils() {}

    public static boolean isOwner(Object recipe, String currentUserId) {
        String ownerId = getOwnerId(recipe);
        return !TextUtils.isEmpty(currentUserId)
                && !TextUtils.isEmpty(ownerId)
                && currentUserId.equals(ownerId);
    }

    public static String getOwnerId(Object recipe) {
        if (recipe instanceof Recipe) {
            return ((Recipe) recipe).getAuthorId();
        }
        if (recipe instanceof com.example.kitchenbrain.model.Recipe) {
            com.example.kitchenbrain.model.Recipe modelRecipe =
                    (com.example.kitchenbrain.model.Recipe) recipe;
            return !TextUtils.isEmpty(modelRecipe.getAuthorId())
                    ? modelRecipe.getAuthorId()
                    : modelRecipe.getCreatedBy();
        }
        if (recipe instanceof SocialRecipe) {
            return ((SocialRecipe) recipe).getAuthorId();
        }
        return null;
    }
}
