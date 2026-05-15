package com.example.kitchenbrain;

/**
 * Message Status Enum - Type-safe status tracking for chat messages
 * 
 * Lifecycle: SENDING → SENT → DELIVERED → READ
 */
public enum MessageStatus {
    /** Message is being prepared/sent (local only) */
    SENDING("sending"),
    
    /** Message successfully saved to Firestore */
    SENT("sent"),
    
    /** Message delivered to recipient's device (acknowledged by their client) */
    DELIVERED("delivered"),
    
    /** Recipient has opened the chat and seen the message */
    READ("read"),
    
    /** Message failed to send (will be retried) */
    FAILED("failed");
    
    private final String value;
    
    MessageStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Convert string value to MessageStatus enum
     */
    public static MessageStatus fromString(String value) {
        if (value == null) return SENDING;
        
        for (MessageStatus status : MessageStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return SENDING; // Default fallback
    }
    
    /**
     * Check if this is a terminal state (no further updates expected)
     */
    public boolean isTerminal() {
        return this == READ || this == FAILED;
    }
    
    /**
     * Check if message can be retried
     */
    public boolean canRetry() {
        return this == FAILED || this == SENDING;
    }
}
