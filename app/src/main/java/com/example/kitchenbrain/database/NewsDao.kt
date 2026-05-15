package com.example.kitchenbrain.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * NewsDao - Room DAO for news caching
 */
@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNewsSync(news: List<NewsArticleEntity>)

    @Query("SELECT * FROM cached_news ORDER BY publishedAt DESC LIMIT 200")
    fun getAllNewsFlow(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM cached_news ORDER BY publishedAt DESC LIMIT 200")
    suspend fun getAllNews(): List<NewsArticleEntity>

    @Query("SELECT * FROM cached_news ORDER BY publishedAt DESC LIMIT 200")
    fun getAllNewsSync(): List<NewsArticleEntity>

    @Query("DELETE FROM cached_news WHERE url NOT IN (SELECT url FROM cached_news ORDER BY publishedAt DESC LIMIT 200)")
    suspend fun deleteOldNews()

    @Query("DELETE FROM cached_news")
    suspend fun clearAllNews()

    @Query("SELECT COUNT(*) FROM cached_news")
    suspend fun getNewsCount(): Int

    @Query("SELECT * FROM cached_news ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomNews(limit: Int): List<NewsArticleEntity>
}
