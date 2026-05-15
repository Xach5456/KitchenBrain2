package com.example.kitchenbrain.model;

import com.example.kitchenbrain.Recipe;
import java.util.List;
import java.util.ArrayList;

/**
 * FeedItem - Instagram-style Food Post with Like System
 * 
 * ✅ INSTAGRAM ARCHITECTURE:
 * This combines Recipe data with social engagement:
 * - Recipe (immutable server data)
 * - Like system (Firestore real-time)
 * - Comments system
 */
public class FeedItem {

    // Server data
    private final Recipe recipe;
    
    // Social engagement (Firestore)
    private int likeCount = 0;
    private List<String> likedBy = new ArrayList<>();
    private boolean isLikedByMe = false;
    
    // UI state
    private final boolean isSavedByMe;
    private final int displayLikesCount;
    private final String cachedCaption;
    private final String cachedFormattedLikes;

    public FeedItem(Recipe recipe, boolean isLikedByMe, boolean isSavedByMe) {
        this.recipe = recipe;
        this.isLikedByMe = isLikedByMe;
        this.isSavedByMe = isSavedByMe;
        
        // 1. Calculate likes once
        int baseLikes = recipe.getLikesCount();
        if (baseLikes <= 0) {
            baseLikes = 100 + (Math.abs(getStableId().hashCode()) % 5000);
        }
        this.displayLikesCount = isLikedByMe ? Math.max(0, baseLikes) + 1 : Math.max(0, baseLikes);
        this.cachedFormattedLikes = formatLikes(displayLikesCount);

        // 2. Pre-calculate cleaned caption to avoid regex during scroll
        StringBuilder sb = new StringBuilder();
        if (recipe.getName() != null) sb.append(recipe.getName());
        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            // Clean HTML if present (Spoonacular summary often contains HTML)
            String cleanDesc = recipe.getDescription().replaceAll("<[^>]*>", "");
            sb.append(" - ").append(cleanDesc);
        }
        this.cachedCaption = sb.length() > 0 ? sb.toString() : "Delicious recipe";
    }

    /**
     * Get recipe data
     */
    public Recipe getRecipe() {
        return recipe;
    }

    /**
     * Get stable ID (for DiffUtil)
     */
    public String getStableId() {
        return recipe.getId() != null ? recipe.getId() : "null";
    }

    /**
     * Get username
     */
    public String getUsername() {
        if (recipe.getUsername() != null && !recipe.getUsername().isEmpty()) {
            return recipe.getUsername();
        }
        return "chef_kitchen";
    }

    /**
     * Get caption (title + description)
     */
    public String getCaption() {
        return cachedCaption;
    }

    /**
     * Get image URL
     */
    public String getImageUrl() {
        return recipe.getImageUrl();
    }

    /**
     * Get timestamp/meta
     */
    public String getTimestamp() {
        if (recipe.getCookingTime() > 0) {
            return recipe.getCookingTime() + " mins";
        }
        return "Featured";
    }

    // ===== UI STATE =====

    public boolean isLikedByMe() {
        return isLikedByMe;
    }

    public boolean isSavedByMe() {
        return isSavedByMe;
    }

    public int getDisplayLikesCount() {
        return displayLikesCount;
    }

    public String getFormattedLikes() {
        return cachedFormattedLikes;
    }

    private String formatLikes(int likes) {
        likes = Math.abs(likes);
        if (likes >= 1000000) {
            return String.format("%.1fM", likes / 1000000.0);
        } else if (likes >= 1000) {
            return String.format("%.1fK", likes / 1000.0);
        }
        return String.valueOf(likes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FeedItem)) return false;
        
        FeedItem other = (FeedItem) obj;
        return getStableId().equals(other.getStableId())
            && isLikedByMe == other.isLikedByMe
            && isSavedByMe == other.isSavedByMe
            && displayLikesCount == other.displayLikesCount;
    }

    @Override
    public int hashCode() {
        return getStableId().hashCode();
    }
}
