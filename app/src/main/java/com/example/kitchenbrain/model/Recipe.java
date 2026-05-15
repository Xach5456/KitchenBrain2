package com.example.kitchenbrain.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recipe model class for Firestore
 * MUST have public no-argument constructor for Firestore deserialization
 */
@Keep
@IgnoreExtraProperties
public class Recipe implements Serializable {
    private String id;
    private String title;
    private String description;
    private String category;
    private int imageResId;
    private double rating;
    // Time fields - USE LONG for Firestore compatibility
    private long cookingTime;        // Changed from String to long
    private long prepTime;           // Added missing field
    private String difficulty;
    private Map<String, Object> ingredientIds; // For Firestore arrayContains queries
    private long createdAt;
    private String createdBy;
    private int servings;
    private List<String> instructions;
    
    // Additional fields for modern chat system
    private List<String> ingredients;
    private String authorId;
    private String imageUrl;
    private String cookingInstructions;
    private Map<String, Object> metadata;
    private int likesCount;  // Added for engagement tracking
    
    // Additional Firestore compatibility fields (may exist in some documents)
    private String videoUrl;      // Optional video URL
    private String name;          // Alternative name field (legacy)
    private String username;      // Author username (legacy)

    /**
     * REQUIRED: Public no-argument constructor for Firebase Firestore
     * Firestore uses reflection to create instances before setting fields
     */
    public Recipe() {
        this.id = "";
        this.title = "";
        this.description = "";
        this.category = "";
        this.imageResId = 0;
        this.rating = 0.0;
        this.cookingTime = 0L;          // Default to 0 (Long)
        this.prepTime = 0L;             // Default to 0 (Long)
        this.difficulty = "Easy";
        this.servings = 1;
        this.instructions = new ArrayList<>();
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new HashMap<>(); // Use Map for Firestore arrayContains
        this.authorId = "";
        this.createdBy = "";
        this.createdAt = System.currentTimeMillis();
        this.imageUrl = "";
        this.cookingInstructions = "";
        this.metadata = new HashMap<>();
        this.likesCount = 0;
        this.videoUrl = null;      // Optional
        this.name = null;          // Legacy field
        this.username = null;      // Legacy field
    }

    /**
     * Full constructor with all parameters
     */
    public Recipe(String id, String title, String description, String category, 
                  int imageResId, double rating, long cookingTime, String difficulty) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.imageResId = imageResId;
        this.rating = rating;
        this.cookingTime = cookingTime;      // Now accepts long
        this.difficulty = difficulty;
        this.prepTime = 0L;
        this.servings = 1;
        this.instructions = new ArrayList<>();
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new HashMap<>();
        this.authorId = "";
        this.createdBy = "";
        this.createdAt = System.currentTimeMillis();
        this.imageUrl = "";
        this.cookingInstructions = "";
        this.metadata = new HashMap<>();
        this.likesCount = 0;
    }

    /**
     * Constructor with ingredient IDs for Firestore queries
     */
    public Recipe(String id, String title, String description, String category, 
                  int imageResId, double rating, long cookingTime, String difficulty,
                  Map<String, Object> ingredientIds) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.imageResId = imageResId;
        this.rating = rating;
        this.cookingTime = cookingTime;      // Now accepts long
        this.difficulty = difficulty;
        this.prepTime = 0L;
        this.servings = 1;
        this.instructions = new ArrayList<>();
        this.ingredients = new ArrayList<>();
        this.ingredientIds = ingredientIds != null ? ingredientIds : new HashMap<>();
        this.authorId = "";
        this.createdBy = "";
        this.createdAt = System.currentTimeMillis();
        this.imageUrl = "";
        this.cookingInstructions = "";
        this.metadata = new HashMap<>();
        this.likesCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    // COOKING TIME - Handle both Long and String for backwards compatibility
    public long getCookingTime() { return cookingTime; }
    public void setCookingTime(long cookingTime) { this.cookingTime = cookingTime; }
    
    // Helper method to handle String input
    public void setCookingTimeFromString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            this.cookingTime = 0L;
        } else {
            try {
                this.cookingTime = Long.parseLong(timeStr);
            } catch (NumberFormatException e) {
                this.cookingTime = 0L;
            }
        }
    }

    public long getPrepTime() { return prepTime; }
    public void setPrepTime(long prepTime) { this.prepTime = prepTime; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getIngredientIds() { 
        if (ingredientIds instanceof Map) {
            Set<String> keys = ((Map<String, Object>) ingredientIds).keySet();
            return new ArrayList<>(keys);
        }
        return new ArrayList<>();
    }
    public void setIngredientIds(List<String> ingredientIds) { 
        Map<String, Object> ingredientMap = new HashMap<>();
        if (ingredientIds != null) {
            for (String ingredient : ingredientIds) {
                ingredientMap.put(ingredient, true);
            }
        }
        this.ingredientIds = ingredientMap;
    }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public long getCreatedAt() { return createdAt; }
    
    // SINGLE unified setter for createdAt - handles all types safely
    @SuppressWarnings("unused")
    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof Long) {
            this.createdAt = (Long) createdAt;
        } else if (createdAt instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof java.util.Date) {
            this.createdAt = ((java.util.Date) createdAt).getTime();
        } else {
            this.createdAt = System.currentTimeMillis();
        }
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCookingInstructions() { return cookingInstructions; }
    public void setCookingInstructions(String cookingInstructions) { 
        this.cookingInstructions = cookingInstructions; 
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Additional Firestore compatibility getters/setters
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}
