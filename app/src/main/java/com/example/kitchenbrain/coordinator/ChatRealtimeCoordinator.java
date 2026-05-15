package com.example.kitchenbrain.coordinator;

import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.provider.ChatIdProvider;
import com.example.kitchenbrain.repository.ChatRepository;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔥 CHAT REALTIME COORDINATOR - Telegram/WhatsApp Level Consistency
 * Features: Event deduplication + state reconciliation + message ordering + engine orchestration
 */
public class ChatRealtimeCoordinator {
    
    private static final String TAG = "ChatCoordinator";
    
    // Event deduplication cache
    private final Map<String, Set<String>> processedEvents = new ConcurrentHashMap<>();
    private final Map<String, Long> lastEventTime = new ConcurrentHashMap<>();
    
    // State reconciliation
    private final Map<String, ChatMessage> localState = new ConcurrentHashMap<>();
    private final Map<String, ChatMessage> remoteState = new ConcurrentHashMap<>();
    
    // Message ordering
    private final List<ChatMessage> orderedMessages = new ArrayList<>();
    private final Object messageLock = new Object();
    
    // Engine coordination
    private final List<CoordinatorListener> listeners = new ArrayList<>();
    
    // Firebase repository
    private ChatRepository chatRepository;
    
    public interface CoordinatorListener {
        void onMessageAdded(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onMessageRemoved(String messageId);
        void onStateChanged(String chatId, ChatState state);
        void onConflictResolved(String messageId, ChatMessage resolved);
        
        interface StatusListener {
            void onMessageStatusChanged(String messageId, ChatMessage.MessageStatus status);
        }
    }
    
    public enum ChatState {
        SYNCING,
        STABLE,
        CONFLICT,
        OFFLINE
    }
    
    /**
     * 🎯 PROCESS INCOMING MESSAGE - Deduplication + State Reconciliation
     */
    public void processIncomingMessage(ChatMessage message, String chatId) {
        if (message == null || message.getMessageId() == null) {
            Log.e(TAG, "❌ Invalid message received");
            return;
        }
        
        Log.d(TAG, "📥 Processing incoming message: " + message.getMessageId());
        
        // 🔥 EVENT DEDUPLICATION
        if (isDuplicateEvent(message.getMessageId(), chatId)) {
            Log.d(TAG, "🔄 Duplicate event detected, skipping: " + message.getMessageId());
            return;
        }
        
        markEventProcessed(message.getMessageId(), chatId);
        
        // 🔥 STATE RECONCILIATION
        ChatMessage resolved = reconcileMessageState(message);
        
        // 🔥 MESSAGE ORDERING
        addOrderedMessage(resolved);
        
        // 🔥 NOTIFY LISTENERS
        notifyMessageAdded(resolved);
        
        Log.d(TAG, "✅ Message processed successfully: " + message.getMessageId());
    }
    
    /**
     * 🔥 PROCESS OPTIMISTIC MESSAGE - Local state management + Firebase send
     */
    public void processOptimisticMessage(ChatMessage message, String chatId) {
        if (message == null || message.getMessageId() == null) {
            Log.e(TAG, "❌ Invalid optimistic message");
            return;
        }
        
        Log.d(TAG, "📤 Processing optimistic message: " + message.getMessageId());
        Log.d("CHAT_DEBUG", "🔥 BEFORE FIREBASE CALL in coordinator");
        
        // Add to local state immediately
        localState.put(message.getMessageId(), message);
        
        // Add to ordered messages
        addOrderedMessage(message);
        
        // Notify listeners
        notifyMessageAdded(message);
        
        Log.d(TAG, "✅ Optimistic message processed: " + message.getMessageId());
        
        // 🔥 CRITICAL: Actually send to Firebase
        if (chatRepository != null) {
            Log.d("CHAT_DEBUG", "🔥 INSIDE FIREBASE SEND - calling repository");
            chatRepository.sendMessage(chatId, message, new ChatRepository.FirebaseCallback() {
                @Override
                public void onSuccess() {
                    Log.d("CHAT_DEBUG", "✅ FIREBASE SUCCESS from coordinator: " + message.getMessageId());
                    // Update message status to SENT
                    message.setStatus(ChatMessage.MessageStatus.SENT);
                    updateMessageStatus(message.getMessageId(), ChatMessage.MessageStatus.SENT);
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e("CHAT_DEBUG", "❌ FIREBASE ERROR from coordinator: " + e.getMessage());
                    // Update message status to FAILED
                    message.setStatus(ChatMessage.MessageStatus.FAILED);
                    updateMessageStatus(message.getMessageId(), ChatMessage.MessageStatus.FAILED);
                }
            });
        } else {
            Log.e("CHAT_DEBUG", "❌ chatRepository is null in coordinator");
        }
    }
    
    /**
     * 🔄 RECONCILE MESSAGE STATE - Local vs Remote merge
     */
    private ChatMessage reconcileMessageState(ChatMessage incomingMessage) {
        String messageId = incomingMessage.getMessageId();
        
        ChatMessage localMessage = localState.get(messageId);
        ChatMessage remoteMessage = remoteState.get(messageId);
        
        if (localMessage == null && remoteMessage == null) {
            // New message
            remoteState.put(messageId, incomingMessage);
            return incomingMessage;
        }
        
        if (localMessage != null && remoteMessage == null) {
            // Local message confirmed by server
            if (isServerConfirmation(localMessage, incomingMessage)) {
                remoteState.put(messageId, incomingMessage);
                return mergeMessageStates(localMessage, incomingMessage);
            }
        }
        
        if (localMessage == null && remoteMessage != null) {
            // Remote message only
            return incomingMessage;
        }
        
        // Both exist - resolve conflict
        ChatMessage resolved = resolveMessageConflict(localMessage, incomingMessage);
        remoteState.put(messageId, resolved);
        
        Log.d(TAG, "🔧 Message conflict resolved: " + messageId);
        notifyConflictResolved(messageId, resolved);
        
        return resolved;
    }
    
    /**
     * 📊 ADD ORDERED MESSAGE - Timestamp normalization + sorting
     */
    private void addOrderedMessage(ChatMessage message) {
        synchronized (messageLock) {
            // Normalize timestamp for consistent ordering
            ChatMessage normalized = normalizeTimestamp(message);
            
            // Check if message already exists
            for (int i = 0; i < orderedMessages.size(); i++) {
                if (orderedMessages.get(i).getMessageId().equals(normalized.getMessageId())) {
                    // Update existing message
                    orderedMessages.set(i, normalized);
                    return;
                }
            }
            
            // Add new message
            orderedMessages.add(normalized);
            
            // Sort by timestamp
            Collections.sort(orderedMessages, new MessageComparator());
        }
    }
    
    /**
     * ⏰ NORMALIZE TIMESTAMP - Server timestamp priority
     */
    private ChatMessage normalizeTimestamp(ChatMessage message) {
        if (message.getTimestamp() == null) {
            // Use current time as fallback
            message.setTimestamp(new Timestamp(new java.util.Date(System.currentTimeMillis())));
            Log.d(TAG, "⏰ Used fallback timestamp for: " + message.getMessageId());
        }
        
        return message;
    }
    
    /**
     * 🔍 IS DUPLICATE EVENT - Prevent Firebase event duplication
     */
    private boolean isDuplicateEvent(String messageId, String chatId) {
        Set<String> chatEvents = processedEvents.get(chatId);
        if (chatEvents == null) {
            chatEvents = new HashSet<>();
            processedEvents.put(chatId, chatEvents);
        }
        
        return chatEvents.contains(messageId);
    }
    
    /**
     * ✅ MARK EVENT PROCESSED - Track processed events
     */
    private void markEventProcessed(String messageId, String chatId) {
        Set<String> chatEvents = processedEvents.get(chatId);
        if (chatEvents == null) {
            chatEvents = new HashSet<>();
            processedEvents.put(chatId, chatEvents);
        }
        
        chatEvents.add(messageId);
        lastEventTime.put(chatId, System.currentTimeMillis());
    }
    
    /**
     * 🔧 RESOLVE MESSAGE CONFLICT - Local vs Remote priority
     */
    private ChatMessage resolveMessageConflict(ChatMessage local, ChatMessage remote) {
        // Priority: Server timestamp > Local timestamp
        if (remote.getTimestamp() != null && local.getTimestamp() != null) {
            if (remote.getTimestamp().toDate().after(local.getTimestamp().toDate())) {
                Log.d(TAG, "🔧 Remote message wins (newer timestamp): " + remote.getMessageId());
                return remote;
            }
        }
        
        // Priority: Server status > Local status
        if (remote.getStatus() != null && local.getStatus() != null) {
            if (remote.getStatus().ordinal() > local.getStatus().ordinal()) {
                Log.d(TAG, "🔧 Remote message wins (higher status): " + remote.getMessageId());
                return remote;
            }
        }
        
        // Default: Keep local (optimistic) state
        Log.d(TAG, "🔧 Local message wins (default): " + local.getMessageId());
        return local;
    }
    
    /**
     * 🔄 MERGE MESSAGE STATES - Combine local and remote
     */
    private ChatMessage mergeMessageStates(ChatMessage local, ChatMessage remote) {
        ChatMessage merged = new ChatMessage();
        merged.setMessageId(local.getMessageId());
        merged.setSenderId(local.getSenderId());
        merged.setReceiverId(local.getReceiverId());
        merged.setText(local.getText());
        
        // Use remote timestamp if available
        merged.setTimestamp(remote.getTimestamp() != null ? remote.getTimestamp() : local.getTimestamp());
        
        // Use remote status if more advanced
        merged.setStatus(remote.getStatus() != null && remote.getStatus().ordinal() > local.getStatus().ordinal() ? 
                      remote.getStatus() : local.getStatus());
        
        // Preserve read receipts from remote
        merged.setRead(remote.isRead());
        merged.setReadAt(remote.getReadAt());
        
        return merged;
    }
    
    /**
     * 🔍 IS SERVER CONFIRMATION - Check if remote confirms local
     */
    private boolean isServerConfirmation(ChatMessage local, ChatMessage remote) {
        return local.getMessageId().equals(remote.getMessageId()) &&
               local.getSenderId().equals(remote.getSenderId()) &&
               local.getText().equals(remote.getText());
    }
    
    /**
     * 📊 GET ORDERED MESSAGES - Thread-safe access
     */
    public List<ChatMessage> getOrderedMessages() {
        synchronized (messageLock) {
            return new ArrayList<>(orderedMessages);
        }
    }
    
    /**
     * 🔥 UPDATE MESSAGE STATUS - Critical for SENDING → SENT transition
     */
    private void updateMessageStatus(String messageId, ChatMessage.MessageStatus newStatus) {
        Log.d("CHAT_DEBUG", "🔥 UPDATING MESSAGE STATUS: " + messageId + " → " + newStatus);
        
        // 🔥 CRITICAL FIX: Create immutable list for ListAdapter diff
        List<ChatMessage> updatedMessages = new ArrayList<>();
        boolean messageFound = false;
        ChatMessage lastUpdatedMsg = null;
        
        for (ChatMessage msg : orderedMessages) {
            if (msg.getMessageId().equals(messageId)) {
                // 🔥 TELEGRAM-LEVEL: Create new message with updated status
                ChatMessage updatedMessage = createMessageWithNewStatus(msg, newStatus);
                updatedMessages.add(updatedMessage);
                messageFound = true;
                lastUpdatedMsg = updatedMessage;
                Log.d("CHAT_DEBUG", "✅ Message status changed in list: " + messageId + " → " + newStatus);
            } else {
                updatedMessages.add(msg);
            }
        }
        
        if (messageFound) {
            // 🔥 CRITICAL: Replace entire list for ListAdapter diff
            synchronized (messageLock) {
                orderedMessages.clear();
                orderedMessages.addAll(updatedMessages);
                Log.d("CHAT_DEBUG", "✅ Ordered messages list replaced for status update");
            }
            
            // Update local state
            ChatMessage localMessage = localState.get(messageId);
            if (localMessage != null) {
                localMessage.setStatus(newStatus);
                Log.d("CHAT_DEBUG", "✅ Local state updated for: " + messageId);
            }
            
            // 🔥 CRITICAL: Route through state manager instead of direct adapter updates
            // This ensures single source of truth architecture
            Log.d("CHAT_DEBUG", "✅ Status update ready for state manager routing");
            notifyMessageUpdated(lastUpdatedMsg);
        } else {
            Log.w("CHAT_DEBUG", "❌ Message not found for status update: " + messageId);
        }
        
        Log.d("CHAT_DEBUG", "✅ Message status update complete for: " + messageId);
    }
    
    /**
     * 🔥 CREATE MESSAGE WITH NEW STATUS - Immutable copy
     */
    private ChatMessage createMessageWithNewStatus(ChatMessage original, ChatMessage.MessageStatus newStatus) {
        ChatMessage newMessage = new ChatMessage();
        newMessage.setMessageId(original.getMessageId());
        newMessage.setSenderId(original.getSenderId());
        newMessage.setReceiverId(original.getReceiverId());
        newMessage.setText(original.getText());
        newMessage.setTimestamp(original.getTimestamp());
        newMessage.setStatus(newStatus); // 🔥 NEW STATUS
        newMessage.setMessageStatus(original.getMessageStatus());
        newMessage.setSenderName(original.getSenderName());
        newMessage.setRead(original.isRead());
        return newMessage;
    }
    
    /**
     * 🔥 NOTIFY MESSAGES UPDATED - For ListAdapter diff
     */
    private void notifyMessagesUpdated(List<ChatMessage> updatedMessages) {
        for (CoordinatorListener listener : listeners) {
            // 🔥 TELEGRAM-LEVEL: Send full list update for proper diff
            for (ChatMessage message : updatedMessages) {
                listener.onMessageUpdated(message);
            }
        }
    }
    
    /**
     * 🔥 NOTIFY MESSAGE STATUS CHANGED - For UI updates
     */
    private void notifyMessageStatusChanged(String messageId, ChatMessage.MessageStatus status) {
        for (CoordinatorListener listener : listeners) {
            if (listener instanceof CoordinatorListener.StatusListener) {
                ((CoordinatorListener.StatusListener) listener).onMessageStatusChanged(messageId, status);
            }
        }
    }
    
    /**
     * 🔥 SET CHAT REPOSITORY - Dependency injection
     */
    public void setChatRepository(ChatRepository repository) {
        this.chatRepository = repository;
        Log.d(TAG, "ChatRepository set in coordinator");
    }
    
    /**
     * 🧹 CLEANUP OLD EVENTS - Prevent memory leaks
     */
    public void cleanupOldEvents(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        
        lastEventTime.entrySet().removeIf(entry -> {
            boolean shouldRemove = currentTime - entry.getValue() > maxAgeMs;
            if (shouldRemove) {
                processedEvents.remove(entry.getKey());
                Log.d(TAG, "🧹 Cleaned up old events for chat: " + entry.getKey());
            }
            return shouldRemove;
        });
    }
    
    /**
     * 📊 GET CHAT STATE - Current coordination state
     */
    public ChatState getChatState(String chatId) {
        Long lastEvent = lastEventTime.get(chatId);
        if (lastEvent == null) {
            return ChatState.OFFLINE;
        }
        
        long timeSinceLastEvent = System.currentTimeMillis() - lastEvent;
        if (timeSinceLastEvent > 30000) { // 30 seconds
            return ChatState.OFFLINE;
        } else if (timeSinceLastEvent > 5000) { // 5 seconds
            return ChatState.STABLE;
        } else {
            return ChatState.SYNCING;
        }
    }
    
    /**
     * 📢 ADD COORDINATOR LISTENER
     */
    public void addListener(CoordinatorListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 📢 REMOVE COORDINATOR LISTENER
     */
    public void removeListener(CoordinatorListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 📢 NOTIFY MESSAGE ADDED
     */
    private void notifyMessageAdded(ChatMessage message) {
        for (CoordinatorListener listener : listeners) {
            try {
                listener.onMessageAdded(message);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying listener", e);
            }
        }
    }
    
    /**
     * 📢 NOTIFY MESSAGE UPDATED
     */
    private void notifyMessageUpdated(ChatMessage message) {
        for (CoordinatorListener listener : listeners) {
            try {
                listener.onMessageUpdated(message);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying listener", e);
            }
        }
    }
    
    /**
     * 📢 NOTIFY CONFLICT RESOLVED
     */
    private void notifyConflictResolved(String messageId, ChatMessage resolved) {
        for (CoordinatorListener listener : listeners) {
            try {
                listener.onConflictResolved(messageId, resolved);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying listener", e);
            }
        }
    }
    
    /**
     * 📊 MESSAGE COMPARATOR - Consistent ordering
     */
    private static class MessageComparator implements Comparator<ChatMessage> {
        @Override
        public int compare(ChatMessage m1, ChatMessage m2) {
            if (m1.getTimestamp() == null && m2.getTimestamp() == null) {
                return 0;
            }
            if (m1.getTimestamp() == null) {
                return 1;
            }
            if (m2.getTimestamp() == null) {
                return -1;
            }
            
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        }
    }
}
