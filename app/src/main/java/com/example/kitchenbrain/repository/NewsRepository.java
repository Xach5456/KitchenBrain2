package com.example.kitchenbrain.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.kitchenbrain.BuildConfig;
import com.example.kitchenbrain.api.NewsApiClient;
import com.example.kitchenbrain.api.NewsApiService;
import com.example.kitchenbrain.database.KitchenBrainDatabase;
import com.example.kitchenbrain.database.NewsArticleEntity;
import com.example.kitchenbrain.database.NewsDao;
import com.example.kitchenbrain.models.Article;
import com.example.kitchenbrain.models.NewsResponse;
import com.example.kitchenbrain.models.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Updated NewsRepository to use Room database (shared with NewsSyncWorker)
 * Uses synchronous DAO methods for Java compatibility.
 */
public class NewsRepository {
    private static final String TAG = "NewsRepository";
    // 🔥 ENHANCED: More specific culinary query to filter out non-food news
    private static final String SEARCH_QUERY = "culinary OR gastronomy OR \"food recipes\" OR \"cooking tips\"";
    
    private final NewsApiService apiService;
    private final NewsDao newsDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NewsRepository(Context context) {
        this.apiService = NewsApiClient.getApiService();
        this.newsDao = KitchenBrainDatabase.Companion.getDatabase(context).newsDao();
    }

    public interface NewsCallback {
        void onSuccess(List<Article> articles);
        void onLoading();
        void onEmpty();
        void onError(String error, List<Article> fallbackData);
    }

    public void getNews(int page, boolean forceRefresh, final NewsCallback callback) {
        if (page == 1) callback.onLoading();

        // Always try to load from Room cache first
        executor.execute(() -> {
            try {
                // Use Sync version for Java
                List<NewsArticleEntity> cachedEntities = newsDao.getAllNewsSync();
                if (!cachedEntities.isEmpty()) {
                    List<Article> cachedArticles = mapEntitiesToArticles(cachedEntities);
                    mainHandler.post(() -> callback.onSuccess(cachedArticles));
                    
                    if (!forceRefresh) return; 
                }
                
                // If cache empty or force refresh, hit the network
                executeApiCall(page, callback);
            } catch (Exception e) {
                Log.e(TAG, "Cache load failed", e);
                executeApiCall(page, callback);
            }
        });
    }

    private void executeApiCall(int page, final NewsCallback callback) {
        String apiKey = BuildConfig.NEWS_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("API key not configured", new ArrayList<>()));
            return;
        }

        // 🎯 Improved sorting to 'relevancy' to ensure culinary focus
        apiService.getFoodNews(SEARCH_QUERY, "en", "relevancy", 40, page, apiKey)
                .enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        saveToRoom(articles);
                        mainHandler.post(() -> callback.onSuccess(articles));
                    } else if (page == 1) {
                        mainHandler.post(callback::onEmpty);
                    }
                } else {
                    mainHandler.post(() -> callback.onError("API Error: " + response.code(), new ArrayList<>()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage(), new ArrayList<>()));
            }
        });
    }

    private void saveToRoom(List<Article> articles) {
        executor.execute(() -> {
            List<NewsArticleEntity> entities = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (Article a : articles) {
                entities.add(new NewsArticleEntity(
                    a.getUrl(), a.getTitle(), a.getAuthor(), a.getDescription(),
                    a.getUrlToImage(), a.getPublishedAt(), a.getContent(),
                    a.getSource() != null ? a.getSource().getName() : null, now
                ));
            }
            try {
                // Use Sync version for Java
                newsDao.insertNewsSync(entities);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save news to Room", e);
            }
        });
    }

    private List<Article> mapEntitiesToArticles(List<NewsArticleEntity> entities) {
        List<Article> articles = new ArrayList<>();
        for (NewsArticleEntity e : entities) {
            Article a = new Article();
            a.setUrl(e.getUrl());
            a.setTitle(e.getTitle());
            a.setAuthor(e.getAuthor());
            a.setDescription(e.getDescription());
            a.setUrlToImage(e.getUrlToImage());
            a.setPublishedAt(e.getPublishedAt());
            a.setContent(e.getContent());
            Source s = new Source();
            s.setName(e.getSourceName());
            a.setSource(s);
            articles.add(a);
        }
        return articles;
    }
}
