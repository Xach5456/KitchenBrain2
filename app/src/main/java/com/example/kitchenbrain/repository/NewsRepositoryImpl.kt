package com.example.kitchenbrain.repository

import android.content.Context
import android.util.Log
import com.example.kitchenbrain.BuildConfig
import com.example.kitchenbrain.api.NewsApiClient
import com.example.kitchenbrain.api.NewsApiService
import com.example.kitchenbrain.database.KitchenBrainDatabase
import com.example.kitchenbrain.database.NewsArticleEntity
import com.example.kitchenbrain.database.NewsDao
import com.example.kitchenbrain.database.toDomain
import com.example.kitchenbrain.models.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
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
    
    // 🔥 STRICT FILTERING: Focused strictly on food, cooking, and culinary arts
    private val FOOD_QUERY = "food OR recipe OR cooking OR restaurant OR kitchen OR chef OR meal OR dish OR baking OR culinary"
    
    // 🌐 TARGETED DOMAINS: Only fetch news from trusted culinary sources
    private val DOMAINS = "allrecipes.com,seriouseats.com,bonappetit.com,foodnetwork.com,simplyrecipes.com,epicurious.com,thekitchn.com,food52.com,delish.com,cookinglight.com,bettycrocker.com,tasteofhome.com,bbcgoodfood.com,nytimes.com"

    private val FOOD_KEYWORDS = listOf(
        "food", "recipe", "cooking", "kitchen", "restaurant", "meal", "dish",
        "chef", "breakfast", "lunch", "dinner", "baking", "culinary", "cuisine",
        "ingredient", "grocery", "dining", "nutrition"
    )

    /**
     * Get news stream from database
     */
    fun getNewsFlow(): Flow<List<Article>> {
        return newsDao.getAllNewsFlow().map { cachedArticles ->
            cachedArticles
                .map { it.toDomain() }
                .filter { it.isFoodNews() }
        }
    }

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

            // 🎯 Fixed: Added queryInTitle to match the updated NewsApiService interface
            val response = apiService.getFoodNews(
                FOOD_QUERY,      // q
                FOOD_QUERY,      // qInTitle
                DOMAINS,         // domains
                "en",            // language
                "relevancy",     // sortBy
                100,             // pageSize
                1,               // page
                BuildConfig.NEWS_API_KEY // apiKey
            ).execute()

            if (response.isSuccessful) {
                val articles = response.body()?.articles
                    ?.filter { it.hasRequiredNewsFields() }
                    ?.filter { it.isFoodNews() }
                    ?: emptyList()
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
            url = url,
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

    private fun Article.hasRequiredNewsFields(): Boolean {
        return !url.isNullOrBlank() && !title.isNullOrBlank()
    }

    private fun Article.isFoodNews(): Boolean {
        val searchableText = listOfNotNull(title, description, content, source?.name)
            .joinToString(separator = " ")
            .lowercase(Locale.US)

        return FOOD_KEYWORDS.any { searchableText.contains(it) }
    }

    /**
     * Get random cached news for "Surprise Me" or filling gaps
     */
    suspend fun getRandomNews(limit: Int): List<Article> {
        return newsDao.getRandomNews(limit)
            .map { it.toDomain() }
            .filter { it.isFoodNews() }
    }
}
