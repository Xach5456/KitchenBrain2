package com.example.kitchenbrain.algorithm

import com.example.kitchenbrain.model.SocialRecipe
import java.util.concurrent.TimeUnit

/**
 * FeedRankingAlgorithm - TikTok/Instagram-style feed ranking
 * 
 * 🔥 RANKING FORMULA:
 * score = (likes * 2) + (comments * 3) + mutualFollowerBonus + recentBonus + socialPriorityBonus
 * 
 * This creates viral mechanics where:
 * - Popular recipes rise to top
 * - Social recipes get priority
 * - New posts don't die immediately
 * - Mutual follower content gets massive boost
 */
object FeedRankingAlgorithm {
    
    // Scoring weights - these can be tuned based on analytics
    private const val LIKE_WEIGHT = 2.0
    private const val COMMENT_WEIGHT = 3.0
    private const val SAVE_WEIGHT = 1.5
    private const val RECENCY_HALFLIFE_HOURS = 24.0 // 24-hour half-life for recency
    private const val MUTUAL_FOLLOWER_BONUS = 50.0
    private const val FOLLOWER_BONUS = 20.0
    private const val NEW_RECIPE_BONUS_HOURS = 6.0
    private const val NEW_RECIPE_BONUS = 15.0
    
    /**
     * Calculate ranking score for a recipe
     * Higher score = higher position in feed
     */
    fun calculateScore(recipe: SocialRecipe, currentUserId: String): Double {
        val baseScore = calculateBaseScore(recipe)
        val socialBonus = calculateSocialBonus(recipe, currentUserId)
        val recencyBonus = calculateRecencyBonus(recipe)
        val newRecipeBonus = calculateNewRecipeBonus(recipe)
        
        val totalScore = baseScore + socialBonus + recencyBonus + newRecipeBonus
        
        // Log for debugging
        android.util.Log.d("FeedRanking", "Recipe: ${recipe.title}, Score: $totalScore " +
                "(Base: $baseScore, Social: $socialBonus, Recency: $recencyBonus, New: $newRecipeBonus)")
        
        return totalScore
    }
    
    /**
     * Base score from social interactions
     */
    private fun calculateBaseScore(recipe: SocialRecipe): Double {
        return (recipe.likes * LIKE_WEIGHT) +
               (recipe.comments * COMMENT_WEIGHT) +
               (recipe.saves * SAVE_WEIGHT)
    }
    
    /**
     * Social priority bonus based on relationship with author
     */
    private fun calculateSocialBonus(recipe: SocialRecipe, currentUserId: String): Double {
        return when {
            recipe.isFromMutualFollower -> MUTUAL_FOLLOWER_BONUS
            recipe.isFromFollower -> FOLLOWER_BONUS
            else -> 0.0
        }
    }
    
    /**
     * Recency bonus - newer content gets boost
     * Uses exponential decay with 24-hour half-life
     */
    private fun calculateRecencyBonus(recipe: SocialRecipe): Double {
        val now = System.currentTimeMillis()
        val recipeTime = recipe.createdAt?.time ?: now
        val hoursAgo = (now - recipeTime) / (1000 * 60 * 60).toDouble()
        
        // Exponential decay: bonus = initialBonus * (0.5 ^ (hoursAgo / halfLife))
        val initialBonus = 10.0
        val decayFactor = Math.pow(0.5, hoursAgo / RECENCY_HALFLIFE_HOURS)
        
        return initialBonus * decayFactor
    }
    
    /**
     * Bonus for very new recipes to prevent them from dying immediately
     */
    private fun calculateNewRecipeBonus(recipe: SocialRecipe): Double {
        val now = System.currentTimeMillis()
        val recipeTime = recipe.createdAt?.time ?: now
        val hoursAgo = (now - recipeTime) / (1000 * 60 * 60).toDouble()
        
        return if (hoursAgo < NEW_RECIPE_BONUS_HOURS) {
            NEW_RECIPE_BONUS * (1.0 - hoursAgo / NEW_RECIPE_BONUS_HOURS)
        } else {
            0.0
        }
    }
    
    /**
     * Sort recipes by ranking score
     */
    fun sortRecipes(recipes: List<SocialRecipe>, currentUserId: String): List<SocialRecipe> {
        return recipes.sortedByDescending { recipe ->
            calculateScore(recipe, currentUserId)
        }
    }
    
    /**
     * Batch calculate scores for multiple recipes
     */
    fun calculateScores(recipes: List<SocialRecipe>, currentUserId: String): Map<String, Double> {
        return recipes.associate { recipe ->
            recipe.recipeId to calculateScore(recipe, currentUserId)
        }
    }
    
    /**
     * Predict viral potential (for analytics)
     * High viral score means likely to perform well
     */
    fun calculateViralPotential(recipe: SocialRecipe): Double {
        val engagementRate = if (recipe.likes > 0) {
            (recipe.likes + recipe.comments * 2 + recipe.saves) / recipe.likes.toDouble()
        } else 0.0
        
        val socialMultiplier = when {
            recipe.isFromMutualFollower -> 1.5
            recipe.isFromFollower -> 1.2
            else -> 1.0
        }
        
        return engagementRate * socialMultiplier
    }
    
    /**
     * Quality score based on recipe completeness
     * Used to filter out low-quality content
     */
    fun calculateQualityScore(recipe: SocialRecipe): Double {
        var score = 0.0
        
        // Has image
        if (!recipe.imageUrl.isNullOrEmpty()) score += 10.0
        
        // Has description
        if (!recipe.description.isNullOrEmpty() && recipe.description.length > 50) score += 5.0
        
        // Has ingredients
        if (!recipe.ingredients.isNullOrEmpty()) score += recipe.ingredients.size * 2.0
        
        // Has steps
        if (!recipe.steps.isNullOrEmpty()) score += recipe.steps.size * 1.5
        
        // Has metadata
        if (recipe.cookTime > 0) score += 3.0
        if (recipe.calories > 0) score += 2.0
        if (!recipe.tags.isNullOrEmpty()) score += recipe.tags.size * 1.0
        
        return score
    }
    
    /**
     * Filter low-quality recipes
     */
    fun filterQualityRecipes(recipes: List<SocialRecipe>): List<SocialRecipe> {
        return recipes.filter { recipe ->
            calculateQualityScore(recipe) >= 15.0 // Minimum quality threshold
        }
    }
    
    /**
     * Get trending recipes (high growth rate)
     */
    fun getTrendingRecipes(recipes: List<SocialRecipe>, timeWindowHours: Long = 24): List<SocialRecipe> {
        val now = System.currentTimeMillis()
        val windowStart = now - TimeUnit.HOURS.toMillis(timeWindowHours)
        
        return recipes.filter { recipe ->
            val createdAt = recipe.createdAt?.time ?: 0
            createdAt >= windowStart && calculateViralPotential(recipe) > 2.0
        }.sortedByDescending { recipe ->
            calculateViralPotential(recipe)
        }
    }
}
