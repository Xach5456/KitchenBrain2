package com.example.kitchenbrain;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private Timestamp timestamp;
    private String messageStatus; // sent, delivered, read
    private String messageType;   // text, image, file

    public ChatMessage() {
        // Default constructor required for Firebase
    }

    public ChatMessage(String messageId, String senderId, String receiverId, String text, Timestamp timestamp, String messageStatus) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.messageStatus = messageStatus;
        this.messageType = "text"; // Default to text
    }

    // Getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getMessageStatus() { return messageStatus; }
    public void setMessageStatus(String messageStatus) { this.messageStatus = messageStatus; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}
