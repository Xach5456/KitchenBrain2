package com.example.kitchenbrain.repository

import android.content.Context
import android.util.Log
import com.example.kitchenbrain.BuildConfig
import com.example.kitchenbrain.api.NewsApiClient
import com.example.kitchenbrain.api.NewsApiService
import com.example.kitchenbrain.database.KitchenBrainDatabase
import com.example.kitchenbrain.database.NewsArticleEntity
import com.example.kitchenbrain.database.NewsDao
import com.example.kitchenbrain.models.Article
import com.example.kitchenbrain.models.NewsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * NewsRepositoryImpl - Production-grade news management
 * 
 * ✅ FEATURES:
 * - Offline-first (Room)
 * - API limit awareness
 * - Cache expiration (30 mins)
 * - Background sync compatible
 */
class NewsRepositoryImpl(context: Context) {
    private val newsDao: NewsDao = KitchenBrainDatabase.getDatabase(context).newsDao()
    private val apiService: NewsApiService = NewsApiClient.getApiService()
    
    private val CACHE_EXPIRATION_MS = TimeUnit.MINUTES.toMillis(30)
    // 🔥 ENHANCED: More specific culinary query to filter out non-food news
    private val FOOD_QUERY = "culinary OR gastronomy OR \"food recipes\" OR \"cooking tips\""

    /**
     * Get news stream from database
     */
    fun getNewsFlow(): Flow<List<NewsArticleEntity>> = newsDao.getAllNewsFlow()

    /**
     * Refresh news from API
     * @param force Force refresh even if cache is valid
     * @return Result of the operation
     */
    suspend fun refreshNews(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val lastNews = newsDao.getAllNews().firstOrNull()
            val isCacheValid = lastNews != null && (System.currentTimeMillis() - lastNews.timestamp) < CACHE_EXPIRATION_MS
            
            if (isCacheValid && !force) {
                Log.d("NewsRepo", "Using valid cache")
                return@withContext Result.success(Unit)
            }

            // 🎯 Improved sorting to 'relevancy' to ensure culinary focus
            val response = apiService.getFoodNews(
                FOOD_QUERY,
                "en",
                "relevancy",
                100,
                1,
                BuildConfig.NEWS_API_KEY
            ).execute()

            if (response.isSuccessful) {
                val articles = response.body()?.articles ?: emptyList()
                if (articles.isNotEmpty()) {
                    val entities = articles.map { it.toEntity() }
                    newsDao.clearAllNews()
                    newsDao.insertNews(entities)
                    newsDao.deleteOldNews() // Keep it under 200
                    return@withContext Result.success(Unit)
                }
                return@withContext Result.failure(Exception("Empty news response"))
            } else {
                if (response.code() == 429) {
                    return@withContext Result.failure(Exception("API Limit reached"))
                }
                return@withContext Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("NewsRepo", "Refresh failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Helper to convert API model to Room entity
     */
    private fun Article.toEntity(): NewsArticleEntity {
        return NewsArticleEntity(
            url = url ?: "",
            title = title,
            author = author,
            description = description,
            urlToImage = urlToImage,
            publishedAt = publishedAt,
            content = content,
            sourceName = source?.name,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Get random cached news for "Surprise Me" or filling gaps
     */
    suspend fun getRandomNews(limit: Int): List<NewsArticleEntity> = newsDao.getRandomNews(limit)
}
