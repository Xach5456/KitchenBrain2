package com.example.kitchenbrain;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🔥 STATIC CACHE - PERSISTENT STATE ACROSS FRAGMENT LIFECYCLE
 * 
 * Problem: SearchFragment gets destroyed on navigation → all data lost
 * Solution: Static cache that survives fragment recreation
 * Thread-safe with synchronized lists
 */
public class SearchCache {
    
    // 🔥 PERSISTENT DATA - survives fragment destruction (thread-safe)
    public static List<String> selectedIngredients = Collections.synchronizedList(new ArrayList<>());
    public static List<com.example.kitchenbrain.model.Recipe> cachedRecipes = Collections.synchronizedList(new ArrayList<>());
    public static List<com.example.kitchenbrain.model.Recipe> filteredRecipes = Collections.synchronizedList(new ArrayList<>());
    public static boolean isDataLoaded = false;
    
    /**
     * 🔥 Clear cache when needed (e.g., logout)
     */
    public static void clearCache() {
        Log.d("SEARCH_DEBUG", "🔥 KILLER FOUND: clearCache() called - this clears selectedIngredients");
        selectedIngredients.clear();
        cachedRecipes.clear();
        filteredRecipes.clear();
        isDataLoaded = false;
    }
    
    /**
     * 🔥 Check if we have cached data
     */
    public static boolean hasCachedData() {
        return isDataLoaded && !cachedRecipes.isEmpty();
    }
    
    /**
     * 🔥 Save current state
     */
    public static void saveState(List<String> ingredients, List<com.example.kitchenbrain.model.Recipe> recipes) {
        // 🔥 CRITICAL FIX: Use copy to avoid reference bug
        selectedIngredients = new ArrayList<>();
        if (ingredients != null) {
            selectedIngredients.addAll(ingredients);
        }
        
        filteredRecipes = new ArrayList<>();
        if (recipes != null) {
            filteredRecipes.addAll(recipes);
        }
    }
}
