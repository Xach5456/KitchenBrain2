package com.example.kitchenbrain.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recipe model class for Firestore
 * Optimized to handle all field types and prevent deserialization crashes.
 * Consistent with the root Recipe model.
 */
@Keep
@IgnoreExtraProperties
public class Recipe implements Serializable {
    @Exclude private String id;
    @Exclude private String title;
    @Exclude private String description;
    @Exclude private String category;
    @Exclude private int imageResId;
    @Exclude private double rating;
    @Exclude private long cookingTime;
    @Exclude private long prepTime;
    @Exclude private String difficulty;
    @Exclude private List<String> ingredientIds;
    @Exclude private Map<String, Object> ingredientIdsMap;
    @Exclude private long createdAtLong;
    @Exclude private String createdBy;
    @Exclude private int servings;
    @Exclude private List<String> instructions;
    
    @Exclude private List<String> ingredients;
    @Exclude private String authorId;
    @Exclude private String imageUrl;
    @Exclude private String cookingInstructions;
    @Exclude private Map<String, Object> metadata;
    @Exclude private int likesCount;
    @Exclude private int likes;
    @Exclude private List<String> likedBy;
    @Exclude private int saves;
    @Exclude private List<String> savedBy;
    @Exclude private int comments;
    @Exclude private int socialPriority;
    
    @Exclude private String videoUrl;
    @Exclude private String name;
    @Exclude private String username;
    @Exclude private long updatedAt;
    
    @Exclude private boolean isDraft;
    @Exclude private boolean draft;

    public Recipe() {
        this.instructions = new ArrayList<>();
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new ArrayList<>();
        this.ingredientIdsMap = new HashMap<>();
        this.metadata = new HashMap<>();
        this.likedBy = new ArrayList<>();
        this.savedBy = new ArrayList<>();
        this.difficulty = "Easy";
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("title")
    public String getTitle() { return title != null ? title : name; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("category")
    public String getCategory() { return category; }
    @PropertyName("category")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("cookingTime")
    public long getCookingTime() { return cookingTime; }
    @PropertyName("cookingTime")
    public void setCookingTime(Object value) { this.cookingTime = convertToLong(value); }

    @PropertyName("prepTime")
    public long getPrepTime() { return prepTime; }
    @PropertyName("prepTime")
    public void setPrepTime(Object value) { this.prepTime = convertToLong(value); }

    @PropertyName("difficulty")
    public String getDifficulty() { return difficulty; }
    @PropertyName("difficulty")
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    @PropertyName("servings")
    public int getServings() { return servings; }
    @PropertyName("servings")
    public void setServings(Object value) { this.servings = convertToInt(value); }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAtLong; }
    @PropertyName("createdAt")
    public void setCreatedAt(Object value) { this.createdAtLong = convertToLong(value); }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }
    @PropertyName("updatedAt")
    public void setUpdatedAt(Object value) { this.updatedAt = convertToLong(value); }

    @PropertyName("socialPriority")
    public int getSocialPriority() { return socialPriority; }
    @PropertyName("socialPriority")
    public void setSocialPriority(Object value) { this.socialPriority = convertToInt(value); }

    @PropertyName("likesCount")
    public int getLikesCount() { return likesCount != 0 ? likesCount : likes; }
    @PropertyName("likesCount")
    public void setLikesCount(Object value) { this.likesCount = convertToInt(value); }

    @PropertyName("likes")
    public int getLikes() { return likes; }
    @PropertyName("likes")
    public void setLikes(Object value) { this.likes = convertToInt(value); }

    @PropertyName("saves")
    public int getSaves() { return saves; }
    @PropertyName("saves")
    public void setSaves(Object value) { this.saves = convertToInt(value); }

    @PropertyName("comments")
    public int getComments() { return comments; }
    @PropertyName("comments")
    public void setComments(Object value) { this.comments = convertToInt(value); }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("authorId")
    public String getAuthorId() { return authorId; }
    @PropertyName("authorId")
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("username")
    public String getUsername() { return username; }
    @PropertyName("username")
    public void setUsername(String username) { this.username = username; }

    @PropertyName("ingredients")
    public List<String> getIngredients() { return ingredients; }
    @PropertyName("ingredients")
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    @PropertyName("ingredientIds")
    public List<String> getIngredientIds() { return ingredientIds; }
    @PropertyName("ingredientIds")
    public void setIngredientIds(List<String> ingredientIds) { this.ingredientIds = ingredientIds; }

    @PropertyName("instructions")
    public List<String> getInstructions() { return instructions; }
    @PropertyName("instructions")
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    @PropertyName("likedBy")
    public List<String> getLikedBy() { return likedBy; }
    @PropertyName("likedBy")
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    @PropertyName("savedBy")
    public List<String> getSavedBy() { return savedBy; }
    @PropertyName("savedBy")
    public void setSavedBy(List<String> savedBy) { this.savedBy = savedBy; }

    @PropertyName("isDraft")
    public boolean isDraft() { return isDraft; }
    @PropertyName("isDraft")
    public void setDraftFlag(boolean draft) { isDraft = draft; }

    @PropertyName("draft")
    public boolean getDraft() { return draft; }
    @PropertyName("draft")
    public void setDraft(boolean draft) { this.draft = draft; }

    @PropertyName("cookingInstructions")
    public String getCookingInstructions() { return cookingInstructions; }
    @PropertyName("cookingInstructions")
    public void setCookingInstructions(String cookingInstructions) { this.cookingInstructions = cookingInstructions; }

    @PropertyName("videoUrl")
    public String getVideoUrl() { return videoUrl; }
    @PropertyName("videoUrl")
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    @PropertyName("createdBy")
    public String getCreatedBy() { return createdBy; }
    @PropertyName("createdBy")
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Exclude
    public Map<String, Object> getMetadata() { return metadata; }
    @Exclude
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Exclude
    public int getImageResId() { return imageResId; }
    @Exclude
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    private long convertToLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private int convertToInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (Exception e) { return 0; }
        }
        return 0;
    }
}
