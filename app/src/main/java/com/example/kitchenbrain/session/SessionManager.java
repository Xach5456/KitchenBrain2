package com.example.kitchenbrain.session;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 🎯 SESSION MANAGER - Single Source of Truth for Authentication State
 * 
 * This is the ONLY place that manages:
 * - Current user ID
 * - Authentication state
 * - Session lifecycle
 * 
 * Architecture:
 * Firebase Auth → SessionManager (reactive) → SocialGraphState → UI
 * 
 * Benefits:
 * - Reactive (LiveData/Flow compatible)
 * - Decouples domain from Firebase
 * - Handles auth state changes
 * - No race conditions
 * 
 * Future: Can be replaced with Kotlin Flow for more reactive power
 */
public class SessionManager {
    
    private static final String TAG = "SessionManager";
    private static volatile SessionManager instance;
    
    private final FirebaseAuth auth;
    private final MutableLiveData<String> currentUserIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticatedLiveData = new MutableLiveData<>();
    
    private FirebaseAuth.AuthStateListener authStateListener;
    
    private SessionManager() {
        this.auth = FirebaseAuth.getInstance();
        setupAuthListener();
    }
    
    /**
     * Setup reactive auth state listener
     * This ensures we ALWAYS have the latest auth state
     */
    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            String userId = user != null ? user.getUid() : null;
            boolean authenticated = user != null;
            
            Log.d(TAG, "🔔 [SESSION] Auth state changed: userId=" + userId + ", authenticated=" + authenticated);
            
            currentUserIdLiveData.postValue(userId);
            isAuthenticatedLiveData.postValue(authenticated);
        };
        
        // Attach listener immediately
        auth.addAuthStateListener(authStateListener);
        
        // Trigger initial state
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserIdLiveData.setValue(currentUser.getUid());
            isAuthenticatedLiveData.setValue(true);
            Log.d(TAG, "✅ [SESSION] Initial auth state: " + currentUser.getUid());
        } else {
            currentUserIdLiveData.setValue(null);
            isAuthenticatedLiveData.setValue(false);
            Log.w(TAG, "⚠️ [SESSION] No user logged in initially");
        }
    }
    
    public static SessionManager get() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get current user ID (synchronous - may be null if not authenticated)
     * 
     * For reactive usage, observe getCurrentUserIdLiveData() instead
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Get current user ID (synchronous - throws if not authenticated)
     * 
     * @throws IllegalStateException if user is not logged in
     */
    public String requireUserId() {
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ [SESSION] User not authenticated");
            throw new IllegalStateException(
                "SessionManager: User not authenticated. " +
                "Call FirebaseAuth.getInstance().signInWithEmailAndPassword() first."
            );
        }
        return userId;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return auth.getCurrentUser() != null;
    }
    
    /**
     * Reactive: Observe current user ID changes
     * 
     * Usage:
     * SessionManager.get().getCurrentUserIdLiveData().observe(lifecycleOwner, userId -> {
     *     // Handle user ID changes
     * });
     */
    public LiveData<String> getCurrentUserIdLiveData() {
        return currentUserIdLiveData;
    }
    
    /**
     * Reactive: Observe authentication state changes
     * 
     * Usage:
     * SessionManager.get().isAuthenticatedLiveData().observe(lifecycleOwner, isAuthenticated -> {
     *     // Handle auth state changes
     * });
     */
    public LiveData<Boolean> isAuthenticatedLiveData() {
        return isAuthenticatedLiveData;
    }
    
    /**
     * Cleanup auth listener (call in Application.onTerminate() or when appropriate)
     */
    public void cleanup() {
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
            Log.d(TAG, "🔄 [SESSION] Auth state listener removed");
        }
    }
    
    /**
     * Reset instance (useful for testing)
     */
    public static void reset() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
            Log.d(TAG, "🔄 [SESSION] SessionManager reset");
        }
    }
}
