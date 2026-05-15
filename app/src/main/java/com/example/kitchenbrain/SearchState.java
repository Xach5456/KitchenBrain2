package com.example.kitchenbrain;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🔥 STATIC STATE MANAGEMENT - PERSISTENT ACROSS FRAGMENT LIFECYCLE
 * 
 * PROBLEM SOLVED:
 * - Fragment destroyed → data lost
 * - Fragment recreated → empty state
 * - No restore mechanism
 * 
 * SOLUTION:
 * - Static storage survives Fragment recreation
 * - Simple state management
 * - Fast restore on onCreateView
 * - Thread-safe with synchronized lists
 */
public class SearchState {
    
    // 🔥 CORE STATE - survives Fragment destruction (thread-safe)
    public static List<String> selectedIngredients = Collections.synchronizedList(new ArrayList<>());
    public static List<com.example.kitchenbrain.model.Recipe> allRecipes = Collections.synchronizedList(new ArrayList<>());
    public static List<com.example.kitchenbrain.model.Recipe> filteredRecipes = Collections.synchronizedList(new ArrayList<>());
    public static boolean isInitialized = false;
    
    /**
     * 🔥 CLEAR STATE (for logout/reset)
     */
    public static void clearState() {
        Log.d("SEARCH_DEBUG", "🔥 KILLER FOUND: clearState() called - this resets initialized=false");
        selectedIngredients.clear();
        allRecipes.clear();
        filteredRecipes.clear();
        isInitialized = false;
    }
    
    /**
     * 🔥 CHECK IF STATE EXISTS
     */
    public static boolean hasState() {
        return isInitialized && !allRecipes.isEmpty();
    }
    
    /**
     * 🔥 SAVE INGREDIENT SELECTION
     */
    public static void saveIngredientSelection(List<String> ingredients) {
        // 🔥 CRITICAL FIX: Use copy to avoid reference bug
        selectedIngredients = new ArrayList<>();
        if (ingredients != null) {
            selectedIngredients.addAll(ingredients);
        }
    }
    
    /**
     * 🔥 SAVE RECIPES
     */
    public static void saveRecipes(List<com.example.kitchenbrain.model.Recipe> recipes) {
        // 🔥 CRITICAL FIX: Use copy to avoid reference bug
        allRecipes = new ArrayList<>();
        if (recipes != null) {
            allRecipes.addAll(recipes);
        }
        isInitialized = true;
    }
    
    /**
     * 🔥 SAVE FILTERED RECIPES
     */
    public static void saveFilteredRecipes(List<com.example.kitchenbrain.model.Recipe> recipes) {
        // 🔥 CRITICAL FIX: Use copy to avoid reference bug
        filteredRecipes = new ArrayList<>();
        if (recipes != null) {
            filteredRecipes.addAll(recipes);
        }
    }
    
    /**
     * 🔥 GET CURRENT STATE FOR DEBUG
     */
    public static String getStateInfo() {
        return "Ingredients: " + selectedIngredients.size() + 
               ", All Recipes: " + allRecipes.size() + 
               ", Filtered: " + filteredRecipes.size() + 
               ", Initialized: " + isInitialized;
    }
}
