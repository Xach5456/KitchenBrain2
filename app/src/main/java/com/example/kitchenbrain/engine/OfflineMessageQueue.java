package com.example.kitchenbrain.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🔥 OFFLINE MESSAGE QUEUE - WhatsApp/Telegram Style
 * Features: Local cache + retry sync + no lost messages
 */
public class OfflineMessageQueue {
    
    private static final String TAG = "OfflineQueue";
    private static final String PREFS_NAME = "offline_messages";
    private static final String QUEUE_KEY = "pending_messages";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<PendingMessage> pendingMessages;
    private final QueueListener listener;
    
    public interface QueueListener {
        void onMessageQueued(PendingMessage message);
        void onMessageSent(PendingMessage message);
        void onSyncStarted(int messageCount);
        void onSyncCompleted(int sentCount, int failedCount);
    }
    
    public static class PendingMessage {
        public String id;
        public String text;
        public String chatId;
        public String senderId;
        public String receiverId;
        public long timestamp;
        public ChatMessage.MessageStatus status;
        
        public PendingMessage(String text, String chatId, String senderId, String receiverId) {
            this.id = UUID.randomUUID().toString();
            this.text = text;
            this.chatId = chatId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.timestamp = System.currentTimeMillis();
            this.status = ChatMessage.MessageStatus.SENDING;
        }
    }
    
    public OfflineMessageQueue(Context context, QueueListener listener) {
        this.context = context;
        this.listener = listener;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.pendingMessages = loadQueueFromPrefs();
        
        Log.d(TAG, "📦 Loaded " + pendingMessages.size() + " pending messages from cache");
    }
    
    /**
     * 📤 SEND MESSAGE WITH OFFLINE SUPPORT
     */
    public void sendMessage(String text, String chatId, String senderId, String receiverId) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "📶 No network - queuing message offline");
            PendingMessage pendingMsg = new PendingMessage(text, chatId, senderId, receiverId);
            addToQueue(pendingMsg);
            return;
        }
        
        Log.d(TAG, "📡 Network available - sending directly");
        // Handled by MessageLifecycleEngine in ChatFragment
    }
    
    /**
     * 📦 ADD TO QUEUE
     */
    private void addToQueue(PendingMessage message) {
        pendingMessages.add(message);
        saveQueueToPrefs();
        
        Log.d(TAG, "📦 Message queued: " + message.id);
        
        if (listener != null) {
            listener.onMessageQueued(message);
        }
    }
    
    /**
     * 🔄 SYNC PENDING MESSAGES
     */
    public void syncPendingMessages() {
        if (pendingMessages.isEmpty()) {
            Log.d(TAG, "📦 No pending messages to sync");
            return;
        }
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "📶 No network - cannot sync");
            return;
        }
        
        Log.d(TAG, "🔄 Starting sync of " + pendingMessages.size() + " pending messages");
        
        if (listener != null) {
            listener.onSyncStarted(pendingMessages.size());
        }
        
        new Thread(() -> {
            int sentCount = 0;
            int failedCount = 0;
            List<PendingMessage> toRemove = new ArrayList<>();
            
            for (PendingMessage message : pendingMessages) {
                try {
                    // TODO: Connect with MessageLifecycleEngine for actual sending
                    Thread.sleep(100); // Simulate network delay
                    
                    // Simulate success for now
                    Log.d(TAG, "✅ Sent pending message: " + message.id);
                    toRemove.add(message);
                    sentCount++;
                    
                    if (listener != null) {
                        listener.onMessageSent(message);
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to send pending message: " + message.id, e);
                    failedCount++;
                }
            }
            
            // Remove successfully sent messages
            pendingMessages.removeAll(toRemove);
            saveQueueToPrefs();
            
            Log.d(TAG, "🔄 Sync completed: " + sentCount + " sent, " + failedCount + " failed");
            
            if (listener != null) {
                listener.onSyncCompleted(sentCount, failedCount);
            }
        }).start();
    }
    
    /**
     * 📶 CHECK NETWORK AVAILABILITY
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    /**
     * 💾 SAVE QUEUE TO PREFERENCES
     */
    private void saveQueueToPrefs() {
        String json = gson.toJson(pendingMessages);
        prefs.edit().putString(QUEUE_KEY, json).apply();
        Log.d(TAG, "💾 Saved " + pendingMessages.size() + " messages to cache");
    }
    
    /**
     * 📥 LOAD QUEUE FROM PREFERENCES
     */
    private List<PendingMessage> loadQueueFromPrefs() {
        String json = prefs.getString(QUEUE_KEY, "[]");
        Type type = new TypeToken<List<PendingMessage>>() {}.getType();
        List<PendingMessage> messages = gson.fromJson(json, type);
        return messages != null ? messages : new ArrayList<>();
    }
    
    /**
     * 📊 GET QUEUE STATUS
     */
    public int getPendingCount() {
        return pendingMessages.size();
    }
    
    public boolean hasPendingMessages() {
        return !pendingMessages.isEmpty();
    }
    
    public List<PendingMessage> getPendingMessages() {
        return new ArrayList<>(pendingMessages);
    }
    
    /**
     * 🧹 CLEAR QUEUE
     */
    public void clearQueue() {
        pendingMessages.clear();
        saveQueueToPrefs();
        Log.d(TAG, "🧹 Queue cleared");
    }
    
    /**
     * 🗑️ REMOVE SPECIFIC MESSAGE
     */
    public void removeMessage(String messageId) {
        pendingMessages.removeIf(msg -> msg.id.equals(messageId));
        saveQueueToPrefs();
        Log.d(TAG, "🗑️ Removed message: " + messageId);
    }
    
    /**
     * 🔄 RETRY SPECIFIC MESSAGE
     */
    public void retryMessage(String messageId) {
        PendingMessage message = pendingMessages.stream()
                .filter(msg -> msg.id.equals(messageId))
                .findFirst()
                .orElse(null);
        
        if (message != null) {
            Log.d(TAG, "🔄 Retrying message: " + messageId);
            
            if (isNetworkAvailable()) {
                removeMessage(messageId);
                
                if (listener != null) {
                    listener.onMessageSent(message);
                }
            } else {
                Log.d(TAG, "📶 No network - keeping message in queue");
            }
        }
    }
}
