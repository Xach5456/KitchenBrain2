package com.example.kitchenbrain.database

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface RecipeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: CachedRecipe)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<CachedRecipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipesSync(recipes: List<CachedRecipe>)
    
    @Query("SELECT * FROM cached_recipes ORDER BY score DESC, timestamp DESC")
    fun getPagedRecipes(): PagingSource<Int, CachedRecipe>

    @Query("SELECT * FROM cached_recipes ORDER BY score DESC, timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecipes(limit: Int, offset: Int): List<CachedRecipe>

    @Query("SELECT * FROM cached_recipes ORDER BY timestamp DESC LIMIT :limit")
    fun getRandomRecipesSync(limit: Int): List<CachedRecipe>
    
    @Query("SELECT * FROM cached_recipes WHERE recipeId = :recipeId")
    suspend fun getRecipeById(recipeId: String): CachedRecipe?
    
    @Query("SELECT * FROM cached_recipes WHERE authorId = :authorId ORDER BY timestamp DESC")
    fun getUserRecipes(authorId: String): LiveData<List<CachedRecipe>>
    
    @Query("SELECT cached_recipes.* FROM cached_recipes INNER JOIN saved_recipes ON cached_recipes.recipeId = saved_recipes.recipeId WHERE saved_recipes.userId = :userId ORDER BY saved_recipes.savedAt DESC")
    fun getSavedRecipes(userId: String): LiveData<List<CachedRecipe>>
    
    @Query("DELETE FROM cached_recipes")
    suspend fun clearAllRecipes()
    
    @Query("SELECT COUNT(*) FROM cached_recipes")
    suspend fun getRecipeCount(): Int
}

@Entity(tableName = "cached_recipes")
data class CachedRecipe(
    @PrimaryKey val recipeId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String?,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val ingredients: List<String>, 
    val steps: List<String>, 
    val cookTime: Int,
    val difficulty: String,
    val calories: Int,
    val servings: Int,
    val tags: List<String>, 
    val likes: Long,
    val comments: Long,
    val saves: Long,
    val likedBy: Map<String, Boolean>, 
    val savedBy: Map<String, Boolean>,
    val socialPriority: String,
    val isFromMutualFollower: Boolean,
    val isFromFollower: Boolean,
    val score: Double,
    val timestamp: Long,
    val updatedAt: Long
)

@Entity(tableName = "saved_recipes", primaryKeys = ["userId", "recipeId"])
data class SavedRecipe(
    val userId: String,
    val recipeId: String,
    val savedAt: Long
)

class RecipeTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")
    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")
    @TypeConverter
    fun fromStringMap(value: Map<String, Boolean>): String = value.entries.joinToString(";") { "${it.key}:${it.value}" }
    @TypeConverter
    fun toStringMap(value: String): Map<String, Boolean> {
        if (value.isEmpty()) return emptyMap()
        return value.split(";").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1].toBoolean() else null
        }.toMap()
    }
}
