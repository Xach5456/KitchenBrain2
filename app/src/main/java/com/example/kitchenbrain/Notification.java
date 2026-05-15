package com.example.kitchenbrain;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification model for follow and friend request notifications
 */
public class Notification {
    private String id;
    private String receiverId;
    private String senderId;
    private String senderUsername;
    private String senderNickname; // 🔥 NEW: Nickname of the sender
    private String senderAvatarUrl;
    private String type;
    private String message;
    private boolean isRead;
    private long createdAt;

    public Notification() {
        // Required empty constructor for Firestore
    }

    public Notification(String receiverId, String senderId, String senderUsername, 
                       String senderAvatarUrl, String type, String message) {
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatarUrl = senderAvatarUrl;
        this.type = type;
        this.message = message;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    // Explicit getter/setter for Firestore to avoid warnings
    @PropertyName("isRead")
    public boolean getIsRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("receiverId", receiverId);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("senderNickname", senderNickname != null ? senderNickname : "");
        map.put("senderAvatarUrl", senderAvatarUrl != null ? senderAvatarUrl : "");
        map.put("type", type);
        map.put("message", message);
        map.put("isRead", isRead);
        map.put("createdAt", createdAt);
        return map;
    }
}
