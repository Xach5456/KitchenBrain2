package com.example.kitchenbrain;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ UNIVERSAL RECIPE MODEL
 * Fully compatible with all Firestore fields and types to eliminate warnings and crashes.
 * Handles robust type conversion for String/Long/Timestamp/Int mismatches.
 */
@IgnoreExtraProperties
public class Recipe implements Parcelable, Serializable {
    @Exclude private String id;
    @Exclude private String recipeId;
    @Exclude private String name;
    @Exclude private String title;
    @Exclude private String description;
    @Exclude private long cookingTime;
    @Exclude private long cookTime;
    @Exclude private String formattedCookTime;
    @Exclude private String cookingInstructions;
    @Exclude private String imageUrl;   
    @Exclude private String videoUrl;   
    @Exclude private String authorId;   
    @Exclude private String authorName;
    @Exclude private String username;   
    @Exclude private long createdAtLong;    
    @Exclude private long updatedAt;
    
    // Engagement
    @Exclude private int likesCount;    
    @Exclude private int likes;
    @Exclude private List<String> likedBy; 
    @Exclude private int saves;
    @Exclude private List<String> savedBy;
    @Exclude private int comments;
    @Exclude private int socialPriority;
    
    // Lists
    @Exclude private List<String> ingredients; 
    @Exclude private List<String> ingredientIds; 
    @Exclude private List<String> steps;
    @Exclude private List<String> tags;
    
    // Status Flags
    @Exclude private boolean isDraft;
    @Exclude private boolean draft;
    @Exclude private boolean fromFollower;
    @Exclude private boolean fromMutualFollower;
    @Exclude private boolean isFromFollower;
    @Exclude private boolean isFromMutualFollower;
    
    @Exclude private int servings;
    @Exclude private int calories;
    @Exclude private String difficulty;

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new ArrayList<>();
        this.steps = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.likedBy = new ArrayList<>();
        this.savedBy = new ArrayList<>();
    }

    @PropertyName("id")
    public String getId() { return id != null ? id : recipeId; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("recipeId")
    public String getRecipeId() { return recipeId; }
    @PropertyName("recipeId")
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("title")
    public String getTitle() { return title != null ? title : name; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    @PropertyName("description")
    public void setDescription(String description) { this.description = description; }

    @PropertyName("cookingTime")
    public long getCookingTime() { return cookingTime != 0 ? cookingTime : cookTime; }
    @PropertyName("cookingTime")
    public void setCookingTime(Object value) { this.cookingTime = convertToLong(value); }

    @PropertyName("cookTime")
    public long getCookTime() { return cookTime; }
    @PropertyName("cookTime")
    public void setCookTime(Object value) { this.cookTime = convertToLong(value); }

    @PropertyName("formattedCookTime")
    public String getFormattedCookTime() { return formattedCookTime; }
    @PropertyName("formattedCookTime")
    public void setFormattedCookTime(String formattedCookTime) { this.formattedCookTime = formattedCookTime; }

    @PropertyName("cookingInstructions")
    public String getCookingInstructions() { return cookingInstructions; }
    @PropertyName("cookingInstructions")
    public void setCookingInstructions(String cookingInstructions) { this.cookingInstructions = cookingInstructions; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("authorId")
    public String getAuthorId() { return authorId; }
    @PropertyName("authorId")
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    @PropertyName("authorName")
    public String getAuthorName() { return authorName != null ? authorName : username; }
    @PropertyName("authorName")
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    @PropertyName("username")
    public String getUsername() { return username; }
    @PropertyName("username")
    public void setUsername(String username) { this.username = username; }

    @PropertyName("ingredients")
    public List<String> getIngredients() { return ingredients; }
    @PropertyName("ingredients")
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    @PropertyName("likesCount")
    public int getLikesCount() { return likesCount != 0 ? likesCount : likes; }
    @PropertyName("likesCount")
    public void setLikesCount(Object value) { this.likesCount = convertToInt(value); }

    @PropertyName("likes")
    public int getLikes() { return likes; }
    @PropertyName("likes")
    public void setLikes(Object value) { this.likes = convertToInt(value); }

    @PropertyName("likedBy")
    public List<String> getLikedBy() { return likedBy; }
    @PropertyName("likedBy")
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    @PropertyName("saves")
    public int getSaves() { return saves; }
    @PropertyName("saves")
    public void setSaves(Object value) { this.saves = convertToInt(value); }

    @PropertyName("savedBy")
    public List<String> getSavedBy() { return savedBy; }
    @PropertyName("savedBy")
    public void setSavedBy(List<String> savedBy) { this.savedBy = savedBy; }

    @PropertyName("comments")
    public int getComments() { return comments; }
    @PropertyName("comments")
    public void setComments(Object value) { this.comments = convertToInt(value); }

    @PropertyName("socialPriority")
    public int getSocialPriority() { return socialPriority; }
    @PropertyName("socialPriority")
    public void setSocialPriority(Object value) { this.socialPriority = convertToInt(value); }

    @PropertyName("videoUrl")
    public String getVideoUrl() { return videoUrl; }
    @PropertyName("videoUrl")
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    @PropertyName("createdAt")
    public Object getCreatedAt() { return createdAtLong; }
    @PropertyName("createdAt")
    public void setCreatedAt(Object value) { this.createdAtLong = convertToLong(value); }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }
    @PropertyName("updatedAt")
    public void setUpdatedAt(Object value) { this.updatedAt = convertToLong(value); }

    @PropertyName("ingredientIds")
    public List<String> getIngredientIds() { return ingredientIds; }
    @PropertyName("ingredientIds")
    public void setIngredientIds(List<String> ingredientIds) { this.ingredientIds = ingredientIds; }

    @PropertyName("steps")
    public List<String> getSteps() { return steps; }
    @PropertyName("steps")
    public void setSteps(List<String> steps) { this.steps = steps; }

    @PropertyName("tags")
    public List<String> getTags() { return tags; }
    @PropertyName("tags")
    public void setTags(List<String> tags) { this.tags = tags; }

    @PropertyName("isDraft")
    public boolean isDraft() { return isDraft; }
    @PropertyName("isDraft")
    public void setDraftFlag(boolean draft) { isDraft = draft; }

    @PropertyName("draft")
    public boolean getDraft() { return draft; }
    @PropertyName("draft")
    public void setDraft(boolean draft) { this.draft = draft; }

    @PropertyName("fromFollower")
    public boolean isFromFollower() { return fromFollower; }
    @PropertyName("fromFollower")
    public void setFromFollower(boolean fromFollower) { this.fromFollower = fromFollower; }

    @PropertyName("fromMutualFollower")
    public boolean isFromMutualFollower() { return fromMutualFollower; }
    @PropertyName("fromMutualFollower")
    public void setFromMutualFollower(boolean fromMutualFollower) { this.fromMutualFollower = fromMutualFollower; }

    @PropertyName("isFromFollower")
    public boolean getIsFromFollower() { return isFromFollower; }
    @PropertyName("isFromFollower")
    public void setIsFromFollower(boolean value) { this.isFromFollower = value; }

    @PropertyName("isFromMutualFollower")
    public boolean getIsFromMutualFollower() { return isFromMutualFollower; }
    @PropertyName("isFromMutualFollower")
    public void setIsFromMutualFollower(boolean value) { this.isFromMutualFollower = value; }

    @PropertyName("servings")
    public int getServings() { return servings; }
    @PropertyName("servings")
    public void setServings(Object value) { this.servings = convertToInt(value); }

    @PropertyName("calories")
    public int getCalories() { return calories; }
    @PropertyName("calories")
    public void setCalories(Object value) { this.calories = convertToInt(value); }

    @PropertyName("difficulty")
    public String getDifficulty() { return difficulty; }
    @PropertyName("difficulty")
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    private long convertToLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Double) return ((Double) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof java.util.Date) return ((java.util.Date) value).getTime();
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

    // --- Parcelable Implementation ---
    protected Recipe(Parcel in) {
        id = in.readString();
        recipeId = in.readString();
        name = in.readString();
        title = in.readString();
        description = in.readString();
        cookingTime = in.readLong();
        cookTime = in.readLong();
        formattedCookTime = in.readString();
        cookingInstructions = in.readString();
        imageUrl = in.readString();
        videoUrl = in.readString();
        authorId = in.readString();
        authorName = in.readString();
        username = in.readString();
        createdAtLong = in.readLong();
        updatedAt = in.readLong();
        likesCount = in.readInt();
        likes = in.readInt();
        likedBy = in.createStringArrayList();
        saves = in.readInt();
        savedBy = in.createStringArrayList();
        comments = in.readInt();
        socialPriority = in.readInt();
        ingredients = in.createStringArrayList();
        ingredientIds = in.createStringArrayList();
        steps = in.createStringArrayList();
        tags = in.createStringArrayList();
        isDraft = in.readByte() != 0;
        draft = in.readByte() != 0;
        fromFollower = in.readByte() != 0;
        fromMutualFollower = in.readByte() != 0;
        isFromFollower = in.readByte() != 0;
        isFromMutualFollower = in.readByte() != 0;
        servings = in.readInt();
        calories = in.readInt();
        difficulty = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(recipeId);
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(cookingTime);
        dest.writeLong(cookTime);
        dest.writeString(formattedCookTime);
        dest.writeString(cookingInstructions);
        dest.writeString(imageUrl);
        dest.writeString(videoUrl);
        dest.writeString(authorId);
        dest.writeString(authorName);
        dest.writeString(username);
        dest.writeLong(createdAtLong);
        dest.writeLong(updatedAt);
        dest.writeInt(likesCount);
        dest.writeInt(likes);
        dest.writeStringList(likedBy);
        dest.writeInt(saves);
        dest.writeStringList(savedBy);
        dest.writeInt(comments);
        dest.writeInt(socialPriority);
        dest.writeStringList(ingredients);
        dest.writeStringList(ingredientIds);
        dest.writeStringList(steps);
        dest.writeStringList(tags);
        dest.writeByte((byte) (isDraft ? 1 : 0));
        dest.writeByte((byte) (draft ? 1 : 0));
        dest.writeByte((byte) (fromFollower ? 1 : 0));
        dest.writeByte((byte) (fromMutualFollower ? 1 : 0));
        dest.writeByte((byte) (isFromFollower ? 1 : 0));
        dest.writeByte((byte) (isFromMutualFollower ? 1 : 0));
        dest.writeInt(servings);
        dest.writeInt(calories);
        dest.writeString(difficulty);
    }

    @Override public int describeContents() { return 0; }
    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override public Recipe createFromParcel(Parcel in) { return new Recipe(in); }
        @Override public Recipe[] newArray(int size) { return new Recipe[size]; }
    };
}
