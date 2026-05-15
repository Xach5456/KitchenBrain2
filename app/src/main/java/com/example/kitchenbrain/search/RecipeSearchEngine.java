package com.example.kitchenbrain.search;

import android.util.Log;
import com.example.kitchenbrain.model.SocialRecipe;
import com.example.kitchenbrain.repository.RecipeRepository;
import com.example.kitchenbrain.algorithm.FeedRankingAlgorithm;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RecipeSearchEngine - Killer feature for ingredient-based recipe discovery
 * 
 * 🔥 FRIDGE MODE ARCHITECTURE:
 * - User selects ingredients: tomato, rice, cheese
 * - Search priority: mutual followers > followers > popular > Spoonacular
 * - Smart ingredient matching and ranking
 * - Social proof integration
 */
public class RecipeSearchEngine {
    
    private static final String TAG = "RecipeSearchEngine";
    
    // Search priority weights
    private static final double MUTUAL_FOLLOWER_WEIGHT = 3.0;
    private static final double FOLLOWER_WEIGHT = 2.0;
    private static final double POPULARITY_WEIGHT = 1.5;
    private static final double FRESHNESS_WEIGHT = 1.2;
    private static final double INGREDIENT_MATCH_WEIGHT = 2.5;
    
    private final RecipeRepository recipeRepository;
    private final FeedRankingAlgorithm rankingAlgorithm;
    
    public RecipeSearchEngine() {
        this.recipeRepository = new RecipeRepository();
        this.rankingAlgorithm = FeedRankingAlgorithm.INSTANCE;
    }
    
    /**
     * Main search method - Fridge Mode
     * @param ingredients List of ingredients user has
     * @param currentUserId Current user ID
     * @param mutualFollowers List of mutual followers
     * @param followers List of followers
     * @param callback Search results callback
     */
    public void searchByIngredients(List<String> ingredients, String currentUserId,
                                  List<String> mutualFollowers, List<String> followers,
                                  SearchCallback callback) {
        
        Log.d(TAG, "🔥 Fridge Mode search: ingredients=" + ingredients);
        
        // Clean and normalize ingredients
        List<String> cleanIngredients = normalizeIngredients(ingredients);
        if (cleanIngredients.isEmpty()) {
            callback.onError("No valid ingredients provided");
            return;
        }
        
        // Execute parallel searches with different priorities
        executeParallelSearch(cleanIngredients, currentUserId, mutualFollowers, followers, callback);
    }
    
    /**
     * Execute parallel searches for different priority levels
     */
    private void executeParallelSearch(List<String> ingredients, String currentUserId,
                                     List<String> mutualFollowers, List<String> followers,
                                     SearchCallback callback) {
        
        List<SearchResult> allResults = new ArrayList<>();
        int[] completedSearches = {0};
        int totalSearches = 3; // mutual, followers, global
        
        // Search 1: Mutual followers recipes (highest priority)
        if (!mutualFollowers.isEmpty()) {
            searchMutualFollowersRecipes(ingredients, mutualFollowers, currentUserId, 
                new RecipeRepository.RecipeCallback<List<SocialRecipe>>() {
                    @Override
                    public void onSuccess(List<SocialRecipe> recipes) {
                        List<SearchResult> results = convertToSearchResults(recipes, "mutual", currentUserId);
                        allResults.addAll(results);
                        checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "⚠️ Mutual followers search failed: " + error);
                        checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                    }
                });
        } else {
            completedSearches[0]++;
        }
        
        // Search 2: Followers recipes (medium priority)
        if (!followers.isEmpty()) {
            searchFollowersRecipes(ingredients, followers, currentUserId,
                new RecipeRepository.RecipeCallback<List<SocialRecipe>>() {
                    @Override
                    public void onSuccess(List<SocialRecipe> recipes) {
                        List<SearchResult> results = convertToSearchResults(recipes, "follower", currentUserId);
                        allResults.addAll(results);
                        checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "⚠️ Followers search failed: " + error);
                        checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                    }
                });
        } else {
            completedSearches[0]++;
        }
        
        // Search 3: Global recipes (lowest priority)
        searchGlobalRecipes(ingredients, currentUserId,
            new RecipeRepository.RecipeCallback<List<SocialRecipe>>() {
                @Override
                public void onSuccess(List<SocialRecipe> recipes) {
                    List<SearchResult> results = convertToSearchResults(recipes, "global", currentUserId);
                    allResults.addAll(results);
                    checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                }
                
                @Override
                public void onError(String error) {
                    Log.w(TAG, "⚠️ Global search failed: " + error);
                    checkSearchCompletion(allResults, completedSearches, totalSearches, callback);
                }
            });
    }
    
    /**
     * Search mutual followers recipes
     */
    private void searchMutualFollowersRecipes(List<String> ingredients, List<String> mutualFollowers,
                                            String currentUserId, RecipeRepository.RecipeCallback<List<SocialRecipe>> callback) {
        // This would use the RecipeRepository's searchRecipes method
        // For now, simulate the search
        recipeRepository.searchRecipes(currentUserId, ingredients, mutualFollowers, new ArrayList<>(), callback);
    }
    
    /**
     * Search followers recipes
     */
    private void searchFollowersRecipes(List<String> ingredients, List<String> followers,
                                      String currentUserId, RecipeRepository.RecipeCallback<List<SocialRecipe>> callback) {
        recipeRepository.searchRecipes(currentUserId, ingredients, new ArrayList<>(), followers, callback);
    }
    
    /**
     * Search global recipes
     */
    private void searchGlobalRecipes(List<String> ingredients, String currentUserId,
                                   RecipeRepository.RecipeCallback<List<SocialRecipe>> callback) {
        recipeRepository.searchRecipes(currentUserId, ingredients, new ArrayList<>(), new ArrayList<>(), callback);
    }
    
    /**
     * Check if all searches are completed and return results
     */
    private void checkSearchCompletion(List<SearchResult> allResults, int[] completed, int total,
                                     SearchCallback callback) {
        completed[0]++;
        if (completed[0] >= total) {
            // Sort and rank all results
            List<SearchResult> rankedResults = rankSearchResults(allResults);
            
            // Remove duplicates and limit results
            List<SearchResult> finalResults = removeDuplicates(rankedResults);
            if (finalResults.size() > 50) {
                finalResults = finalResults.subList(0, 50);
            }
            
            Log.d(TAG, "✅ Search completed: " + finalResults.size() + " results");
            callback.onSuccess(finalResults);
        }
    }
    
    /**
     * Convert SocialRecipe to SearchResult with ranking
     */
    private List<SearchResult> convertToSearchResults(List<SocialRecipe> recipes, String priority, String currentUserId) {
        List<SearchResult> results = new ArrayList<>();
        
        for (SocialRecipe recipe : recipes) {
            double searchScore = calculateSearchScore(recipe, priority, currentUserId);
            results.add(new SearchResult(recipe, priority, searchScore));
        }
        
        return results;
    }
    
    /**
     * Calculate search score for ranking
     */
    private double calculateSearchScore(SocialRecipe recipe, String priority, String currentUserId) {
        double baseScore = rankingAlgorithm.calculateScore(recipe, currentUserId);
        
        // Apply priority multiplier
        double priorityMultiplier = getPriorityMultiplier(priority);
        
        // Apply ingredient match bonus
        double ingredientBonus = calculateIngredientMatchBonus(recipe);
        
        return baseScore * priorityMultiplier + ingredientBonus;
    }
    
    /**
     * Get priority multiplier based on social relationship
     */
    private double getPriorityMultiplier(String priority) {
        switch (priority) {
            case "mutual": return MUTUAL_FOLLOWER_WEIGHT;
            case "follower": return FOLLOWER_WEIGHT;
            case "global": return 1.0;
            default: return 1.0;
        }
    }
    
    /**
     * Calculate ingredient match bonus
     */
    private double calculateIngredientMatchBonus(SocialRecipe recipe) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return 0.0;
        }
        
        // More ingredients = higher bonus (assuming better match)
        return recipe.getIngredients().size() * INGREDIENT_MATCH_WEIGHT;
    }
    
    /**
     * Rank search results by score
     */
    private List<SearchResult> rankSearchResults(List<SearchResult> results) {
        Collections.sort(results, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult r1, SearchResult r2) {
                return Double.compare(r2.searchScore, r1.searchScore); // Descending order
            }
        });
        return results;
    }
    
    /**
     * Remove duplicate recipes
     */
    private List<SearchResult> removeDuplicates(List<SearchResult> results) {
        Set<String> seenRecipeIds = new HashSet<>();
        List<SearchResult> uniqueResults = new ArrayList<>();
        
        for (SearchResult result : results) {
            String recipeId = result.recipe.getRecipeId();
            if (!seenRecipeIds.contains(recipeId)) {
                seenRecipeIds.add(recipeId);
                uniqueResults.add(result);
            }
        }
        
        return uniqueResults;
    }
    
    /**
     * Normalize and clean ingredient names
     */
    private List<String> normalizeIngredients(List<String> ingredients) {
        List<String> clean = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        for (String ingredient : ingredients) {
            if (ingredient == null || ingredient.trim().isEmpty()) {
                continue;
            }
            
            // Normalize: lowercase, trim, remove plurals
            String normalized = ingredient.toLowerCase().trim();
            
            // Simple plural removal
            if (normalized.endsWith("s") && normalized.length() > 2) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            
            if (!seen.contains(normalized) && normalized.length() > 1) {
                seen.add(normalized);
                clean.add(normalized);
            }
        }
        
        return clean;
    }

    /**
     * Search result wrapper
     */
    public static class SearchResult {
        public final SocialRecipe recipe;
        public final String priority;
        public final double searchScore;
        
        public SearchResult(SocialRecipe recipe, String priority, double searchScore) {
            this.recipe = recipe;
            this.priority = priority;
            this.searchScore = searchScore;
        }
    }
    
    /**
     * Search callback interface
     */
    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }
}
