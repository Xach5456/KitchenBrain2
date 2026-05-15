package com.example.kitchenbrain;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ PRODUCTION-READY User Model
 * Fully compatible with Firestore deserialization and Social Graph system.
 */
@Keep
@IgnoreExtraProperties
public class User {
    @DocumentId
    private String id;
    private String userId;
    private String username;
    private String usernameLower;
    private String nickname;
    private String email;
    private String avatarUrl;
    private Timestamp createdAt;
    private int recipesCount;
    private int followersCount;
    private int followingCount;
    private List<String> followers;
    private List<String> following;
    private Boolean isOnline;
    private Object lastSeen;
    private Boolean isGuest;
    private Boolean messagingEnabled;
    
    // Firestore mapping compatibility fields
    private Boolean online;
    private java.util.Date lastSeenAsDate;
    private com.google.firebase.Timestamp lastSeenAsTimestamp;
    private List<String> hiddenRecipes;

    public User() {
        this.isOnline = false;
        this.online = false;
        this.messagingEnabled = true;
        this.followersCount = 0;
        this.followingCount = 0;
        this.recipesCount = 0;
    }

    public User(String username) {
        this();
        this.username = username;
        this.createdAt = Timestamp.now();
    }

    public String getId() {
        return id != null ? id : userId;
    }

    public void setId(String id) {
        this.id = id;
        if (this.userId == null) {
            this.userId = id;
        }
    }

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
        if (username != null) {
            this.usernameLower = username.toLowerCase().trim();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
        return isOnline != null ? isOnline : online;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
        this.online = isOnline;
    }

    public Object getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Object lastSeen) {
        this.lastSeen = lastSeen;
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

    public List<String> getHiddenRecipes() {
        return hiddenRecipes;
    }

    public void setHiddenRecipes(List<String> hiddenRecipes) {
        this.hiddenRecipes = hiddenRecipes;
    }

    // --- Legacy / Firestore Warning Resolvers ---

    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; this.isOnline = online; }

    public void setLastSeenAsDate(java.util.Date lastSeenAsDate) {
        if (lastSeenAsDate != null) this.lastSeen = lastSeenAsDate.getTime();
    }
    
    public void setLastSeenAsTimestamp(com.google.firebase.Timestamp lastSeenAsTimestamp) {
        if (lastSeenAsTimestamp != null) this.lastSeen = lastSeenAsTimestamp.toDate().getTime();
    }

    public String getUsernameLower() { return usernameLower; }
    public void setUsernameLower(String usernameLower) { this.usernameLower = usernameLower; }
}
