package com.example.kitchenbrain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a food product/ingredient in the KitchenBrain app
 * MUST have public no-argument constructor for Firestore deserialization
 */
public class FoodProduct implements Serializable {
    private String id;
    private String name;
    private String category;
    private String emoji;
    private List<String> aliases; // Alternative names for better matching
    
    /**
     * REQUIRED: Public no-argument constructor for Firebase Firestore
     */
    public FoodProduct() {
        this.id = "";
        this.name = "";
        this.category = "";
        this.emoji = "";
        this.aliases = new ArrayList<>();
    }
    
    public FoodProduct(String id, String name, String category, String emoji, String... aliases) {
        this.id = id;
        this.name = name != null ? name.toLowerCase() : "";
        this.category = category;
        this.emoji = emoji;
        this.aliases = aliases.length > 0 ? Arrays.asList(aliases) : new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public List<String> getAliases() {
        return aliases;
    }
    
    /**
     * Check if this product matches a search query
     */
    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Check main name
        if (name.contains(lowerQuery)) {
            return true;
        }
        
        // Check aliases
        for (String alias : aliases) {
            if (alias.toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }
        
        // Check category
        if (category.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get display name with emoji
     */
    public String getDisplayName() {
        return emoji + " " + capitalize(name);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
