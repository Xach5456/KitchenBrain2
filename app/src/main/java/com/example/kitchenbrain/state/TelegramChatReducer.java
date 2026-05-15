package com.example.kitchenbrain.state;

import android.util.Log;
import com.example.kitchenbrain.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 🔥 TELEGRAM-STYLE CHAT REDUCER - Redux-like State Management
 * Features: Single source of truth + immutable state + single submitList gate
 */
public class TelegramChatReducer {
    
    private static final String TAG = "TelegramReducer";
    
    // 🔥 CRITICAL: Single immutable state
    private volatile ChatState currentState = new ChatState();
    private final Object stateLock = new Object();
    
    // 🔥 CRITICAL: Single listener (adapter)
    private StateListener stateListener;
    
    public interface StateListener {
        void onStateChanged(ChatState newState);
    }
    
    /**
     * 🔥 REDUCER ACTIONS - Single source of truth updates
     */
    public static class Action {
        public enum Type {
            LOAD_MESSAGES,      // Firebase initial load
            ADD_MESSAGE,        // Optimistic add
            UPDATE_MESSAGE,     // Status update
            REMOVE_MESSAGE,     // Message removal
            CLEAR_ALL           // Clear chat
        }
        
        public final Type type;
        public final ChatMessage message;
        public final List<ChatMessage> messages;
        public final String messageId;
        public final ChatMessage.MessageStatus newStatus;
        
        private Action(Type type, ChatMessage message, List<ChatMessage> messages, 
                       String messageId, ChatMessage.MessageStatus newStatus) {
            this.type = type;
            this.message = message;
            this.messages = messages;
            this.messageId = messageId;
            this.newStatus = newStatus;
        }
        
        // 🔥 ACTION FACTORIES
        public static Action loadMessages(List<ChatMessage> messages) {
            return new Action(Type.LOAD_MESSAGES, null, messages, null, null);
        }
        
        public static Action addMessage(ChatMessage message) {
            return new Action(Type.ADD_MESSAGE, message, null, null, null);
        }
        
        public static Action updateMessage(String messageId, ChatMessage.MessageStatus newStatus) {
            return new Action(Type.UPDATE_MESSAGE, null, null, messageId, newStatus);
        }
        
        public static Action removeMessage(String messageId) {
            return new Action(Type.REMOVE_MESSAGE, null, null, messageId, null);
        }
        
        public static Action clearAll() {
            return new Action(Type.CLEAR_ALL, null, null, null, null);
        }
    }
    
    /**
     * 🔥 CRITICAL: Set single state listener (adapter only)
     */
    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
        // Immediately notify with current state
        if (listener != null) {
            listener.onStateChanged(currentState);
        }
    }
    
    /**
     * 🔥 CRITICAL: Dispatch action through reducer (single entry point)
     */
    public void dispatch(Action action) {
        Log.d(TAG, "🔥 DISPATCH ACTION: " + action.type);
        
        synchronized (stateLock) {
            ChatState newState = reduce(currentState, action);
            
            if (newState != currentState) {
                currentState = newState;
                Log.d(TAG, "✅ STATE REDUCED: " + newState.getMessages().size() + " messages");
                
                // 🔥 CRITICAL: Single listener notification
                if (stateListener != null) {
                    stateListener.onStateChanged(newState);
                }
            }
        }
    }
    
    /**
     * 🔥 REDUCER LOGIC - Pure function, no side effects
     */
    private ChatState reduce(ChatState state, Action action) {
        switch (action.type) {
            case LOAD_MESSAGES:
                return reduceLoadMessages(state, action.messages);
                
            case ADD_MESSAGE:
                return reduceAddMessage(state, action.message);
                
            case UPDATE_MESSAGE:
                return reduceUpdateMessage(state, action.messageId, action.newStatus);
                
            case REMOVE_MESSAGE:
                return reduceRemoveMessage(state, action.messageId);
                
            case CLEAR_ALL:
                return reduceClearAll(state);
                
            default:
                return state;
        }
    }
    
    /**
     * 🔥 REDUCER: Load messages (replace entire list)
     */
    private ChatState reduceLoadMessages(ChatState state, List<ChatMessage> messages) {
        Log.d(TAG, "🔥 REDUCE LOAD_MESSAGES: " + messages.size() + " messages");
        
        List<ChatMessage> sortedMessages = sortMessages(new ArrayList<>(messages));
        return new ChatState(sortedMessages, false, null);
    }
    
    /**
     * 🔥 REDUCER: Add message (with deduplication)
     */
    private ChatState reduceAddMessage(ChatState state, ChatMessage newMessage) {
        Log.d(TAG, "🔥 REDUCE ADD_MESSAGE: " + newMessage.getMessageId());
        
        List<ChatMessage> currentMessages = new ArrayList<>(state.getMessages());
        
        // 🔥 DEDUPLICATION CHECK
        for (ChatMessage existing : currentMessages) {
            if (existing.getMessageId().equals(newMessage.getMessageId())) {
                Log.d(TAG, "❌ Message already exists: " + newMessage.getMessageId());
                return state; // No change
            }
        }
        
        currentMessages.add(newMessage);
        List<ChatMessage> sortedMessages = sortMessages(currentMessages);
        
        return new ChatState(sortedMessages, false, null);
    }
    
    /**
     * 🔥 REDUCER: Update message status
     */
    private ChatState reduceUpdateMessage(ChatState state, String messageId, ChatMessage.MessageStatus newStatus) {
        Log.d(TAG, "🔥 REDUCE UPDATE_MESSAGE: " + messageId + " → " + newStatus);
        
        List<ChatMessage> currentMessages = new ArrayList<>(state.getMessages());
        boolean messageFound = false;
        
        for (int i = 0; i < currentMessages.size(); i++) {
            ChatMessage msg = currentMessages.get(i);
            if (msg.getMessageId().equals(messageId)) {
                // 🔥 CRITICAL: Create new message with updated status
                ChatMessage updatedMessage = createMessageWithNewStatus(msg, newStatus);
                currentMessages.set(i, updatedMessage);
                messageFound = true;
                Log.d(TAG, "✅ Message status updated: " + messageId + " → " + newStatus);
                break;
            }
        }
        
        if (!messageFound) {
            Log.w(TAG, "❌ Message not found for update: " + messageId);
            return state; // No change
        }
        
        return new ChatState(currentMessages, false, null);
    }
    
    /**
     * 🔥 REDUCER: Remove message
     */
    private ChatState reduceRemoveMessage(ChatState state, String messageId) {
        Log.d(TAG, "🔥 REDUCE REMOVE_MESSAGE: " + messageId);
        
        List<ChatMessage> currentMessages = new ArrayList<>(state.getMessages());
        boolean removed = currentMessages.removeIf(msg -> msg.getMessageId().equals(messageId));
        
        if (!removed) {
            Log.w(TAG, "❌ Message not found for removal: " + messageId);
            return state; // No change
        }
        
        return new ChatState(currentMessages, false, null);
    }
    
    /**
     * 🔥 REDUCER: Clear all messages
     */
    private ChatState reduceClearAll(ChatState state) {
        Log.d(TAG, "🔥 REDUCE CLEAR_ALL");
        return new ChatState(Collections.emptyList(), false, null);
    }
    
    /**
     * 🔥 CRITICAL: Create message with new status (immutable copy)
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
        return newMessage;
    }
    
    /**
     * 🔥 CRITICAL: Sort messages by timestamp
     */
    private List<ChatMessage> sortMessages(List<ChatMessage> messages) {
        Collections.sort(messages, new Comparator<ChatMessage>() {
            @Override
            public int compare(ChatMessage a, ChatMessage b) {
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
            }
        });
        return messages;
    }
    
    /**
     * 🔥 CRITICAL: Get current state (for debugging)
     */
    public ChatState getCurrentState() {
        return currentState;
    }
    
    /**
     * 🔥 CRITICAL: Get current messages (for debugging)
     */
    public List<ChatMessage> getCurrentMessages() {
        return new ArrayList<>(currentState.getMessages());
    }
    
    /**
     * 🔥 CRITICAL: Debug state
     */
    public void debugState() {
        Log.d(TAG, "🔥 CURRENT STATE DEBUG:");
        Log.d(TAG, "  - Messages count: " + currentState.getMessages().size());
        Log.d(TAG, "  - Is loading: " + currentState.isLoading());
        Log.d(TAG, "  - Error: " + currentState.getError());
        Log.d(TAG, "  - Last updated: " + currentState.getLastUpdated());
    }
}
