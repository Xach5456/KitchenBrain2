package com.example.kitchenbrain.state;

import com.example.kitchenbrain.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🔥 TELEGRAM-LEVEL IMMUTABLE CHAT STATE
 * Features: Single source of truth + immutable snapshots + no race conditions
 */
public class ChatState {
    
    private final List<ChatMessage> messages;
    private final long lastUpdated;
    private final boolean isLoading;
    private final String error;
    
    // 🔥 TELEGRAM-LEVEL: Empty state constructor
    public ChatState() {
        this.messages = Collections.emptyList();
        this.lastUpdated = System.currentTimeMillis();
        this.isLoading = false;
        this.error = null;
    }
    
    // 🔥 TELEGRAM-LEVEL: Full state constructor
    public ChatState(List<ChatMessage> messages, boolean isLoading, String error) {
        this.messages = messages != null ? 
                Collections.unmodifiableList(new ArrayList<>(messages)) : 
                Collections.emptyList();
        this.lastUpdated = System.currentTimeMillis();
        this.isLoading = isLoading;
        this.error = error;
    }
    
    // 🔥 TELEGRAM-LEVEL: Copy builder for state updates
    public ChatState withMessages(List<ChatMessage> newMessages) {
        return new ChatState(newMessages, this.isLoading, this.error);
    }
    
    public ChatState withLoading(boolean loading) {
        return new ChatState(this.messages, loading, this.error);
    }
    
    public ChatState withError(String error) {
        return new ChatState(this.messages, this.isLoading, error);
    }
    
    // 🔥 TELEGRAM-LEVEL: Immutable getters
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public boolean isLoading() {
        return isLoading;
    }
    
    public String getError() {
        return error;
    }
    
    public boolean hasError() {
        return error != null;
    }
    
    public boolean isEmpty() {
        return messages.isEmpty();
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    @Override
    public String toString() {
        return "ChatState{" +
                "messageCount=" + messages.size() +
                ", lastUpdated=" + lastUpdated +
                ", isLoading=" + isLoading +
                ", hasError=" + hasError() +
                '}';
    }
}
