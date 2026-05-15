package com.example.kitchenbrain.model;

import com.example.kitchenbrain.User;

/**
 * ✅ INSTAGRAM PATTERN: Unified list item type
 * 
 * NEVER mix UI state with data models.
 * This separates:
 * - Real User objects (from cache/database)
 * - Loading placeholders (UI state only)
 */
public abstract class ListItem {
    
    public enum ItemType {
        USER,      // Real user with full data
        LOADING    // Placeholder while loading
    }
    
    public abstract ItemType getType();
    
    /**
     * Real user item
     */
    public static class UserItem extends ListItem {
        private final User user;
        
        public UserItem(User user) {
            this.user = user;
        }
        
        public User getUser() {
            return user;
        }
        
        @Override
        public ItemType getType() {
            return ItemType.USER;
        }
    }
    
    /**
     * Loading placeholder item
     */
    public static class LoadingItem extends ListItem {
        private final String userId;
        
        public LoadingItem(String userId) {
            this.userId = userId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        @Override
        public ItemType getType() {
            return ItemType.LOADING;
        }
    }
}
