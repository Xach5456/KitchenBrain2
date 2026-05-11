package com.example.kitchenbrain;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String username;
    private String usernameLower;  // 🔥 NEW: For case-insensitive search
    private String email;
    private String avatarUrl;
    private Timestamp createdAt;
    private int recipesCount;
    private int followersCount;  // ✅ NEW: Follower count
    private int followingCount;  // ✅ NEW: Following count
    private List<String> followers;
    private List<String> following;
    private Boolean isOnline;
    private Object lastSeen; // Can be Long (from DB) or Timestamp (for flexibility)
    private Boolean isGuest;
    private Boolean messagingEnabled;
    
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
        this.messagingEnabled = true; // Default: messaging enabled
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
        this.messagingEnabled = true; // Default: messaging enabled
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
        this.messagingEnabled = true; // Default: messaging enabled
    }


    private List<String> hiddenRecipes;

    public List<String> getHiddenRecipes() {
        return hiddenRecipes;
    }

    public void setHiddenRecipes(List<String> hiddenRecipes) {
        this.hiddenRecipes = hiddenRecipes;
    }

    // Document ID field for Firestore compatibility
    private String id;

    public String getId() {
        return id != null ? id : userId;
    }

    public void setId(String id) {
        this.id = id;
        // 🔥 CRITICAL FIX: Ensure userId is synced with Firestore document ID
        if (this.userId == null) {
            this.userId = id;
        }
    }

    public String getDisplayId() {
        return id != null ? id : userId;
    }


    // Getters and Setters
    public String getUserId() {
        return userId != null ? userId : id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (this.id == null) {
            this.id = userId;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        // 🔥 AUTO-UPDATE: Set usernameLower when username changes
        if (username != null) {
            this.usernameLower = username.toLowerCase().trim();
        }
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

    // ✅ NEW: Getters and Setters for follower/following counts
    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
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
    
    public Boolean getMessagingEnabled() {
        return messagingEnabled;
    }
    
    public void setMessagingEnabled(Boolean messagingEnabled) {
        this.messagingEnabled = messagingEnabled;
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
    
    // 🔥 NEW: usernameLower getters/setters
    public String getUsernameLower() {
        return usernameLower;
    }
    
    public void setUsernameLower(String usernameLower) {
        this.usernameLower = usernameLower;
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