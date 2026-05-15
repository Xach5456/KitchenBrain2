package com.example.kitchenbrain.model;

import com.google.firebase.Timestamp;

public class ChatUser {
    private String userId;
    private String displayName;
    private String username;
    private String email;
    private String photoUrl;
    private String avatarUrl;
    private boolean isOnline;
    private Timestamp lastSeen;
    private Timestamp lastMessageTimestamp;
    private String statusMessage;
    private String lastMessage; // ✅ NEW: Last message preview

    public ChatUser() {
    }

    public ChatUser(String userId, String displayName, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.isOnline = false;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }

    public Timestamp getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    
    // ✅ NEW: Last message getter/setter
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getDisplayStatus() {
        if (isOnline) return "Online";
        if (lastSeen != null) {
            long diff = System.currentTimeMillis() - lastSeen.toDate().getTime();
            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + "m ago";
            if (diff < 86400000) return (diff / 3600000) + "h ago";
            return "Yesterday";
        }
        return "Offline";
    }
}
