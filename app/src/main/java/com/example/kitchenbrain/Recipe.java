package com.example.kitchenbrain;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * App model for a recipe. {@link Parcelable} so we can pass instances in {@link android.content.Intent}
 * to {@link RecipeDetailActivity} without fragile manual serialization.
 * {@link Serializable} kept for existing {@link Bundle#putSerializable} / Firestore flows.
 */
public class Recipe implements Parcelable, Serializable {
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
    private List<String> ingredientIds; // List of ingredient IDs for filtering
    /** Average user rating 0..5 (shown with {@link android.widget.RatingBar} on detail screen). */
    private float averageRating;
    /** Number of text reviews — shown next to stars, e.g. "4.7 (124 отзыва)". */
    private int reviewCount;
    /** Number of servings (порции). */
    private int servings;
    /** e.g. Easy / Medium / Hard — shown as a pill on the detail screen. */
    private String difficulty;
    /** Optional tips card (Material 3 detail UX). */
    private String chefTips;
    /** Short nutrition summary text (per serving or whole dish). */
    private String nutritionFacts;
    /** Comment count for social row (separate from star reviewCount). */
    private int commentCount;
    /** Stability score for recipe ranking/quality */
    private double stability;

    // Default constructor required for Firebase
    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.ingredientIds = new ArrayList<>();
        this.averageRating = 0f;
        this.reviewCount = 0;
        this.servings = 4;
        this.difficulty = "Medium";
        this.chefTips = "";
        this.nutritionFacts = "";
        this.commentCount = 0;
    }

    // Full constructor
    public Recipe(String id, String name, String description, int cookingTime, 
                  String cookingInstructions, String imageUrl, String videoUrl,
                  String authorId, String username, Object createdAt, int likesCount, 
                  List<String> ingredients, List<String> ingredientIds) {
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
        this.ingredientIds = ingredientIds != null ? ingredientIds : new ArrayList<>();
        this.averageRating = 0f;
        this.reviewCount = 0;
        this.servings = 4;
        this.difficulty = "Medium";
        this.chefTips = "";
        this.nutritionFacts = "";
        this.commentCount = 0;
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
    
    // Method to add ingredient ID
    public void addIngredientId(String ingredientId) {
        if (ingredientIds == null) {
            ingredientIds = new ArrayList<>();
        }
        if (ingredientId != null && !ingredientId.trim().isEmpty()) {
            ingredientIds.add(ingredientId.trim());
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
    
    // Getter and setter for ingredientIds
    public List<String> getIngredientIds() {
        return ingredientIds != null ? ingredientIds : new ArrayList<>();
    }
    
    public void setIngredientIds(List<String> ingredientIds) {
        this.ingredientIds = ingredientIds;
    }

    public float getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getDifficulty() {
        return difficulty != null ? difficulty : "Medium";
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getChefTips() {
        return chefTips != null ? chefTips : "";
    }

    public void setChefTips(String chefTips) {
        this.chefTips = chefTips;
    }

    public String getNutritionFacts() {
        return nutritionFacts != null ? nutritionFacts : "";
    }

    public void setNutritionFacts(String nutritionFacts) {
        this.nutritionFacts = nutritionFacts;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    // --- Parcelable (pass to RecipeDetailActivity via Intent) ---

    protected Recipe(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        cookingTime = in.readInt();
        cookingInstructions = in.readString();
        imageUrl = in.readString();
        videoUrl = in.readString();
        authorId = in.readString();
        username = in.readString();
        likesCount = in.readInt();
        ingredients = in.createStringArrayList();
        if (ingredients == null) ingredients = new ArrayList<>();
        ingredientIds = in.createStringArrayList();
        if (ingredientIds == null) ingredientIds = new ArrayList<>();
        averageRating = in.readFloat();
        reviewCount = in.readInt();
        servings = in.readInt();
        long createdMs = in.readLong();
        createdAt = createdMs >= 0 ? new Date(createdMs) : null;
        difficulty = in.readString();
        if (difficulty == null || difficulty.isEmpty()) {
            difficulty = "Medium";
        }
        chefTips = in.readString();
        nutritionFacts = in.readString();
        commentCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeInt(cookingTime);
        dest.writeString(cookingInstructions);
        dest.writeString(imageUrl);
        dest.writeString(videoUrl);
        dest.writeString(authorId);
        dest.writeString(username);
        dest.writeInt(likesCount);
        dest.writeStringList(ingredients != null ? ingredients : new ArrayList<>());
        dest.writeStringList(ingredientIds != null ? ingredientIds : new ArrayList<>());
        dest.writeFloat(averageRating);
        dest.writeInt(reviewCount);
        dest.writeInt(servings);
        Date d = getCreatedAt();
        dest.writeLong(d != null ? d.getTime() : -1L);
        dest.writeString(difficulty != null ? difficulty : "Medium");
        dest.writeString(chefTips != null ? chefTips : "");
        dest.writeString(nutritionFacts != null ? nutritionFacts : "");
        dest.writeInt(commentCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };
}