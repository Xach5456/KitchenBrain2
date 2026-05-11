package com.example.kitchenbrain.api.spoonacular;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SpoonacularResponse {
    @SerializedName("recipes")
    public List<SpoonacularRecipe> recipes;
}
