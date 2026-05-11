package com.example.kitchenbrain.api.spoonacular;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SpoonacularRecipe {
    @SerializedName("id")
    public int id;
    
    @SerializedName("title")
    public String title;
    
    @SerializedName("image")
    public String image;
    
    @SerializedName("readyInMinutes")
    public int readyInMinutes;
    
    @SerializedName("summary")
    public String summary;
    
    @SerializedName("instructions")
    public String instructions;
    
    @SerializedName("servings")
    public int servings;
    
    @SerializedName("sourceUrl")
    public String sourceUrl;
    
    @SerializedName("aggregateLikes")
    public int aggregateLikes;
    
    @SerializedName("extendedIngredients")
    public List<Ingredient> extendedIngredients;

    public static class Ingredient {
        @SerializedName("original")
        public String original;
    }
}
