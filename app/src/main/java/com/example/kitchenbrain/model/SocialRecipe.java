package com.example.kitchenbrain.model;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SocialRecipe - Unified model for recipe creation and storage.
 * 
 * Fields are public for direct Kotlin access in FeedRankingAlgorithm,
 * while methods are maintained for Java compatibility.
 */
public class SocialRecipe {
    
    @PropertyName("id")
    public String id;
    public String authorId;
    public String username;   
    public String name;       
    public String description;
    public String imageUrl;
    public String videoUrl;
    public List<String> ingredients;
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
    
    public SocialRecipe() {}
    
    public SocialRecipe(String authorId, String username, String name, String description) {
        this.authorId = authorId;
        this.username = username;
        this.name = name;
        this.description = description;
        this.likes = 0;
        this.isDraft = false;
        this.socialPriority = "global";
    }

    // --- ID Accessors ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    @PropertyName("recipeId")
    public String getRecipeId() { return id; }
    @PropertyName("recipeId")
    public void setRecipeId(String id) { this.id = id; }

    // --- Author Accessors ---
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    @PropertyName("authorName")
    public String getAuthorName() { return username; }
    @PropertyName("authorName")
    public void setAuthorName(String authorName) { this.username = authorName; }

    // --- Content Accessors ---
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
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    
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

    // --- Metadata Accessors ---
    @PropertyName("cookingTime")
    public long getCookingTime() { return cookingTime; }
    @PropertyName("cookingTime")
    public void setCookingTime(long cookingTime) { this.cookingTime = cookingTime; }
    
    @PropertyName("cookTime")
    public long getCookTime() { return cookingTime; }
    @PropertyName("cookTime")
    public void setCookTime(long cookTime) { this.cookingTime = cookTime; }

    public String getFormattedCookTime() {
        if (cookingTime == 0) return "Time unknown";
        if (cookingTime < 60) return cookingTime + " min";
        long hours = cookingTime / 60;
        long minutes = cookingTime % 60;
        if (minutes == 0) return hours + " hr";
        return hours + " hr " + minutes + " min";
    }

    public String getDifficulty() { return difficulty != null ? difficulty : "Medium"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    
    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public boolean isDraft() { return isDraft; }
    public void setDraft(boolean draft) { isDraft = draft; }

    // --- Social Metrics Accessors ---
    public long getLikes() { return likes; }
    public void setLikes(long likes) { this.likes = likes; }
    
    public long getComments() { return comments; }
    public void setComments(long comments) { this.comments = comments; }
    
    public long getSaves() { return saves; }
    public void setSaves(long saves) { this.saves = saves; }
    
    public Map<String, Boolean> getLikedBy() { return likedBy; }
    public void setLikedBy(Map<String, Boolean> likedBy) { this.likedBy = likedBy; }
    
    public Map<String, Boolean> getSavedBy() { return savedBy; }
    public void setSavedBy(Map<String, Boolean> savedBy) { this.savedBy = savedBy; }

    // --- Social Priority Accessors ---
    public boolean isFromMutualFollower() { return isFromMutualFollower; }
    public void setFromMutualFollower(boolean fromMutualFollower) { isFromMutualFollower = fromMutualFollower; }
    
    public boolean isFromFollower() { return isFromFollower; }
    public void setFromFollower(boolean fromFollower) { isFromFollower = fromFollower; }
    
    public String getSocialPriority() { return socialPriority; }
    public void setSocialPriority(String socialPriority) { this.socialPriority = socialPriority; }

    // --- Timestamps Accessors ---
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public void setTimestamp(long timestamp) {
        this.createdAt = new Date(timestamp);
    }
}
