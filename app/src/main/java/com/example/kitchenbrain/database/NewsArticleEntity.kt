package com.example.kitchenbrain.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NewsArticleEntity - Room entity for news caching
 * 
 * ✅ PRODUCTION-READY:
 * - Stores server articles locally
 * - Supports offline mode
 * - Includes timestamp for cache expiration
 */
@Entity(tableName = "cached_news")
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val author: String?,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val content: String?,
    val sourceName: String?,
    val timestamp: Long = System.currentTimeMillis()
)
