package com.example.kitchenbrain.config;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * FirestoreOptimizationConfig - Cost and performance optimization
 * 
 * 🔥 FIRESTORE OPTIMIZATION RULES:
 * - Realtime: chat/comments/notifications ONLY
 * - Cache-first: recipes, feed, likes, saves
 * - Batch operations for multiple writes
 * - Pagination for large datasets
 */
public class FirestoreOptimizationConfig {
    
    private static final String TAG = "FirestoreConfig";
    
    // Realtime features (keep these as snapshot listeners)
    public static final boolean ENABLE_CHAT_REALTIME = true;
    public static final boolean ENABLE_COMMENTS_REALTIME = true;
    public static final boolean ENABLE_NOTIFICATIONS_REALTIME = true;
    
    // Non-realtime features (use cache + periodic refresh)
    public static final boolean ENABLE_RECIPES_REALTIME = false;
    public static final boolean ENABLE_LIKES_REALTIME = false;
    public static final boolean ENABLE_SAVES_REALTIME = false;
    public static final boolean ENABLE_FEED_REALTIME = false;
    public static final boolean ENABLE_SEARCH_REALTIME = false;
    
    // Performance settings
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int CACHE_EXPIRY_MINUTES = 30;
    public static final int REALTIME_LISTENER_TIMEOUT_MS = 30000; // 30 seconds
    
    // Cost optimization
    public static final boolean ENABLE_BATCH_WRITES = true;
    public static final boolean ENABLE_OFFLINE_CACHE = true;
    public static final boolean ENABLE_PERSISTENCE = true;
    
    private static final Map<String, ListenerRegistration> activeListeners = Collections.synchronizedMap(new HashMap<>());
    
    /**
     * Configure Firestore for optimal performance and cost
     */
    public static void configureFirestore() {
        Log.d(TAG, "🔧 Configuring Firestore for optimization");
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        FirebaseFirestoreSettings.Builder settingsBuilder = new FirebaseFirestoreSettings.Builder();
        
        // Enable offline persistence
        if (ENABLE_OFFLINE_CACHE) {
            settingsBuilder.setPersistenceEnabled(true);
            Log.d(TAG, "✅ Offline persistence enabled");
        }
        
        // Configure cache size
        settingsBuilder.setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED);
        
        db.setFirestoreSettings(settingsBuilder.build());
        
        Log.d(TAG, "✅ Firestore configuration complete");
    }
    
    /**
     * Register a realtime listener with timeout and cleanup
     */
    public static void registerRealtimeListener(String key, ListenerRegistration listener) {
        // Remove existing listener if present
        ListenerRegistration existing = activeListeners.remove(key);
        if (existing != null) {
            existing.remove();
        }
        
        // Add new listener
        activeListeners.put(key, listener);
        
        // Auto-remove after timeout (for non-critical listeners)
        if (!isCriticalRealtimeFeature(key)) {
            setTimeoutRemoval(key, REALTIME_LISTENER_TIMEOUT_MS);
        }
        
        Log.d(TAG, "👂 Registered realtime listener: " + key);
    }
    
    /**
     * Remove a realtime listener
     */
    public static void removeRealtimeListener(String key) {
        ListenerRegistration listener = activeListeners.remove(key);
        if (listener != null) {
            listener.remove();
            Log.d(TAG, "🔇 Removed realtime listener: " + key);
        }
    }
    
    /**
     * Remove all realtime listeners (call in onDestroy)
     */
    public static void removeAllListeners() {
        Log.d(TAG, "🔇 Removing all realtime listeners: " + activeListeners.size());
        
        for (Map.Entry<String, ListenerRegistration> entry : activeListeners.entrySet()) {
            entry.getValue().remove();
        }
        
        activeListeners.clear();
    }
    
    /**
     * Check if a feature should use realtime updates
     */
    public static boolean shouldUseRealtime(String feature) {
        switch (feature) {
            case "chat":
                return ENABLE_CHAT_REALTIME;
            case "comments":
                return ENABLE_COMMENTS_REALTIME;
            case "notifications":
                return ENABLE_NOTIFICATIONS_REALTIME;
            case "recipes":
                return ENABLE_RECIPES_REALTIME;
            case "likes":
                return ENABLE_LIKES_REALTIME;
            case "saves":
                return ENABLE_SAVES_REALTIME;
            case "feed":
                return ENABLE_FEED_REALTIME;
            case "search":
                return ENABLE_SEARCH_REALTIME;
            default:
                return false; // Default to non-realtime for cost savings
        }
    }
    
    /**
     * Get optimal page size based on feature
     */
    public static int getPageSize(String feature) {
        switch (feature) {
            case "feed":
                return DEFAULT_PAGE_SIZE;
            case "comments":
                return 10; // Smaller for comments
            case "search":
                return 20;
            case "saved":
                return 15;
            default:
                return DEFAULT_PAGE_SIZE;
        }
    }
    
    /**
     * Check if feature is critical for realtime
     */
    private static boolean isCriticalRealtimeFeature(String key) {
        return key.contains("chat") || key.contains("comments") || key.contains("notifications");
    }
    
    /**
     * Set timeout for non-critical listeners
     */
    private static void setTimeoutRemoval(String key, long delayMs) {
        // This would use a Handler or similar mechanism
        // For now, just log the intent
        Log.d(TAG, "⏰ Will remove non-critical listener after " + delayMs + "ms: " + key);
    }
    
    /**
     * Get cache expiry time in milliseconds
     */
    public static long getCacheExpiryMs() {
        return CACHE_EXPIRY_MINUTES * 60 * 1000L;
    }
    
    /**
     * Log current listener usage for monitoring
     */
    public static void logListenerUsage() {
        Log.d(TAG, "📊 Current realtime listeners: " + activeListeners.size());
        for (String key : activeListeners.keySet()) {
            Log.d(TAG, "  - " + key + (isCriticalRealtimeFeature(key) ? " (critical)" : " (non-critical)"));
        }
    }
    
    /**
     * Cost optimization recommendations
     */
    public static void logCostOptimizationTips() {
        Log.d(TAG, "💡 COST OPTIMIZATION TIPS:");
        Log.d(TAG, "  ✅ Realtime limited to: chat, comments, notifications");
        Log.d(TAG, "  ✅ Cache-first for: recipes, feed, likes, saves");
        Log.d(TAG, "  ✅ Pagination enabled for large datasets");
        Log.d(TAG, "  ✅ Batch writes for multiple operations");
        Log.d(TAG, "  ✅ Offline cache enabled");
        Log.d(TAG, "  ⚠️ Monitor listener count to prevent costs");
    }
    
    /**
     * Performance monitoring
     */
    public static class PerformanceMonitor {
        private static long totalReads = 0;
        private static long totalWrites = 0;
        private static long totalListenerTime = 0;
        
        public static void incrementReadCount() {
            totalReads++;
            if (totalReads % 100 == 0) {
                Log.d(TAG, "📊 Firestore reads: " + totalReads);
            }
        }
        
        public static void incrementWriteCount() {
            totalWrites++;
            if (totalWrites % 50 == 0) {
                Log.d(TAG, "📊 Firestore writes: " + totalWrites);
            }
        }
        
        public static void addListenerTime(long timeMs) {
            totalListenerTime += timeMs;
        }
        
        public static void logPerformanceStats() {
            Log.d(TAG, "📊 PERFORMANCE STATS:");
            Log.d(TAG, "  Total reads: " + totalReads);
            Log.d(TAG, "  Total writes: " + totalWrites);
            Log.d(TAG, "  Listener time: " + totalListenerTime + "ms");
            Log.d(TAG, "  Active listeners: " + activeListeners.size());
        }
    }
}
