package com.example.kitchenbrain;

/**
 * AI Chat Message Model
 */
public class AIChatMessage {
    private String senderId;
    private String message;
    private String senderType; // "user" or "ai"
    private long timestamp;

    public AIChatMessage() {
        // Required for Firestore
    }

    public AIChatMessage(String senderId, String message, String senderType, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.senderType = senderType;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderType() {
        return senderType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
