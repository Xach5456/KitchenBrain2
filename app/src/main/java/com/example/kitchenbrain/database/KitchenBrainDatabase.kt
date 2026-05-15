package com.example.kitchenbrain.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * KitchenBrainDatabase - Room database for caching
 * 
 * 🔥 CACHING ARCHITECTURE:
 * - Repository -> Room Cache -> UI flow
 * - Offline-first design
 * - Reduces Firestore costs
 */
@Database(
    entities = [
        CachedRecipe::class,
        SavedRecipe::class,
        NewsArticleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RecipeTypeConverters::class)
abstract class KitchenBrainDatabase : RoomDatabase() {
    
    abstract fun recipeDao(): RecipeDao
    abstract fun newsDao(): NewsDao
    
    companion object {
        @Volatile
        private var INSTANCE: KitchenBrainDatabase? = null
        
        fun getDatabase(context: Context): KitchenBrainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenBrainDatabase::class.java,
                    "kitchen_brain_database"
                )
                .fallbackToDestructiveMigration() // For development simplicity
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
