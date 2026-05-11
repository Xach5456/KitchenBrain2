package com.example.kitchenbrain.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.kitchenbrain.BuildConfig;
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
 * Production-Ready News Repository for Kitchen Brain.
 * 
 * Implements:
 * 1. CONTENT FILTERING (Food/Recipes/Cooking)
 * 2. STALE-WHILE-REVALIDATE Strategy (Instant UI, Background Sync)
 * 3. OFFLINE-FIRST PERSISTENT CACHE (SharedPreferences JSON)
 * 4. RATE LIMIT PROTECTION (15-min throttle)
 * 5. ERROR RESILIENCE (Fallback to cache on 401, 429, or Network failure)
 */
public class NewsRepository {
    private static final String TAG = "NewsRepository";
    private static final String PREFS_NAME = "kb_news_cache_v3";
    private static final String KEY_ARTICLES = "cached_articles_json";
    private static final String KEY_TIMESTAMP = "last_fetch_timestamp";
    
    // Search constraints
    private static final String SEARCH_QUERY = "food OR recipe OR cooking OR restaurant";
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(15);
    private static final int PAGE_SIZE = 40;
    
    private final NewsApiService apiService;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    // In-memory cache for ultra-fast session updates
    private static final List<Article> memoryCache = new ArrayList<>();
    private static long lastMemoryUpdateMs = 0;

    public NewsRepository(Context context) {
        this.apiService = NewsApiClient.getApiService();
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        // Auto-hydrate memory cache from disk on first instantiation
        if (memoryCache.isEmpty()) {
            loadFromDisk();
        }
    }

    public interface NewsCallback {
        void onSuccess(List<Article> articles);
        void onLoading();
        void onEmpty();
        void onError(String error, List<Article> fallbackData);
    }

    /**
     * Production Fetch Strategy:
     * 1. Returns cached data immediately if available (Instant UI).
     * 2. Checks throttle: if fresh (<15m), skips network.
     * 3. Executes network call if stale or forced.
     * 4. Updates cache on success, fallbacks on failure.
     */
    public void getNews(int page, boolean forceRefresh, final NewsCallback callback) {
        long now = System.currentTimeMillis();
        long lastFetch = prefs.getLong(KEY_TIMESTAMP, 0);
        boolean isStale = (now - lastMemoryUpdateMs) > CACHE_DURATION;
        boolean isThrottled = (now - lastFetch) < CACHE_DURATION && !forceRefresh;

        // ✅ STEP 1: INSTANT CACHE RETURN
        if (page == 1 && !memoryCache.isEmpty()) {
            Log.d(TAG, "📦 [CACHE] Returning " + memoryCache.size() + " articles from memory");
            callback.onSuccess(new ArrayList<>(memoryCache));
            
            // Return early if fresh enough
            if (!isStale && !forceRefresh) {
                Log.d(TAG, "⏱️ [THROTTLE] Cache is fresh, background sync skipped.");
                return;
            }
            
            // Prevent API spam if recently fetched successfully
            if (isThrottled) {
                Log.w(TAG, "🛑 [RATE_LIMIT] Throttled. Background update skipped to save API credits.");
                return;
            }
        } else if (page == 1) {
            callback.onLoading();
        }

        // ✅ STEP 2: NETWORK UPDATE
        executeApiCall(page, callback);
    }

    private void executeApiCall(int page, final NewsCallback callback) {
        String apiKey = BuildConfig.NEWS_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "❌ [CONFIG] NEWS_API_KEY is missing from BuildConfig!");
            callback.onError("API key not configured", memoryCache);
            return;
        }

        Log.d(TAG, "🌐 [API] Requesting page " + page + " for: " + SEARCH_QUERY);
        apiService.getFoodNews(SEARCH_QUERY, "en", "publishedAt", PAGE_SIZE, page, apiKey)
                .enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                // Requirement: Log full URL for debugging
                Log.d(TAG, "🌐 [DEBUG_URL] " + call.request().url());

                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        processApiResponse(articles, page, callback);
                    } else if (page == 1) {
                        Log.w(TAG, "⚠️ [API] Response was success but articles list is empty.");
                        callback.onEmpty();
                    }
                } else {
                    handleApiError(response.code(), callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ [NETWORK] Failed: " + t.getMessage());
                callback.onError("Network failure. Check connection.", memoryCache);
            }
        });
    }

    private void processApiResponse(List<Article> articles, int page, NewsCallback callback) {
        // CONTENT FILTERING: Ensure articles are relevant and have required visual data
        List<Article> filtered = new ArrayList<>();
        String[] foodKeywords = {"food", "recipe", "cook", "restaurant", "eat", "chef", "dish", "meal", "kitchen", "culinary", "baking"};
        
        for (Article a : articles) {
            // Must have title, image, and url
            if (a.getTitle() == null || a.getUrlToImage() == null || a.getUrl() == null) continue;
            
            // Secondary filter to remove noise from "v2/everything"
            String compositeText = (a.getTitle() + " " + (a.getDescription() != null ? a.getDescription() : "")).toLowerCase();
            boolean isRelevant = false;
            for (String keyword : foodKeywords) {
                if (compositeText.contains(keyword)) {
                    isRelevant = true;
                    break;
                }
            }
            
            if (isRelevant) filtered.add(a);
        }

        // Update persistence if it's the first page
        if (page == 1) {
            synchronized (memoryCache) {
                memoryCache.clear();
                memoryCache.addAll(filtered);
            }
            lastMemoryUpdateMs = System.currentTimeMillis();
            saveToDisk(filtered);
        }
        
        Log.d(TAG, "✅ [SUCCESS] Received " + filtered.size() + " filtered articles.");
        callback.onSuccess(filtered);
    }

    private void handleApiError(int code, NewsCallback callback) {
        String errorMessage;
        switch (code) {
            case 401: errorMessage = "API Key Unauthorized. Verify local.properties."; break;
            case 429: errorMessage = "Rate limit reached. Try again in 15 mins."; break;
            case 500: errorMessage = "NewsAPI server error (500)."; break;
            default: errorMessage = "API Request failed with code: " + code; break;
        }
        Log.e(TAG, "❌ [API_ERROR] " + code + ": " + errorMessage);
        callback.onError(errorMessage, memoryCache);
    }

    // ===== PERSISTENCE LAYER =====

    private void saveToDisk(List<Article> articles) {
        new Thread(() -> {
            try {
                String json = gson.toJson(articles);
                prefs.edit()
                    .putString(KEY_ARTICLES, json)
                    .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                    .apply();
                Log.d(TAG, "💾 [DISK] Cache updated with " + articles.size() + " items.");
            } catch (Exception e) {
                Log.e(TAG, "❌ [DISK] Save error: " + e.getMessage());
            }
        }).start();
    }

    private void loadFromDisk() {
        String json = prefs.getString(KEY_ARTICLES, null);
        if (json != null) {
            try {
                Type type = new TypeToken<ArrayList<Article>>(){}.getType();
                List<Article> diskArticles = gson.fromJson(json, type);
                if (diskArticles != null) {
                    synchronized (memoryCache) {
                        memoryCache.clear();
                        memoryCache.addAll(diskArticles);
                    }
                    lastMemoryUpdateMs = prefs.getLong(KEY_TIMESTAMP, 0);
                    Log.d(TAG, "📖 [DISK] Hydrated memory cache: " + memoryCache.size() + " items.");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ [DISK] Load error: " + e.getMessage());
            }
        }
    }
}
