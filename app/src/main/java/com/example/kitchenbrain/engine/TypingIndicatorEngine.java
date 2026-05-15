package com.example.kitchenbrain.engine;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import com.example.kitchenbrain.repository.ChatRepository;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔥 TYPING INDICATOR ENGINE - Telegram Style
 * Features: Smart debouncing + Firebase metadata + Real-time signals
 */
public class TypingIndicatorEngine {
    
    private static final String TAG = "TypingEngine";
    private static final long TYPING_TIMEOUT_MS = 1500; // Stop typing after 1.5s
    
    private final ChatRepository repository;
    private final TypingListener listener;
    private final Handler typingHandler;
    
    // Debouncing system
    private final Map<String, Runnable> stopTypingRunnables = new HashMap<>();
    private final Map<String, Boolean> isTypingStates = new HashMap<>();
    
    public interface TypingListener {
        void onTypingStarted(String userId, String username);
        void onTypingStopped(String userId);
    }
    
    public TypingIndicatorEngine(ChatRepository repository, TypingListener listener) {
        this.repository = repository;
        this.listener = listener;
        this.typingHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 🔥 TYPING TEXTWATCHER - Smart debouncing
     * Only sends true/false to Firebase, not every character
     */
    public TextWatcher createTypingWatcher(String chatId, String currentUserId) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // User started typing
                if (s.length() > 0) {
                    startTyping(chatId, currentUserId);
                } else {
                    stopTyping(chatId, currentUserId);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        };
    }
    
    /**
     * 🚀 START TYPING - Firebase metadata update
     */
    private void startTyping(String chatId, String userId) {
        // Cancel existing stop typing runnable
        cancelStopTypingRunnable(userId);
        
        // Only send if not already typing
        if (!Boolean.TRUE.equals(isTypingStates.get(userId))) {
            Log.d(TAG, "🔤 User " + userId + " started typing");
            
            // Update Firebase metadata
            repository.setTyping(chatId, userId, true);
            
            // Update local state
            isTypingStates.put(userId, true);
        }
        
        // Schedule stop typing
        scheduleStopTypingRunnable(chatId, userId);
    }
    
    /**
     * 🛑 STOP TYPING - Firebase metadata update
     */
    private void stopTyping(String chatId, String userId) {
        cancelStopTypingRunnable(userId);
        
        if (Boolean.TRUE.equals(isTypingStates.get(userId))) {
            Log.d(TAG, "🔤 User " + userId + " stopped typing");
            
            // Update Firebase metadata
            repository.setTyping(chatId, userId, false);
            
            // Update local state
            isTypingStates.put(userId, false);
        }
    }
    
    /**
     * ⏰ SCHEDULE STOP TYPING - Debouncing logic
     */
    private void scheduleStopTypingRunnable(String chatId, String userId) {
        Runnable stopRunnable = () -> {
            Log.d(TAG, "⏰ Typing timeout for user " + userId);
            stopTyping(chatId, userId);
        };
        
        stopTypingRunnables.put(userId, stopRunnable);
        typingHandler.postDelayed(stopRunnable, TYPING_TIMEOUT_MS);
    }
    
    /**
     * ❌ CANCEL STOP TYPING RUNNABLE
     */
    private void cancelStopTypingRunnable(String userId) {
        Runnable existingRunnable = stopTypingRunnables.get(userId);
        if (existingRunnable != null) {
            typingHandler.removeCallbacks(existingRunnable);
            stopTypingRunnables.remove(userId);
        }
    }
    
    /**
     * 👀 LISTEN TO TYPING EVENTS - Real-time updates
     */
    public void listenToTypingEvents(String chatId, String currentUserId) {
        repository.listenToTyping(chatId, new ChatRepository.TypingListener() {
            @Override
            public void onTypingChanged(Map<String, Boolean> typingStates) {
                for (Map.Entry<String, Boolean> entry : typingStates.entrySet()) {
                    String userId = entry.getKey();
                    Boolean isTyping = entry.getValue();
                    
                    // Skip current user
                    if (userId.equals(currentUserId)) {
                        continue;
                    }
                    
                    // Handle typing state change
                    if (Boolean.TRUE.equals(isTyping)) {
                        if (listener != null) {
                            listener.onTypingStarted(userId, getUsernameForId(userId));
                        }
                    } else {
                        if (listener != null) {
                            listener.onTypingStopped(userId);
                        }
                    }
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "❌ Typing listener error", error);
            }
        });
    }
    
    /**
     * 🧹 CLEANUP - Remove all handlers
     */
    public void cleanup() {
        Log.d(TAG, "🧹 Cleaning up typing handlers");
        
        // Cancel all pending runnables
        for (Runnable runnable : stopTypingRunnables.values()) {
            typingHandler.removeCallbacks(runnable);
        }
        
        // Clear state
        stopTypingRunnables.clear();
        isTypingStates.clear();
    }
    
    /**
     * 👤 GET USERNAME FOR ID - Placeholder implementation
     * In real app, this would query user data
     */
    private String getUsernameForId(String userId) {
        // TODO: Implement proper username lookup
        return "User_" + userId.substring(0, 8);
    }
}
