package com.example.kitchenbrain.session;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.example.kitchenbrain.persistence.ChatSessionStore;
import com.example.kitchenbrain.state.ChatStateManager;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔥 CHAT SESSION CONTROLLER - Telegram-style Architecture
 * Features: Single source of truth + state ownership + lifecycle independence
 */
public class ChatSessionController {
    
    private static final String TAG = "ChatSessionController";
    
    // Singleton instance for global chat session management
    private static volatile ChatSessionController instance;
    private static final Object LOCK = new Object();
    
    // Active chat sessions (supports multiple chats)
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();
    
    // 🔥 TELEGRAM-GRADE: Persistence layer integration
    private ChatSessionStore sessionStore;
    private Context applicationContext;
    
    // Session listener interface
    public interface SessionListener {
        void onSessionCreated(String chatId, ChatSession session);
        void onSessionUpdated(String chatId, ChatSession session);
        void onSessionDestroyed(String chatId);
        void onSessionError(String chatId, Exception error);
    }
    
    private SessionListener sessionListener;
    
    /**
     * 🔥 Get singleton instance with context (for persistence)
     * ⚠️ WARNING: Storing context in singleton can cause memory leaks
     * Use application context instead
     */
    public static ChatSessionController getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    // Use application context to avoid memory leaks
                    instance = new ChatSessionController(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * 🔥 Get singleton instance (legacy, will initialize persistence later)
     */
    public static ChatSessionController getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ChatSessionController(null);
                }
            }
        }
        return instance;
    }
    
    /**
     * 🎯 CREATE OR GET CHAT SESSION - Single source of truth
     */
    public ChatSession createOrGetSession(Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "❌ Cannot create session: bundle is null");
            return null;
        }
        
        String otherUserId = bundle.getString("other_user_id");
        String currentUserId = getCurrentUserId();
        
        if (otherUserId == null || currentUserId == null) {
            Log.e(TAG, "❌ Cannot create session: missing user IDs");
            return null;
        }
        
        // 🔥 Generate deterministic chatId (single source of truth)
        String chatId = ChatIdProvider.getChatId(currentUserId, otherUserId);
        
        ChatSession existingSession = activeSessions.get(chatId);
        if (existingSession != null) {
            existingSession.updateFromBundle(bundle);
            return existingSession;
        }
        
        // Create new session
        ChatSession newSession = new ChatSession(chatId, currentUserId, otherUserId);
        newSession.initFromBundle(bundle);
        
        // Store in active sessions
        activeSessions.put(chatId, newSession);
        
        Log.d(TAG, "✅ Created new session: " + chatId);
        
        // Notify listener
        if (sessionListener != null) {
            sessionListener.onSessionCreated(chatId, newSession);
        }
        
        return newSession;
    }
    
    /**
     * 📱 GET EXISTING SESSION
     */
    public ChatSession getSession(String chatId) {
        if (chatId == null) {
            return null;
        }
        return activeSessions.get(chatId);
    }
    
    /**
     *  TELEGRAM-STYLE: Create minimal fallback session
     */
    public ChatSession createMinimalSession(String chatId, String currentUserId, String otherUserId) {
        if (chatId == null || currentUserId == null || otherUserId == null) {
            return null;
        }
        
        ChatSession minimalSession = activeSessions.get(chatId);
        if (minimalSession == null) {
            minimalSession = new ChatSession(chatId, currentUserId, otherUserId);
            activeSessions.put(chatId, minimalSession);
            Log.d(TAG, "✅ TELEGRAM-STYLE: Minimal session created: " + chatId);
        }
        return minimalSession;
    }
    
    /**
     * 🗑️ DESTROY SESSION
     */
    public void destroySession(String chatId) {
        if (chatId == null) return;
        ChatSession removed = activeSessions.remove(chatId);
        if (removed != null) {
            removed.cleanup();
            if (sessionListener != null) {
                sessionListener.onSessionDestroyed(chatId);
            }
        }
    }
    
    /**
     * 🧹 CLEANUP ALL SESSIONS
     */
    public void cleanupAllSessions() {
        for (Map.Entry<String, ChatSession> entry : activeSessions.entrySet()) {
            entry.getValue().cleanup();
        }
        activeSessions.clear();
    }
    
    /**
     * 🔐 GET CURRENT USER ID
     */
    private String getCurrentUserId() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null && auth.getCurrentUser() != null) {
                return auth.getCurrentUser().getUid();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to get current user ID", e);
        }
        return null;
    }
    
    private ChatSessionController(Context context) {
        this.applicationContext = context != null ? context.getApplicationContext() : null;
    }
    
    /**
     * 🎯 CHAT SESSION CLASS - Individual session state
     */
    public static class ChatSession {
        private final String chatId;
        private final String currentUserId;
        private final String otherUserId;
        private final long createdTime;
        private ChatStateManager stateManager;
        
        // Session data
        private Map<String, Object> sessionData;
        
        public ChatSession(String chatId, String currentUserId, String otherUserId) {
            this.chatId = chatId;
            this.currentUserId = currentUserId;
            this.otherUserId = otherUserId;
            this.createdTime = System.currentTimeMillis();
            this.sessionData = new HashMap<>();
        }

        /**
         * 🔥 TELEGRAM-STYLE: Lazy-load StateManager for persistence
         */
        public ChatStateManager getStateManager() {
            if (stateManager == null) {
                stateManager = ChatStateManager.getInstance();
                Log.d("ChatSession", "🚀 Using Singleton StateManager for session: " + chatId);
            }
            return stateManager;
        }
        
        public void initFromBundle(Bundle bundle) {
            if (bundle == null) return;
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) sessionData.put(key, value);
            }
        }
        
        public void updateFromBundle(Bundle bundle) {
            if (bundle == null) return;
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) sessionData.put(key, value);
            }
        }
        
        public String getChatId() { return chatId; }
        public String getCurrentUserId() { return currentUserId; }
        public String getOtherUserId() { return otherUserId; }
        
        public void cleanup() {
            if (stateManager != null) {
                stateManager.clearMessages();
            }
            sessionData.clear();
        }
    }
}
