package com.example.kitchenbrain;

/**
 * Represents a guest user session in the app
 */
public class GuestUser {
    private static final String GUEST_USER_ID = "guest_user_" + System.currentTimeMillis();
    private static final String GUEST_USERNAME = "Guest User";
    
    private String userId;
    private String username;
    private boolean isGuest;
    
    public GuestUser() {
        this.userId = GUEST_USER_ID;
        this.username = GUEST_USERNAME;
        this.isGuest = true;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public boolean isGuest() {
        return isGuest;
    }
}