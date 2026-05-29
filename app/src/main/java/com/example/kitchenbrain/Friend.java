package com.example.kitchenbrain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a friend relationship in the system
 */
public class Friend {
    @DocumentId
    private String id;
    private String userId;
    private String friendId;
    private String friendUsername;
    private String friendAvatarUrl;
    private boolean isOnline;
    private Timestamp lastActive;
    private Timestamp createdAt;

    public Friend() {
        // Required empty constructor for Firestore
    }

    public Friend(String userId, String friendId, String friendUsername, String friendAvatarUrl) {
        this.userId = userId;
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.friendAvatarUrl = friendAvatarUrl;
        this.isOnline = false;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }

    public String getFriendAvatarUrl() {
        return friendAvatarUrl;
    }

    public void setFriendAvatarUrl(String friendAvatarUrl) {
        this.friendAvatarUrl = friendAvatarUrl;
    }

    @PropertyName("isOnline")
    public boolean getIsOnline() {
        return isOnline;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
    
    @PropertyName("isOnline")
    public void setIsOnline(boolean online) {
        this.isOnline = online;
    }

    public Timestamp getLastActive() {
        return lastActive;
    }

    public void setLastActive(Timestamp lastActive) {
        this.lastActive = lastActive;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
