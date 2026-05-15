package com.example.kitchenbrain.navigation;

import android.util.Log;

import androidx.fragment.app.FragmentActivity;

/**
 * 🎯 NAVIGATION PROVIDER - Dependency Injection Container
 * 
 * This is a simple DI container that provides ChatNavigator instances.
 * 
 * Architecture:
 * UI → ChatNavigatorProvider.get() → ChatNavigator (interface)
 * 
 * Benefits:
 * - Single point of configuration
 * - Easy to swap implementations (e.g., for testing)
 * - Lazy initialization
 * - Thread-safe singleton
 * 
 * Future: Can be replaced with Hilt/Dagger/Koin
 */
public class ChatNavigatorProvider {
    
    private static final String TAG = "ChatNavigatorProvider";
    private static volatile ChatNavigatorProvider instance;
    
    private ChatNavigator navigator;
    
    private ChatNavigatorProvider() {}
    
    public static ChatNavigatorProvider get() {
        if (instance == null) {
            synchronized (ChatNavigatorProvider.class) {
                if (instance == null) {
                    instance = new ChatNavigatorProvider();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the navigator with Android implementation
     * Call this ONCE in MainActivity.onCreate()
     */
    public void initialize(FragmentActivity activity) {
        Log.d(TAG, "🚀 [PROVIDER] Initializing ChatNavigator with Android implementation");
        this.navigator = new ChatNavigatorImpl(activity);
        Log.d(TAG, "✅ [PROVIDER] ChatNavigator initialized successfully");
    }
    
    /**
     * Get the ChatNavigator instance
     * 
     * @throws IllegalStateException if not initialized
     */
    public ChatNavigator getNavigator() {
        if (navigator == null) {
            throw new IllegalStateException(
                "ChatNavigator not initialized! Call ChatNavigatorProvider.get().initialize(activity) first."
            );
        }
        return navigator;
    }
    
    /**
     * Check if navigator is initialized
     */
    public boolean isInitialized() {
        return navigator != null;
    }
    
    /**
     * Reset navigator (useful for testing)
     */
    public void reset() {
        navigator = null;
        Log.d(TAG, "🔄 [PROVIDER] ChatNavigator reset");
    }
}
