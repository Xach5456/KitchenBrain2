package com.example.kitchenbrain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * EngagementState - SEPARATE UI State Layer
 * 
 * ✅ INSTAGRAM ARCHITECTURE:
 * - Article = immutable server data
 * - EngagementState = local UI state (likes, saves)
 * - FeedItem = projection (Article + EngagementState)
 * 
 * This class manages ALL user interaction state separately from domain models.
 * Benefits:
 * - Server data stays immutable
 * - Easy to diff/compare snapshots
 * - No state corruption on refresh/pagination
 * - Clear separation of concerns
 */
public class EngagementState {

    // ✅ Local engagement state (not from server)
    private final Set<String> likedArticleIds;
    private final Set<String> savedArticleIds;

    public EngagementState() {
        this.likedArticleIds = new HashSet<>();
        this.savedArticleIds = new HashSet<>();
    }

    public EngagementState(EngagementState other) {
        // Copy constructor for immutable updates
        this.likedArticleIds = new HashSet<>(other.likedArticleIds);
        this.savedArticleIds = new HashSet<>(other.savedArticleIds);
    }

    // ===== LIKE STATE =====

    /**
     * Check if article is liked
     */
    public boolean isLiked(String articleId) {
        return likedArticleIds.contains(articleId);
    }

    /**
     * Toggle like state (returns NEW state - immutable pattern)
     */
    public EngagementState toggleLike(String articleId) {
        EngagementState newState = new EngagementState(this);
        
        if (newState.likedArticleIds.contains(articleId)) {
            newState.likedArticleIds.remove(articleId);
        } else {
            newState.likedArticleIds.add(articleId);
        }
        
        return newState;
    }

    /**
     * Get all liked article IDs (immutable)
     */
    public Set<String> getLikedArticleIds() {
        return Collections.unmodifiableSet(likedArticleIds);
    }

    // ===== SAVE STATE =====

    /**
     * Check if article is saved
     */
    public boolean isSaved(String articleId) {
        return savedArticleIds.contains(articleId);
    }

    /**
     * Toggle save state (returns NEW state - immutable pattern)
     */
    public EngagementState toggleSave(String articleId) {
        EngagementState newState = new EngagementState(this);
        
        if (newState.savedArticleIds.contains(articleId)) {
            newState.savedArticleIds.remove(articleId);
        } else {
            newState.savedArticleIds.add(articleId);
        }
        
        return newState;
    }

    /**
     * Get all saved article IDs (immutable)
     */
    public Set<String> getSavedArticleIds() {
        return Collections.unmodifiableSet(savedArticleIds);
    }

    // ===== UTILITY =====

    /**
     * Get engagement count (for debugging)
     */
    public int getEngagementCount() {
        return likedArticleIds.size() + savedArticleIds.size();
    }

    @Override
    public String toString() {
        return "EngagementState{" +
                "likes=" + likedArticleIds.size() +
                ", saves=" + savedArticleIds.size() +
                '}';
    }
}
