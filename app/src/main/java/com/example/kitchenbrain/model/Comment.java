package com.example.kitchenbrain.model;

/**
 * Comment - Instagram-style Comment Model
 * 
 * ✅ INSTAGRAM ARCHITECTURE:
 * Real-time comments with user info and timestamps
 */
public class Comment {
    
    public String commentId;
    public String userId;
    public String userName;
    public String userAvatar;
    public String text;
    public long timestamp;
    
    // Firestore requirement
    public Comment() {}
    
    public Comment(String userId, String userName, String userAvatar, String text, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.text = text;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getCommentId() { return commentId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    
    // Setters (for Firestore)
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
