package com.example.kitchenbrain.ai;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AIRecipeGenerator - Viral feature for AI-powered recipe generation
 * 
 * 🔥 AI RECIPE ARCHITECTURE:
 * - User inputs: chicken rice cheese garlic
 * - AI generates: title, ingredients, steps, calories, description
 * - Smart recipe combinations based on ingredient compatibility
 * - Nutritional estimation and cooking time calculation
 */
public class AIRecipeGenerator {
    
    private static final String TAG = "AIRecipeGenerator";
    
    // Ingredient compatibility database
    private static final Map<String, List<String>> INGREDIENT_COMPATIBILITY = new HashMap<>();
    private static final Map<String, String> INGREDIENT_CATEGORIES = new HashMap<>();
    private static final List<String> COOKING_METHODS = Arrays.asList(
        "baked", "fried", "grilled", "roasted", "steamed", "sautéed", "boiled", "stir-fried"
    );
    private static final List<String> CUISINE_STYLES = Arrays.asList(
        "Italian", "Asian", "Mexican", "Mediterranean", "American", "Indian", "French"
    );
    
    static {
        initializeIngredientDatabase();
    }
    
    /**
     * Generate recipe from ingredient list
     */
    public void generateRecipe(List<String> ingredients, RecipeGenerationCallback callback) {
        Log.d(TAG, "🤖 Generating AI recipe from: " + ingredients);
        
        if (ingredients == null || ingredients.isEmpty()) {
            callback.onError("No ingredients provided");
            return;
        }
        
        try {
            AIRecipe recipe = createAIRecipe(ingredients);
            Log.d(TAG, "✅ AI recipe generated: " + recipe.title);
            callback.onSuccess(recipe);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to generate recipe", e);
            callback.onError("Failed to generate recipe: " + e.getMessage());
        }
    }
    
    /**
     * Create AI recipe from ingredients
     */
    private AIRecipe createAIRecipe(List<String> ingredients) {
        // Normalize ingredients
        List<String> normalizedIngredients = normalizeIngredients(ingredients);
        
        // Determine main ingredient and cuisine style
        String mainIngredient = determineMainIngredient(normalizedIngredients);
        String cuisineStyle = determineCuisineStyle(normalizedIngredients);
        String cookingMethod = determineCookingMethod(normalizedIngredients);
        
        // Generate recipe components
        String title = generateRecipeTitle(mainIngredient, cuisineStyle, cookingMethod);
        String description = generateRecipeDescription(mainIngredient, cuisineStyle, cookingMethod);
        List<String> fullIngredients = generateFullIngredients(normalizedIngredients);
        List<String> steps = generateCookingSteps(normalizedIngredients, cookingMethod);
        int cookTime = estimateCookingTime(normalizedIngredients, cookingMethod);
        int calories = estimateCalories(fullIngredients);
        String difficulty = determineDifficulty(normalizedIngredients, cookingMethod);
        int servings = estimateServings(normalizedIngredients);
        
        return new AIRecipe(
            title,
            description,
            fullIngredients,
            steps,
            cookTime,
            calories,
            difficulty,
            servings,
            cuisineStyle,
            cookingMethod,
            mainIngredient,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Normalize ingredient names
     */
    private List<String> normalizeIngredients(List<String> ingredients) {
        List<String> normalized = new ArrayList<>();
        for (String ingredient : ingredients) {
            if (ingredient != null && !ingredient.trim().isEmpty()) {
                String norm = ingredient.toLowerCase().trim();
                // Remove common suffixes
                if (norm.endsWith("s")) norm = norm.substring(0, norm.length() - 1);
                normalized.add(norm);
            }
        }
        return normalized;
    }
    
    /**
     * Determine main ingredient (most substantial)
     */
    private String determineMainIngredient(List<String> ingredients) {
        // Priority: protein > starch > vegetable > other
        for (String ingredient : ingredients) {
            String category = INGREDIENT_CATEGORIES.get(ingredient);
            if ("protein".equals(category)) {
                return ingredient;
            }
        }
        for (String ingredient : ingredients) {
            String category = INGREDIENT_CATEGORIES.get(ingredient);
            if ("starch".equals(category)) {
                return ingredient;
            }
        }
        return ingredients.get(0); // fallback
    }
    
    /**
     * Determine cuisine style based on ingredients
     */
    private String determineCuisineStyle(List<String> ingredients) {
        // Simple heuristics for cuisine detection
        if (containsAny(ingredients, Arrays.asList("rice", "soy", "ginger", "garlic"))) {
            return "Asian";
        }
        if (containsAny(ingredients, Arrays.asList("pasta", "tomato", "basil", "olive"))) {
            return "Italian";
        }
        if (containsAny(ingredients, Arrays.asList("tortilla", "cheese", "chili", "bean"))) {
            return "Mexican";
        }
        if (containsAny(ingredients, Arrays.asList("bread", "cheese", "butter"))) {
            return "French";
        }
        return "Mediterranean"; // default
    }
    
    /**
     * Determine cooking method
     */
    private String determineCookingMethod(List<String> ingredients) {
        // Heuristics for cooking method
        if (containsAny(ingredients, Arrays.asList("chicken", "beef", "pork"))) {
            return "grilled";
        }
        if (containsAny(ingredients, Arrays.asList("pasta", "rice"))) {
            return "boiled";
        }
        if (containsAny(ingredients, Arrays.asList("egg", "cheese"))) {
            return "fried";
        }
        return "baked"; // default
    }
    
    /**
     * Generate recipe title
     */
    private String generateRecipeTitle(String mainIngredient, String cuisine, String method) {
        Random random = new Random();
        List<String> titleTemplates = Arrays.asList(
            "Delicious " + capitalize(mainIngredient) + " " + method,
            cuisine + " Style " + capitalize(mainIngredient),
            "Easy " + capitalize(mainIngredient) + " Recipe",
            "Homemade " + cuisine + " " + capitalize(mainIngredient),
            "Quick " + capitalize(mainIngredient) + " " + method
        );
        
        return titleTemplates.get(random.nextInt(titleTemplates.size()));
    }
    
    /**
     * Generate recipe description
     */
    private String generateRecipeDescription(String mainIngredient, String cuisine, String method) {
        return "A delicious " + cuisine.toLowerCase() + " style " + mainIngredient + " dish, " + method + " to perfection. " +
               "This recipe combines traditional flavors with modern cooking techniques for a memorable meal. " +
               "Perfect for family dinners or special occasions.";
    }
    
    /**
     * Generate full ingredient list with quantities
     */
    private List<String> generateFullIngredients(List<String> userIngredients) {
        List<String> fullIngredients = new ArrayList<>();
        Random random = new Random();
        
        // Add user ingredients with quantities
        for (String ingredient : userIngredients) {
            String quantity = generateQuantity(ingredient);
            fullIngredients.add(quantity + " " + capitalize(ingredient));
        }
        
        // Add common complementary ingredients
        List<String> complementary = getComplementaryIngredients(userIngredients);
        for (String ingredient : complementary) {
            if (random.nextBoolean()) { // Randomly add some complementary ingredients
                String quantity = generateQuantity(ingredient);
                fullIngredients.add(quantity + " " + capitalize(ingredient));
            }
        }
        
        return fullIngredients;
    }
    
    /**
     * Generate cooking steps
     */
    private List<String> generateCookingSteps(List<String> ingredients, String method) {
        List<String> steps = new ArrayList<>();
        
        // Common step templates
        steps.add("Prepare all ingredients by washing and cutting as needed.");
        steps.add("Heat a large pan over medium heat and add oil.");
        
        // Method-specific steps
        switch (method) {
            case "grilled":
                steps.add("Season the main ingredients with salt, pepper, and spices.");
                steps.add("Place on grill and cook for 4-6 minutes per side until done.");
                break;
            case "fried":
                steps.add("Coat ingredients with flour or batter as needed.");
                steps.add("Fry in hot oil until golden brown and crispy.");
                break;
            case "baked":
                steps.add("Preheat oven to 375°F (190°C).");
                steps.add("Place ingredients in baking dish and bake for 25-30 minutes.");
                break;
            default:
                steps.add("Cook ingredients according to package instructions or until tender.");
        }
        
        // Final steps
        steps.add("Garnish with fresh herbs and serve hot.");
        steps.add("Enjoy your delicious homemade meal!");
        
        return steps;
    }
    
    /**
     * Estimate cooking time in minutes
     */
    private int estimateCookingTime(List<String> ingredients, String method) {
        int baseTime = 15; // minimum prep time
        
        switch (method) {
            case "grilled": baseTime += 15; break;
            case "fried": baseTime += 10; break;
            case "baked": baseTime += 30; break;
            case "boiled": baseTime += 20; break;
            default: baseTime += 15;
        }
        
        // Add time based on ingredient complexity
        baseTime += ingredients.size() * 2;
        
        return baseTime;
    }
    
    /**
     * Estimate calories
     */
    private int estimateCalories(List<String> ingredients) {
        int totalCalories = 0;
        
        for (String ingredient : ingredients) {
            totalCalories += getIngredientCalories(ingredient);
        }
        
        // Add calories for cooking method (oil, etc.)
        totalCalories += 50;
        
        return totalCalories;
    }
    
    /**
     * Determine difficulty level
     */
    private String determineDifficulty(List<String> ingredients, String method) {
        int complexity = ingredients.size();
        
        if (method.equals("baked") || method.equals("grilled")) {
            complexity += 1;
        }
        
        if (complexity <= 3) return "Easy";
        if (complexity <= 6) return "Medium";
        return "Hard";
    }
    
    /**
     * Estimate servings
     */
    private int estimateServings(List<String> ingredients) {
        // Base on protein ingredients
        int proteinCount = 0;
        for (String ingredient : ingredients) {
            if ("protein".equals(INGREDIENT_CATEGORIES.get(ingredient))) {
                proteinCount++;
            }
        }
        
        return Math.max(2, proteinCount * 2);
    }
    
    // Helper methods
    private boolean containsAny(List<String> list, List<String> targets) {
        for (String target : targets) {
            if (list.contains(target)) return true;
        }
        return false;
    }
    
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private String generateQuantity(String ingredient) {
        Random random = new Random();
        String category = INGREDIENT_CATEGORIES.get(ingredient);
        
        switch (category) {
            case "protein": return (random.nextInt(3) + 1) + " lbs";
            case "starch": return (random.nextInt(2) + 1) + " cups";
            case "vegetable": return (random.nextInt(3) + 1) + " cups";
            case "dairy": return (random.nextInt(2) + 1) + " cups";
            case "spice": return (random.nextInt(3) + 1) + " tsp";
            default: return "1 cup";
        }
    }
    
    private List<String> getComplementaryIngredients(List<String> ingredients) {
        List<String> complementary = new ArrayList<>();
        
        for (String ingredient : ingredients) {
            List<String> comps = INGREDIENT_COMPATIBILITY.get(ingredient);
            if (comps != null) {
                complementary.addAll(comps);
            }
        }
        
        return complementary;
    }
    
    private int getIngredientCalories(String ingredient) {
        // Approximate calories per standard serving
        String category = INGREDIENT_CATEGORIES.get(ingredient);
        switch (category) {
            case "protein": return 200;
            case "starch": return 150;
            case "vegetable": return 50;
            case "dairy": return 100;
            case "spice": return 5;
            default: return 100;
        }
    }
    
    /**
     * Initialize ingredient database
     */
    private static void initializeIngredientDatabase() {
        // Ingredient categories
        INGREDIENT_CATEGORIES.put("chicken", "protein");
        INGREDIENT_CATEGORIES.put("beef", "protein");
        INGREDIENT_CATEGORIES.put("pork", "protein");
        INGREDIENT_CATEGORIES.put("fish", "protein");
        INGREDIENT_CATEGORIES.put("egg", "protein");
        
        INGREDIENT_CATEGORIES.put("rice", "starch");
        INGREDIENT_CATEGORIES.put("pasta", "starch");
        INGREDIENT_CATEGORIES.put("potato", "starch");
        INGREDIENT_CATEGORIES.put("bread", "starch");
        
        INGREDIENT_CATEGORIES.put("tomato", "vegetable");
        INGREDIENT_CATEGORIES.put("onion", "vegetable");
        INGREDIENT_CATEGORIES.put("garlic", "vegetable");
        INGREDIENT_CATEGORIES.put("carrot", "vegetable");
        INGREDIENT_CATEGORIES.put("broccoli", "vegetable");
        
        INGREDIENT_CATEGORIES.put("cheese", "dairy");
        INGREDIENT_CATEGORIES.put("milk", "dairy");
        INGREDIENT_CATEGORIES.put("butter", "dairy");
        
        INGREDIENT_CATEGORIES.put("salt", "spice");
        INGREDIENT_CATEGORIES.put("pepper", "spice");
        INGREDIENT_CATEGORIES.put("basil", "spice");
        INGREDIENT_CATEGORIES.put("oregano", "spice");
        
        // Ingredient compatibility
        INGREDIENT_COMPATIBILITY.put("chicken", Arrays.asList("rice", "vegetable", "garlic", "onion"));
        INGREDIENT_COMPATIBILITY.put("rice", Arrays.asList("vegetable", "soy", "ginger", "garlic"));
        INGREDIENT_COMPATIBILITY.put("pasta", Arrays.asList("tomato", "cheese", "basil", "garlic"));
        INGREDIENT_COMPATIBILITY.put("tomato", Arrays.asList("cheese", "basil", "onion", "garlic"));
    }
    
    /**
     * AI Recipe data model
     */
    public static class AIRecipe {
        public final String title;
        public final String description;
        public final List<String> ingredients;
        public final List<String> steps;
        public final int cookTime;
        public final int calories;
        public final String difficulty;
        public final int servings;
        public final String cuisineStyle;
        public final String cookingMethod;
        public final String mainIngredient;
        public final long generatedAt;
        
        public AIRecipe(String title, String description, List<String> ingredients, List<String> steps,
                       int cookTime, int calories, String difficulty, int servings, String cuisineStyle,
                       String cookingMethod, String mainIngredient, long generatedAt) {
            this.title = title;
            this.description = description;
            this.ingredients = ingredients;
            this.steps = steps;
            this.cookTime = cookTime;
            this.calories = calories;
            this.difficulty = difficulty;
            this.servings = servings;
            this.cuisineStyle = cuisineStyle;
            this.cookingMethod = cookingMethod;
            this.mainIngredient = mainIngredient;
            this.generatedAt = generatedAt;
        }
        
        @Override
        public String toString() {
            return "AIRecipe{" +
                    "title='" + title + '\'' +
                    ", difficulty='" + difficulty + '\'' +
                    ", cookTime=" + cookTime +
                    ", calories=" + calories +
                    '}';
        }
    }
    
    // Callback interface
    public interface RecipeGenerationCallback {
        void onSuccess(AIRecipe recipe);
        void onError(String error);
    }
}
