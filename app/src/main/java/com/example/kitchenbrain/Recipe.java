package com.example.kitchenbrain;

import java.io.Serializable;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private String description;
    private int cookingTime;  // Changed to int to match Firestore NUMBER
    private String cookingInstructions;
    private String imageUrl;   // Renamed from image to match Firestore
    private String videoUrl;   // Renamed from video to match Firestore
    private String authorId;   // New field for recipe ownership
    private String username;   // New field for display name
    private Object createdAt;    // Can be Date, Timestamp, or String - to handle various formats from Firestore
    private int likesCount;    // New field for engagement
    private List<String> ingredients; // List of ingredients for the recipe

    // Default constructor required for Firebase
    public Recipe() {
        this.ingredients = new ArrayList<>();
    }

    // Full constructor
    public Recipe(String id, String name, String description, int cookingTime, 
                  String cookingInstructions, String imageUrl, String videoUrl,
                  String authorId, String username, Object createdAt, int likesCount, List<String> ingredients) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cookingTime = cookingTime;
        this.cookingInstructions = cookingInstructions;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.authorId = authorId;
        this.username = username;
        this.createdAt = createdAt;
        this.likesCount = likesCount;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    // Геттеры и сеттеры для всех полей
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(int cookingTime) {
        this.cookingTime = cookingTime;
    }

    public String getCookingInstructions() {
        return cookingInstructions;
    }

    public void setCookingInstructions(String cookingInstructions) {
        this.cookingInstructions = cookingInstructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getCreatedAt() {
        if (createdAt instanceof Date) {
            return (Date) createdAt;
        } else if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate();
        } else if (createdAt instanceof String) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return sdf.parse((String) createdAt);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }
    
    // Convenience method to accept Date
    public void setCreatedAtFromDate(Date date) {
        this.createdAt = date;
    }
    
    // Convenience method to accept Timestamp
    public void setCreatedAtFromTimestamp(Timestamp timestamp) {
        this.createdAt = timestamp;
    }
    
    // Convenience method to accept String
    public void setCreatedAtFromString(String dateString) {
        this.createdAt = dateString;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public List<String> getIngredients() {
        if (ingredients == null) {
            ingredients = new ArrayList<>();
        }
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }
    
    // Method to add an ingredient to the recipe
    public void addIngredient(String ingredient) {
        if (ingredients == null) {
            ingredients = new ArrayList<>();
        }
        if (ingredient != null && !ingredient.trim().isEmpty()) {
            ingredients.add(ingredient.trim());
        }
    }
    
    // Method to check if recipe contains specific ingredients
    public boolean containsIngredients(List<String> requiredIngredients) {
        if (requiredIngredients == null || requiredIngredients.isEmpty()) {
            return true; // If no ingredients specified, return all recipes
        }
        
        if (ingredients == null || ingredients.isEmpty()) {
            return false; // Recipe has no ingredients
        }
        
        // Check if all required ingredients are present in the recipe
        for (String required : requiredIngredients) {
            boolean found = false;
            for (String recipeIngredient : ingredients) {
                if (recipeIngredient.toLowerCase().contains(required.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false; // Missing at least one required ingredient
            }
        }
        return true; // All required ingredients found
    }
}