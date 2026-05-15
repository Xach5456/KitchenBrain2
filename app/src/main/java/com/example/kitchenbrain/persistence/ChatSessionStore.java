package com.example.kitchenbrain.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.kitchenbrain.session.ChatSessionController;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔥 CHAT SESSION PERSISTENCE ENGINE - Telegram-grade Distributed State
 * Features: 3-layer model (RAM + Disk + Server) + Recovery + Sync
 */
public class ChatSessionStore {
    
    private static final String TAG = "ChatSessionStore";
    private static final String PREFS_NAME = "chat_sessions";
    private static final String SESSION_KEY_PREFIX = "session_";
    
    // Singleton instance
    private static volatile ChatSessionStore instance;
    private static final Object LOCK = new Object();
    
    // Persistence layers
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final FirebaseFirestore firestore;
    
    // Active session cache (RAM layer)
    private final Map<String, SessionData> sessionCache = new ConcurrentHashMap<>();
    
    // Server sync listeners
    private final Map<String, ListenerRegistration> syncListeners = new ConcurrentHashMap<>();
    
    // Session listener
    public interface SessionPersistenceListener {
        void onSessionRestored(String chatId, SessionData session);
        void onSessionSynced(String chatId, SessionData session);
        void onSessionConflict(String chatId, SessionData local, SessionData server);
        void onPersistenceError(String chatId, Exception error);
    }
    
    private SessionPersistenceListener persistenceListener;
    
    /**
     * 🔥 Get singleton instance
     */
    public static ChatSessionStore getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ChatSessionStore(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    private ChatSessionStore(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        
        Log.d(TAG, "🔥 ChatSessionStore initialized");
    }
    
    /**
     * 💾 PERSIST SESSION - 3-layer save (RAM + Disk + Server)
     */
    public void persistSession(String chatId, SessionData session) {
        if (chatId == null || session == null) {
            Log.e(TAG, "❌ Cannot persist session: invalid parameters");
            return;
        }
        
        Log.d(TAG, "💾 Persisting session: " + chatId);
        
        // 🔥 LAYER 1: RAM (immediate)
        sessionCache.put(chatId, session);
        
        // 🔥 LAYER 2: Disk (persistent)
        persistToDisk(chatId, session);
        
        // 🔥 LAYER 3: Server (distributed)
        persistToServer(chatId, session);
        
        Log.d(TAG, "✅ Session persisted to all layers: " + chatId);
    }
    
    /**
     * 📥 RESTORE SESSION - 3-layer recovery (Server → Disk → RAM)
     */
    public SessionData restoreSession(String chatId) {
        if (chatId == null) {
            Log.e(TAG, "❌ Cannot restore session: chatId is null");
            return null;
        }
        
        Log.d(TAG, "📥 Restoring session: " + chatId);
        
        // 🔥 LAYER 1: Check RAM first (fastest)
        SessionData cached = sessionCache.get(chatId);
        if (cached != null && cached.isValid()) {
            Log.d(TAG, "✅ Session restored from RAM: " + chatId);
            return cached;
        }
        
        // 🔥 LAYER 2: Check Disk (persistent)
        SessionData diskSession = restoreFromDisk(chatId);
        if (diskSession != null && diskSession.isValid()) {
            // Cache in RAM for next time
            sessionCache.put(chatId, diskSession);
            Log.d(TAG, "✅ Session restored from Disk: " + chatId);
            
            // Start server sync in background
            syncWithServer(chatId, diskSession);
            return diskSession;
        }
        
        // 🔥 LAYER 3: Check Server (source of truth)
        SessionData serverSession = restoreFromServer(chatId);
        if (serverSession != null && serverSession.isValid()) {
            // Cache in RAM and Disk
            sessionCache.put(chatId, serverSession);
            persistToDisk(chatId, serverSession);
            Log.d(TAG, "✅ Session restored from Server: " + chatId);
            return serverSession;
        }
        
        Log.w(TAG, "⚠️ Session not found anywhere: " + chatId);
        return null;
    }
    
    /**
     * 🗑️ DELETE SESSION - Remove from all layers
     */
    public void deleteSession(String chatId) {
        if (chatId == null) {
            Log.e(TAG, "❌ Cannot delete session: chatId is null");
            return;
        }
        
        Log.d(TAG, "🗑️ Deleting session: " + chatId);
        
        // Remove from RAM
        sessionCache.remove(chatId);
        
        // Remove from Disk
        sharedPreferences.edit().remove(SESSION_KEY_PREFIX + chatId).apply();
        
        // Remove from Server
        firestore.collection("chat_sessions").document(chatId).delete();
        
        // Remove sync listener
        ListenerRegistration listener = syncListeners.remove(chatId);
        if (listener != null) {
            listener.remove();
        }
        
        Log.d(TAG, "✅ Session deleted from all layers: " + chatId);
    }
    
    /**
     * 🔄 SYNC SESSION WITH SERVER - Real-time reconciliation
     */
    private void syncWithServer(String chatId, SessionData localSession) {
        Log.d(TAG, "🔄 Syncing session with server: " + chatId);
        
        // Set up real-time listener
        ListenerRegistration listener = firestore.collection("chat_sessions")
                .document(chatId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Server sync error: " + chatId, error);
                        if (persistenceListener != null) {
                            persistenceListener.onPersistenceError(chatId, error);
                        }
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        SessionData serverSession = documentToSession(snapshot);
                        
                        if (serverSession != null && serverSession.isValid()) {
                            // Check for conflicts
                            if (!localSession.equals(serverSession)) {
                                Log.w(TAG, "⚠️ Session conflict detected: " + chatId);
                                if (persistenceListener != null) {
                                    persistenceListener.onSessionConflict(chatId, localSession, serverSession);
                                }
                                
                                // Resolve conflict (server wins for now)
                                sessionCache.put(chatId, serverSession);
                                persistToDisk(chatId, serverSession);
                            } else {
                                Log.d(TAG, "✅ Session in sync: " + chatId);
                            }
                            
                            if (persistenceListener != null) {
                                persistenceListener.onSessionSynced(chatId, serverSession);
                            }
                        }
                    }
                });
        
        syncListeners.put(chatId, listener);
    }
    
    /**
     * 💾 PERSIST TO DISK - SharedPreferences
     */
    private void persistToDisk(String chatId, SessionData session) {
        try {
            JSONObject sessionJson = sessionToJson(session);
            String key = SESSION_KEY_PREFIX + chatId;
            
            sharedPreferences.edit()
                    .putString(key, sessionJson.toString())
                    .apply();
            
            Log.d(TAG, "💾 Session persisted to disk: " + chatId);
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ Failed to persist session to disk: " + chatId, e);
        }
    }
    
    /**
     * 📥 RESTORE FROM DISK - SharedPreferences
     */
    private SessionData restoreFromDisk(String chatId) {
        try {
            String key = SESSION_KEY_PREFIX + chatId;
            String sessionJson = sharedPreferences.getString(key, null);
            
            if (sessionJson != null) {
                JSONObject json = new JSONObject(sessionJson);
                SessionData session = jsonToSession(json);
                
                Log.d(TAG, "📥 Session restored from disk: " + chatId);
                return session;
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ Failed to restore session from disk: " + chatId, e);
        }
        
        return null;
    }
    
    /**
     * 💾 PERSIST TO SERVER - Firestore
     */
    private void persistToServer(String chatId, SessionData session) {
        Map<String, Object> sessionMap = sessionToMap(session);
        
        firestore.collection("chat_sessions")
                .document(chatId)
                .set(sessionMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "💾 Session persisted to server: " + chatId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to persist session to server: " + chatId, e);
                    if (persistenceListener != null) {
                        persistenceListener.onPersistenceError(chatId, e);
                    }
                });
    }
    
    /**
     * 📥 RESTORE FROM SERVER - Firestore
     */
    private SessionData restoreFromServer(String chatId) {
        // Synchronous restore for immediate needs
        // In production, this should be handled with proper async flow
        
        try {
            DocumentSnapshot snapshot = firestore.collection("chat_sessions")
                    .document(chatId)
                    .get()
                    .getResult();
            
            if (snapshot != null && snapshot.exists()) {
                SessionData session = documentToSession(snapshot);
                Log.d(TAG, "📥 Session restored from server: " + chatId);
                return session;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to restore session from server: " + chatId, e);
        }
        
        return null;
    }
    
    /**
     * 🧹 CLEANUP ALL SESSIONS
     */
    public void cleanupAllSessions() {
        Log.d(TAG, "🧹 Cleaning up all sessions...");
        
        // Clear RAM
        sessionCache.clear();
        
        // Clear Disk
        sharedPreferences.edit().clear().apply();
        
        // Remove all server listeners
        for (ListenerRegistration listener : syncListeners.values()) {
            listener.remove();
        }
        syncListeners.clear();
        
        Log.d(TAG, "✅ All sessions cleaned up");
    }
    
    /**
     * 📊 GET CACHED SESSIONS COUNT
     */
    public int getCachedSessionsCount() {
        return sessionCache.size();
    }
    
    /**
     * 📢 SET PERSISTENCE LISTENER
     */
    public void setPersistenceListener(SessionPersistenceListener listener) {
        this.persistenceListener = listener;
    }
    
    /**
     * 🔄 SESSION DATA CLASS
     */
    public static class SessionData {
        public final String chatId;
        public final String currentUserId;
        public final String otherUserId;
        public final long createdTime;
        public final long lastUpdated;
        public final Map<String, Object> sessionData;
        
        public SessionData(String chatId, String currentUserId, String otherUserId, 
                         Map<String, Object> sessionData) {
            this.chatId = chatId;
            this.currentUserId = currentUserId;
            this.otherUserId = otherUserId;
            this.createdTime = System.currentTimeMillis();
            this.lastUpdated = System.currentTimeMillis();
            this.sessionData = new HashMap<>(sessionData);
        }
        
        public boolean isValid() {
            return chatId != null && currentUserId != null && otherUserId != null;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            SessionData that = (SessionData) obj;
            
            return chatId.equals(that.chatId) &&
                   currentUserId.equals(that.currentUserId) &&
                   otherUserId.equals(that.otherUserId);
        }
        
        @Override
        public int hashCode() {
            int result = chatId.hashCode();
            result = 31 * result + currentUserId.hashCode();
            result = 31 * result + otherUserId.hashCode();
            return result;
        }
    }
    
    // Helper methods for JSON conversion
    private JSONObject sessionToJson(SessionData session) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("chatId", session.chatId);
        json.put("currentUserId", session.currentUserId);
        json.put("otherUserId", session.otherUserId);
        json.put("createdTime", session.createdTime);
        json.put("lastUpdated", session.lastUpdated);
        
        JSONObject dataJson = new JSONObject();
        for (Map.Entry<String, Object> entry : session.sessionData.entrySet()) {
            dataJson.put(entry.getKey(), entry.getValue().toString());
        }
        json.put("sessionData", dataJson);
        
        return json;
    }
    
    private SessionData jsonToSession(JSONObject json) throws JSONException {
        String chatId = json.getString("chatId");
        String currentUserId = json.getString("currentUserId");
        String otherUserId = json.getString("otherUserId");
        
        Map<String, Object> sessionData = new HashMap<>();
        JSONObject dataJson = json.getJSONObject("sessionData");
        Iterator<String> keys = dataJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            sessionData.put(key, dataJson.getString(key));
        }
        
        return new SessionData(chatId, currentUserId, otherUserId, sessionData);
    }
    
    private Map<String, Object> sessionToMap(SessionData session) {
        Map<String, Object> map = new HashMap<>();
        map.put("chatId", session.chatId);
        map.put("currentUserId", session.currentUserId);
        map.put("otherUserId", session.otherUserId);
        map.put("createdTime", session.createdTime);
        map.put("lastUpdated", session.lastUpdated);
        map.put("sessionData", session.sessionData);
        return map;
    }
    
    private SessionData documentToSession(DocumentSnapshot snapshot) {
        String chatId = snapshot.getString("chatId");
        String currentUserId = snapshot.getString("currentUserId");
        String otherUserId = snapshot.getString("otherUserId");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = (Map<String, Object>) snapshot.get("sessionData");
        
        if (sessionData == null) {
            sessionData = new HashMap<>();
        }
        
        return new SessionData(chatId, currentUserId, otherUserId, sessionData);
    }
}
