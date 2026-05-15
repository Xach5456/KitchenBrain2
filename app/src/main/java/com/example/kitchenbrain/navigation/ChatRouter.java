package com.example.kitchenbrain.navigation;

import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.example.kitchenbrain.ChatFragment;
import com.example.kitchenbrain.R;

/**
 * 🎯 CHAT ROUTER - Convenience Facade
 * 
 * This is a STATIC FACADE that delegates to ChatNavigator.
 * It provides a simple API for components that don't want DI.
 * 
 * Architecture:
 * UI → ChatRouter.open() → ChatNavigatorProvider → ChatNavigator → FragmentManager
 * 
 * For FULL DI, use ChatNavigator directly:
 * ChatNavigatorProvider.get().getNavigator().openChat(userId, username);
 * 
 * For SIMPLE usage, use ChatRouter:
 * ChatRouter.open(activity, userId, username);
 */
public class ChatRouter {
    
    private static final String TAG = "ChatRouter";
    
    /**
     * Open chat with another user (simple static API)
     * 
     * @param activity Current FragmentActivity
     * @param otherUserId User ID to chat with
     * @param username Username for logging
     */
    public static void open(FragmentActivity activity, String otherUserId, String username) {
        Log.d(TAG, "🎯 [CHAT_ROUTER] Static facade called");
        
        // Delegate to ChatNavigator (true DI)
        try {
            ChatNavigator navigator = ChatNavigatorProvider.get().getNavigator();
            navigator.openChat(otherUserId, username);
            Log.d(TAG, "✅ [CHAT_ROUTER] Delegated to ChatNavigator successfully");
        } catch (IllegalStateException e) {
            // Fallback: Create temporary navigator if not initialized
            Log.w(TAG, "⚠️ [CHAT_ROUTER] ChatNavigator not initialized, creating temporary instance");
            ChatNavigator tempNavigator = new ChatNavigatorImpl(activity);
            tempNavigator.openChat(otherUserId, username);
        }
    }
}
