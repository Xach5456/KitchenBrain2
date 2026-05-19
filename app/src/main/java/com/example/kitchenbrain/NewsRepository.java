package com.example.kitchenbrain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.kitchenbrain.api.NewsApiClient;
import com.example.kitchenbrain.api.NewsApiService;
import com.example.kitchenbrain.models.Article;
import com.example.kitchenbrain.models.NewsResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production-Ready News Repository - Telegram-Grade Feed Logic
 */
public class NewsRepository {
    private static final String TAG = "NewsRepository";
    private static final String PREFS_NAME = "news_cache_prod";
    private static final String KEY_ARTICLES = "cached_articles";
    private static final String KEY_TIMESTAMP = "last_fetch_ms";
    
    // Requirements: Strictly Food, Cook, Restaurant/Restoran
    private static final String FOOD_QUERY = "food OR recipe OR cooking OR restaurant OR kitchen OR chef OR meal OR dish OR baking OR culinary";
    private static final String DOMAINS = "allrecipes.com,seriouseats.com,bonappetit.com,foodnetwork.com,simplyrecipes.com,epicurious.com,thekitchn.com,food52.com,delish.com,cookinglight.com,bettycrocker.com,tasteofhome.com,bbcgoodfood.com,nytimes.com";
    private static final long CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(15); 
    private static final int PAGE_SIZE = 50;
    
    private final NewsApiService apiService;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    private static final List<Article> memoryCache = new ArrayList<>();
    private static long lastUpdateMs = 0;

    public NewsRepository(Context context) {
        this.apiService = NewsApiClient.getApiService();
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        if (memoryCache.isEmpty()) {
            loadFromDisk();
        }
    }

    public interface NewsCallback {
        void onSuccess(List<Article> articles);
        void onEmpty();
        void onError(String error);
    }

    public void fetchNews(final NewsCallback callback) {
        fetchNews(1, false, callback);
    }

    public void fetchNews(int page, boolean forceRefresh, final NewsCallback callback) {
        long now = System.currentTimeMillis();
        long lastFetch = prefs.getLong(KEY_TIMESTAMP, 0);
        boolean isStale = (now - lastUpdateMs) > CACHE_EXPIRATION;
        boolean isThrottled = (now - lastFetch) < CACHE_EXPIRATION && !forceRefresh;

        if (page == 1 && !memoryCache.isEmpty()) {
            callback.onSuccess(new ArrayList<>(memoryCache));
            if (!isStale && !forceRefresh) return;
            if (isThrottled && !forceRefresh) return;
        }

        executeApiCall(page, callback);
    }

    private void executeApiCall(int page, final NewsCallback callback) {
        String apiKey = BuildConfig.NEWS_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not configured");
            return;
        }

        // 🔥 FIXED: Passing all 8 required parameters (q, qInTitle, domains, language, sortBy, pageSize, page, apiKey)
        apiService.getFoodNews(FOOD_QUERY, FOOD_QUERY, DOMAINS, "en", "publishedAt", PAGE_SIZE, page, apiKey)
                .enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        handleSuccess(articles, page, callback);
                    } else if (page == 1) {
                        callback.onEmpty();
                    }
                } else {
                    handleError(response.code(), callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                if (memoryCache.isEmpty()) callback.onError("Network error");
            }
        });
    }

    private void handleSuccess(List<Article> freshArticles, int page, NewsCallback callback) {
        // 🔥 STRICT FILTERING: Food, Cook, Restaurant/Restoran
        List<Article> filtered = new ArrayList<>();
        for (Article article : freshArticles) {
            if (isFoodNews(article)) filtered.add(article);
        }

        if (page == 1) {
            synchronized (memoryCache) {
                memoryCache.clear();
                memoryCache.addAll(filtered);
            }
            lastUpdateMs = System.currentTimeMillis();
            persistToDisk(filtered);
        }
        
        callback.onSuccess(filtered);
    }

    private void handleError(int code, NewsCallback callback) {
        if (memoryCache.isEmpty()) callback.onError("Server error (" + code + ")");
    }

    private void persistToDisk(List<Article> articles) {
        new Thread(() -> {
            try {
                String json = gson.toJson(articles);
                prefs.edit()
                    .putString(KEY_ARTICLES, json)
                    .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                    .apply();
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadFromDisk() {
        String json = prefs.getString(KEY_ARTICLES, null);
        if (json != null) {
            try {
                Type type = new TypeToken<ArrayList<Article>>(){}.getType();
                List<Article> diskList = gson.fromJson(json, type);
                if (diskList != null) {
                    synchronized (memoryCache) {
                        memoryCache.clear();
                        for (Article article : diskList) {
                            if (isFoodNews(article)) memoryCache.add(article);
                        }
                    }
                    lastUpdateMs = prefs.getLong(KEY_TIMESTAMP, 0);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean isFoodNews(Article article) {
        if (article == null || article.getTitle() == null || article.getUrl() == null || article.getUrl().isEmpty()) return false;
        String text = (
                safe(article.getTitle()) + " " +
                safe(article.getDescription()) + " " +
                safe(article.getContent()) + " " +
                (article.getSource() != null ? safe(article.getSource().getName()) : "")
        ).toLowerCase();

        String[] keywords = {
                "food", "recipe", "cooking", "kitchen", "restaurant", "meal", "dish",
                "chef", "breakfast", "lunch", "dinner", "baking", "culinary", "cuisine",
                "ingredient", "grocery", "dining", "nutrition"
        };

        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
