package com.example.kitchenbrain.model;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SocialRecipe - Unified model for recipe creation and storage.
 */
public class SocialRecipe implements Serializable {
    
    @PropertyName("id")
    public String id;
    public String authorId;
    public String username;   
    public String name;       
    public String description;
    public String notes;
    public String imageUrl;
    public String videoUrl;
    public List<String> ingredients;
    public List<String> ingredientIds; 
    public List<String> steps;
    public String cookingInstructions; 
    public long cookingTime;   
    
    public String difficulty; 
    public int calories;
    public List<String> tags; 
    public int servings;
    public boolean isDraft;
    
    public long likes;
    public long comments;
    public long saves;
    public Map<String, Boolean> likedBy; 
    public Map<String, Boolean> savedBy; 
    
    public boolean isFromMutualFollower;
    public boolean isFromFollower;
    
    @ServerTimestamp
    public Date createdAt;
    @ServerTimestamp
    public Date updatedAt;
    
    public String socialPriority; 
    
    public SocialRecipe() {
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new ArrayList<>();
        this.steps = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.likedBy = new HashMap<>();
        this.savedBy = new HashMap<>();
    }
    
    public SocialRecipe(String authorId, String username, String name, String description) {
        this();
        this.authorId = authorId;
        this.username = username;
        this.name = name;
        this.description = description;
        this.likes = 0;
        this.isDraft = false;
        this.socialPriority = "global";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    @PropertyName("recipeId")
    public String getRecipeId() { return id; }
    @PropertyName("recipeId")
    public void setRecipeId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    @PropertyName("authorName")
    public String getAuthorName() { return username; }
    @PropertyName("authorName")
    public void setAuthorName(String authorName) { this.username = authorName; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }
    
    @PropertyName("title")
    public String getTitle() { return name; }
    @PropertyName("title")
    public void setTitle(String title) { this.name = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    @PropertyName("ingredientIds")
    public List<String> getIngredientIds() { return ingredientIds; }
    @PropertyName("ingredientIds")
    public void setIngredientIds(List<String> ingredientIds) { this.ingredientIds = ingredientIds; }
    
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { 
        this.steps = steps;
        if (steps != null && !steps.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            this.cookingInstructions = sb.toString();
        }
    }

    public String getCookingInstructions() { return cookingInstructions; }
    public void setCookingInstructions(String cookingInstructions) { this.cookingInstructions = cookingInstructions; }

    @PropertyName("cookingTime")
    public long getCookingTime() { return cookingTime; }
    @PropertyName("cookingTime")
    public void setCookingTime(long cookingTime) { this.cookingTime = cookingTime; }
    
    @PropertyName("cookTime")
    public long getCookTime() { return cookingTime; }
    @PropertyName("cookTime")
    public void setCookTime(long cookTime) { this.cookingTime = cookTime; }

    public String getFormattedCookTime() {
        return cookingTime > 0 ? cookingTime + " min" : "--";
    }

    @PropertyName("likes")
    public long getLikes() { return likes; }
    @PropertyName("likes")
    public void setLikes(long likes) { this.likes = likes; }
    
    @PropertyName("likesCount")
    public void setLikesCount(long likes) { this.likes = likes; }

    @PropertyName("likedBy")
    public Map<String, Boolean> getLikedBy() { return likedBy; }
    
    @PropertyName("likedBy")
    public void setLikedBy(Object value) { 
        if (value instanceof Map) {
            this.likedBy = (Map<String, Boolean>) value;
        } else if (value instanceof List) {
            this.likedBy = new HashMap<>();
            for (Object id : (List) value) {
                if (id != null) this.likedBy.put(id.toString(), true);
            }
        }
    }
    
    @PropertyName("savedBy")
    public Map<String, Boolean> getSavedBy() { return savedBy; }
    
    @PropertyName("savedBy")
    public void setSavedBy(Object value) { 
        if (value instanceof Map) {
            this.savedBy = (Map<String, Boolean>) value;
        } else if (value instanceof List) {
            this.savedBy = new HashMap<>();
            for (Object id : (List) value) {
                if (id != null) this.savedBy.put(id.toString(), true);
            }
        }
    }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
    
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getDifficulty() { return difficulty != null ? difficulty : "Medium"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public void setTimestamp(long timestamp) {
        this.createdAt = new Date(timestamp);
    }
}
