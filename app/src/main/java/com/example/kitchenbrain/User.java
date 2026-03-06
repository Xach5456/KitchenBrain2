package com.example.kitchenbrain;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String username;
    private String email;
    private String avatarUrl;
    private Timestamp createdAt;
    private int recipesCount;
    private List<String> followers;
    private List<String> following;
    private Boolean isOnline;
    private Object lastSeen; // Can be Long (from DB) or Timestamp (for flexibility)
    private Boolean isGuest;
    
    // Fields for Firestore mapping compatibility
    private Boolean online;
    private java.util.Date lastSeenAsDate;
    private com.google.firebase.Timestamp lastSeenAsTimestamp;

    // Default constructor required for Firestore
    public User() {
        this.isOnline = false;
        this.lastSeen = Timestamp.now().toDate().getTime(); // Store as Long milliseconds
        this.online = false; // Default value
        this.lastSeenAsDate = null;
        this.lastSeenAsTimestamp = null;
    }

    // Constructor with username
    public User(String username) {
        this.username = username;
        this.createdAt = Timestamp.now();
        this.recipesCount = 0;
        this.isOnline = false;
        this.lastSeen = Timestamp.now().toDate().getTime(); // Store as Long milliseconds
        this.online = false; // Default value
        this.lastSeenAsDate = null;
        this.lastSeenAsTimestamp = null;
    }

    // Full constructor
    public User(String username, String email, String avatarUrl, Timestamp createdAt, int recipesCount) {
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.recipesCount = recipesCount;
        this.isOnline = false;
        this.lastSeen = Timestamp.now().toDate().getTime(); // Store as Long milliseconds
        this.online = false; // Default value
        this.lastSeenAsDate = null;
        this.lastSeenAsTimestamp = null;
    }


    private List<String> hiddenRecipes;

    public List<String> getHiddenRecipes() {
        return hiddenRecipes;
    }

    public void setHiddenRecipes(List<String> hiddenRecipes) {
        this.hiddenRecipes = hiddenRecipes;
    }


    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getRecipesCount() {
        return recipesCount;
    }

    public void setRecipesCount(int recipesCount) {
        this.recipesCount = recipesCount;
    }

    public List<String> getFollowers() {
        if (followers == null) followers = new ArrayList<>();
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        if (following == null) following = new ArrayList<>();
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

    public Boolean getIsOnline() {
        if (isOnline != null) {
            return isOnline;
        }
        // Fallback to the 'online' field for compatibility
        return online;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Object getLastSeen() {
        return lastSeen;
    }
    
    public Boolean getIsGuest() {
        return isGuest;
    }
    
    public void setIsGuest(Boolean isGuest) {
        this.isGuest = isGuest;
    }
    
    // Convenience method to get last seen as Timestamp
    public com.google.firebase.Timestamp getLastSeenAsTimestamp() {
        if (lastSeen instanceof Long) {
            // Convert Long (milliseconds) to Timestamp
            Long millis = (Long) lastSeen;
            return new com.google.firebase.Timestamp(millis / 1000, (int) ((millis % 1000) * 1000000));
        } else if (lastSeen instanceof com.google.firebase.Timestamp) {
            return (com.google.firebase.Timestamp) lastSeen;
        } else if (lastSeen instanceof java.util.Date) {
            java.util.Date date = (java.util.Date) lastSeen;
            return new com.google.firebase.Timestamp(date.getTime() / 1000, (int) ((date.getTime() % 1000) * 1000000));
        }
        return null;
    }
    
    // Convenience method to get last seen as Date
    public java.util.Date getLastSeenAsDate() {
        if (lastSeen instanceof Long) {
            return new java.util.Date((Long) lastSeen);
        } else if (lastSeen instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) lastSeen).toDate();
        } else if (lastSeen instanceof java.util.Date) {
            return (java.util.Date) lastSeen;
        }
        return null;
    }

    public void setLastSeen(Object lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    // Convenience method to set last seen from Timestamp
    public void setLastSeenFromTimestamp(com.google.firebase.Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    // Convenience method to set last seen from Long
    public void setLastSeenFromLong(Long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    // Convenience method to set last seen from Date
    public void setLastSeenFromDate(java.util.Date lastSeen) {
        if (lastSeen != null) {
            this.lastSeen = lastSeen.getTime();
        } else {
            this.lastSeen = null;
        }
    }
    
    // Getters and setters for the additional fields to resolve Firestore warnings
    
    public Boolean getOnline() {
        return online;
    }
    
    public void setOnline(Boolean online) {
        this.online = online;
    }
    
    // Add setters for fields that exist in Firestore to prevent warnings
    public void setLastSeenAsDate(java.util.Date lastSeenAsDate) {
        // Convert Date to our internal format (Long milliseconds)
        if (lastSeenAsDate != null) {
            this.lastSeen = lastSeenAsDate.getTime();
        } else {
            this.lastSeen = null;
        }
    }
    
    public void setLastSeenAsTimestamp(com.google.firebase.Timestamp lastSeenAsTimestamp) {
        // Convert Timestamp to our internal format (Long milliseconds)
        if (lastSeenAsTimestamp != null) {
            this.lastSeen = lastSeenAsTimestamp.toDate().getTime();
        } else {
            this.lastSeen = null;
        }
    }
}