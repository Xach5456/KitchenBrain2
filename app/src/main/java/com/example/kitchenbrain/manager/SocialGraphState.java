package com.example.kitchenbrain.manager;

import android.util.Log;

import com.example.kitchenbrain.session.SessionManager;

import java.util.HashSet;
import java.util.Set;

/**
 * ✅ DOMAIN LAYER - Social Graph State (Source of Truth)
 * 
 * This is the ONLY place that manages:
 * - Who follows whom
 * - Graph relationships  
 * - Domain validation rules
 * 
 * ⚠️ NO events, NO listeners, NO callbacks
 * Pure state holder with validation.
 * 
 * Instagram Pattern:
 * Repository calls domain → mutates state → explicitly rebuilds snapshot
 * 
 * ✅ PRODUCTION ARCHITECTURE:
 * - Depends on SessionManager (not Firebase directly)
 * - Decoupled from infrastructure
 * - Pure domain logic
 */
public class SocialGraphState {
    private static final String TAG = "SocialGraphState";
    private static SocialGraphState instance;
    
    // ✅ DOMAIN STATE - Source of truth for relationships
    private final Set<String> followingSet = new HashSet<>();
    private final Set<String> followersSet = new HashSet<>();
    
    // SessionManager provides current user ID (decoupled from Firebase)
    private final SessionManager sessionManager;
    
    private SocialGraphState() {
        // ✅ Dependency Injection: Get SessionManager
        this.sessionManager = SessionManager.get();
        Log.d(TAG, "✅ [DOMAIN] SocialGraphState created with SessionManager");
    }
    
    /**
     * ✅ LAZY SAFE ACCESSOR - Gets currentUserId from SessionManager
     * 
     * This is the PRODUCTION PATTERN:
     * - No manual initialize() needed
     * - Decoupled from Firebase (uses SessionManager)
     * - Fails fast with clear error message
     */
    private String requireUserId() {
        // ✅ Get from SessionManager (reactive, always up-to-date)
        String userId = sessionManager.getCurrentUserId();
        
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ [DOMAIN] User not authenticated - cannot perform follow operation");
            Log.e(TAG, "❌ [DOMAIN] FIX: User must be logged in via Firebase Auth");
            throw new IllegalStateException(
                "SocialGraphState: User not authenticated. " +
                "SessionManager.getCurrentUserId() returned null."
            );
        }
        
        return userId;
    }
    
    public static SocialGraphState getInstance() {
        if (instance == null) {
            synchronized (SocialGraphState.class) {
                if (instance == null) {
                    instance = new SocialGraphState();
                }
            }
        }
        return instance;
    }
    
    /**
     * ✅ Initialize graph for a user (BACKWARD COMPATIBILITY)
     * @deprecated SessionManager provides user ID automatically. This is kept for backward compatibility.
     */
    @Deprecated
    public void initialize(String userId) {
        // This is no longer needed - SessionManager handles it
        // But kept for backward compatibility
        Log.d(TAG, "⚠️ [DOMAIN] Manual initialize() called - no longer needed, SessionManager handles it");
        followingSet.clear();
        followersSet.clear();
    }
    
    /**
     * ✅ INTENT: Follow a user
     * 
     * This is a DOMAIN OPERATION, not UI operation.
     * Validates rules, updates state, returns result.
     * 
     * ✅ PRODUCTION: Uses requireUserId() for auto-initialization
     */
    public boolean followUser(String targetUserId) {
        // ✅ Auto-get currentUserId (initializes if needed)
        String userId = requireUserId();
        
        Log.d(TAG, "🔍 [DOMAIN] followUser called: userId=" + userId + ", targetUserId=" + targetUserId);
        
        if (targetUserId == null || targetUserId.isEmpty()) {
            Log.e(TAG, "❌ [DOMAIN] Cannot follow - targetUserId is null or empty");
            return false;
        }
        
        if (targetUserId.equals(userId)) {
            Log.e(TAG, "❌ [DOMAIN] Cannot follow self: " + userId);
            return false;
        }
        
        if (followingSet.contains(targetUserId)) {
            Log.w(TAG, "⚠️ [DOMAIN] Already following: " + targetUserId);
            return false;
        }
        
        // ✅ Update domain state
        followingSet.add(targetUserId);
        
        Log.d(TAG, "✅ [DOMAIN] Followed user: " + targetUserId + " (followingSet size: " + followingSet.size() + ")");
        return true;
    }
    
    /**
     * ✅ INTENT: Unfollow a user
     */
    public boolean unfollowUser(String targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        
        if (!followingSet.contains(targetUserId)) {
            Log.w(TAG, "⚠️ [DOMAIN] Not following: " + targetUserId);
            return false;
        }
        
        followingSet.remove(targetUserId);
        Log.d(TAG, "✅ [DOMAIN] Unfollowed user: " + targetUserId);
        return true;
    }
    
    /**
     * ✅ INTENT: Someone followed you
     * 
     * Called when you receive a notification that user X followed you
     */
    public void onNewFollower(String followerId) {
        if (followerId != null) {
            followersSet.add(followerId);
            Log.d(TAG, "✅ [DOMAIN] New follower: " + followerId);
        }
    }
    
    /**
     * ✅ INTENT: Someone unfollowed you
     */
    public void onFollowerRemoved(String followerId) {
        if (followerId != null) {
            followersSet.remove(followerId);
            Log.d(TAG, "✅ [DOMAIN] Follower removed: " + followerId);
        }
    }
    
    /**
     * ✅ SYNC: Reconcile with server state
     * 
     * Called by Firestore listener to sync local domain state with server
     */
    public void reconcileWithServer(Set<String> serverFollowing, Set<String> serverFollowers) {
        boolean changed = false;
        
        if (!followingSet.equals(serverFollowing)) {
            followingSet.clear();
            followingSet.addAll(serverFollowing);
            changed = true;
            Log.d(TAG, "🔄 [DOMAIN] Following reconciled with server: " + serverFollowing.size() + " users");
        }
        
        if (!followersSet.equals(serverFollowers)) {
            followersSet.clear();
            followersSet.addAll(serverFollowers);
            changed = true;
            Log.d(TAG, "🔄 [DOMAIN] Followers reconciled with server: " + serverFollowers.size() + " users");
        }
        
        if (!changed) {
            Log.d(TAG, "✅ [DOMAIN] State matches server - no reconciliation needed");
        }
    }
    
    /**
     * ✅ QUERIES - Domain questions
     */
    public boolean isFollowing(String userId) {
        return followingSet.contains(userId);
    }
    
    public boolean isFollower(String userId) {
        return followersSet.contains(userId);
    }
    
    public boolean isMutual(String userId) {
        return followingSet.contains(userId) && followersSet.contains(userId);
    }
    
    /**
     * ✅ GETTERS - For snapshot derivation
     */
    public Set<String> getFollowingSet() {
        return new HashSet<>(followingSet); // Return copy to prevent mutation
    }
    
    public Set<String> getFollowersSet() {
        return new HashSet<>(followersSet); // Return copy to prevent mutation
    }
    
    /**
     * ✅ Get current user ID from SessionManager
     */
    public String getCurrentUserId() {
        return sessionManager.getCurrentUserId();
    }
    
    /**
     * ✅ RESET - Clear all state (logout)
     */
    public void reset() {
        // Note: SessionManager handles auth state, we just clear graph data
        followingSet.clear();
        followersSet.clear();
        Log.d(TAG, "🗑️ [DOMAIN] Graph state reset (SessionManager still active)");
    }
    
    /**
     * ✅ DEBUG INFO
     */
    public String getStateInfo() {
        return "Following: " + followingSet.size() + 
               ", Followers: " + followersSet.size();
    }
}
