package com.example.kitchenbrain.state;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.repository.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🔥 TELEGRAM-LEVEL STATE MANAGER - Final Architecture
 * Features: Single source of truth + immutable snapshots + locked state updates + Persistence
 */
public class ChatStateManager {
    
    private static final String TAG = "ChatStateManager";
    
    // 🔥 SINGLETON INSTANCE
    private static ChatStateManager instance;
    
    public static synchronized ChatStateManager getInstance() {
        if (instance == null) {
            instance = new ChatStateManager();
        }
        return instance;
    }
    
    // 🔥 CRITICAL: Single source of truth with thread safety
    private volatile ChatState currentState = new ChatState();
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();
    
    // 🔥 PERSISTENCE: Firebase listener management
    private ListenerRegistration firebaseListener;
    private String currentChatId;
    
    // 🔥 CRITICAL: Synchronization lock for atomic state updates
    private final Object stateLock = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface StateListener {
        void onStateChanged(List<ChatMessage> messages);
    }
    
    private ChatStateManager() {
        // Private constructor for singleton
    }
    
    /**
     * 🔥 PERSISTENCE: Start listening if not already listening to this chat
     */
    public void startListening(String chatId, ChatRepository repository) {
        if (chatId == null || repository == null) return;
        
        synchronized (stateLock) {
            if (chatId.equals(currentChatId) && firebaseListener != null) {
                Log.d(TAG, "✅ Already listening to chat: " + chatId);
                return;
            }
            
            Log.d(TAG, "🔄 Switching listener to chat: " + chatId);
            stopListening();
            
            currentChatId = chatId;
            firebaseListener = repository.listenMessages(chatId, new ChatRepository.MessageListener() {
                @Override
                public void onMessages(List<ChatMessage> messages) {
                    updateMessages(messages);
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "❌ Firebase listener error", error);
                }
            });
        }
    }
    
    /**
     * 🔥 PERSISTENCE: Stop listener
     */
    public void stopListening() {
        synchronized (stateLock) {
            if (firebaseListener != null) {
                firebaseListener.remove();
                firebaseListener = null;
                Log.d(TAG, "🛑 Stopped listening to chat: " + currentChatId);
            }
            currentChatId = null;
            // Optionally clear messages when stopping
            // clearMessages(); 
        }
    }
    
    /**
     * 🔥 CRITICAL: Locked atomic state update (final architecture)
     */
    public void updateMessages(List<ChatMessage> newMessages) {
        Log.d(TAG, "🔥 LOCKED STATE UPDATE: " + newMessages.size() + " messages");
        
        synchronized (stateLock) {
            List<ChatMessage> filteredMessages = filterAndSortMessages(newMessages);
            ChatState newState = new ChatState(new ArrayList<>(filteredMessages), false, null);
            currentState = newState;
            
            Log.d(TAG, "✅ STATE UPDATED ATOMICALLY: " + filteredMessages.size() + " messages");
            notifyListeners(filteredMessages);
        }
    }
    
    private void notifyListeners(List<ChatMessage> messages) {
        mainHandler.post(() -> {
            Log.d(TAG, "🔥 NOTIFYING LISTENERS: " + listeners.size() + " listeners");
            for (StateListener listener : listeners) {
                try {
                    listener.onStateChanged(new ArrayList<>(messages));
                } catch (Exception e) {
                    Log.e(TAG, "❌ Listener notification failed", e);
                }
            }
        });
    }
    
    /**
     * 🔥 CRITICAL: Add single message atomically (optimistic updates)
     */
    public void addMessage(ChatMessage message) {
        synchronized (stateLock) {
            List<ChatMessage> currentMessages = new ArrayList<>(currentState.getMessages());
            
            // Deduplication
            boolean exists = false;
            for (ChatMessage m : currentMessages) {
                if (m.getMessageId().equals(message.getMessageId())) {
                    exists = true;
                    break;
                }
            }
            if (exists) return;

            currentMessages.add(message);
            List<ChatMessage> sortedMessages = sortMessages(currentMessages);
            currentState = new ChatState(sortedMessages, false, null);
            notifyListeners(sortedMessages);
        }
    }
    
    /**
     * 🔥 CRITICAL: Update message status atomically
     */
    public void updateMessageStatus(String messageId, ChatMessage.MessageStatus newStatus) {
        synchronized (stateLock) {
            List<ChatMessage> currentMessages = new ArrayList<>(currentState.getMessages());
            boolean messageFound = false;
            
            for (int i = 0; i < currentMessages.size(); i++) {
                ChatMessage msg = currentMessages.get(i);
                if (msg.getMessageId().equals(messageId)) {
                    ChatMessage updatedMessage = createMessageWithNewStatus(msg, newStatus);
                    currentMessages.set(i, updatedMessage);
                    messageFound = true;
                    break;
                }
            }
            
            if (messageFound) {
                currentState = new ChatState(currentMessages, false, null);
                notifyListeners(currentMessages);
            }
        }
    }
    
    private ChatMessage createMessageWithNewStatus(ChatMessage original, ChatMessage.MessageStatus newStatus) {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessageId(original.getMessageId());
        newMessage.setSenderId(original.getSenderId());
        newMessage.setReceiverId(original.getReceiverId());
        newMessage.setText(original.getText());
        newMessage.setTimestamp(original.getTimestamp());
        newMessage.setStatus(newStatus);
        newMessage.setMessageStatus(original.getMessageStatus());
        newMessage.setSenderName(original.getSenderName());
        newMessage.setRead(original.isRead());
        return newMessage;
    }
    
    public void updateMessage(ChatMessage updatedMessage) {
        if (updatedMessage == null || updatedMessage.getMessageId() == null) return;
        
        synchronized (stateLock) {
            List<ChatMessage> currentMessages = new ArrayList<>(currentState.getMessages());
            boolean found = false;
            for (int i = 0; i < currentMessages.size(); i++) {
                if (currentMessages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                    currentMessages.set(i, updatedMessage);
                    found = true;
                    break;
                }
            }
            if (found) {
                List<ChatMessage> sortedMessages = sortMessages(currentMessages);
                currentState = currentState.withMessages(sortedMessages);
                notifyListeners(sortedMessages);
            }
        }
    }
    
    public void removeMessage(String messageId) {
        if (messageId == null) return;
        synchronized (stateLock) {
            List<ChatMessage> currentMessages = new ArrayList<>(currentState.getMessages());
            boolean removed = currentMessages.removeIf(msg -> msg.getMessageId().equals(messageId));
            if (removed) {
                currentState = currentState.withMessages(currentMessages);
                notifyListeners(currentMessages);
            }
        }
    }
    
    public void setLoading(boolean loading) {
        synchronized (stateLock) {
            currentState = currentState.withLoading(loading);
            notifyListeners(currentState.getMessages());
        }
    }
    
    public void setError(String error) {
        synchronized (stateLock) {
            currentState = currentState.withError(error);
            notifyListeners(currentState.getMessages());
        }
    }
    
    public void clearMessages() {
        synchronized (stateLock) {
            currentState = currentState.withMessages(Collections.emptyList());
            notifyListeners(Collections.emptyList());
        }
    }
    
    public ChatState getCurrentState() {
        return currentState;
    }
    
    private List<ChatMessage> filterAndSortMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return Collections.emptyList();
        List<ChatMessage> validMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg != null && msg.getMessageId() != null && msg.getText() != null) {
                validMessages.add(msg);
            }
        }
        return sortMessages(validMessages);
    }
    
    private List<ChatMessage> sortMessages(List<ChatMessage> messages) {
        if (messages.isEmpty()) return messages;
        List<ChatMessage> sorted = new ArrayList<>(messages);
        Collections.sort(sorted, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage a, ChatMessage b) {
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
            }
        });
        return sorted;
    }
    
    public void addStateListener(StateListener listener) {
        if (listener != null) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                Log.d(TAG, "➕ Listener added. Total: " + listeners.size());
            }
            listener.onStateChanged(currentState.getMessages());
        }
    }
    
    public void removeStateListener(StateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            Log.d(TAG, "➖ Listener removed. Total: " + listeners.size());
        }
    }
}
