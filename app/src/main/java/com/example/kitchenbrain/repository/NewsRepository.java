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
 * Updated NewsRepository to use Room database.
 * 🔥 STRICT FILTERING: Now uses targeted keywords and culinary-only domains.
 */
public class NewsRepository {
    private static final String TAG = "NewsRepository";
    
    // 🎯 Keywords for strict food/cooking filtering
    private static final String SEARCH_QUERY = "food OR recipe OR cooking OR restaurant OR kitchen OR chef OR meal OR dish OR baking OR culinary";
    
    // 🌐 Trusted culinary domains only
    private static final String DOMAINS = "allrecipes.com,seriouseats.com,bonappetit.com,foodnetwork.com,simplyrecipes.com,epicurious.com,thekitchn.com,food52.com,delish.com,cookinglight.com,bettycrocker.com,tasteofhome.com,bbcgoodfood.com";
    private static final String[] FOOD_KEYWORDS = {
            "food", "recipe", "cooking", "kitchen", "restaurant", "meal", "dish",
            "chef", "breakfast", "lunch", "dinner", "baking", "culinary", "cuisine",
            "ingredient", "grocery", "dining", "nutrition"
    };
    
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

        executor.execute(() -> {
            try {
                List<NewsArticleEntity> cachedEntities = newsDao.getAllNewsSync();
                if (!cachedEntities.isEmpty()) {
                    List<Article> cachedArticles = mapEntitiesToArticles(cachedEntities);
                    if (!cachedArticles.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(cachedArticles));
                    }
                    
                    if (!forceRefresh) return; 
                }
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

        // qInTitle ensures the focus is on the article topic
        apiService.getFoodNews(SEARCH_QUERY, SEARCH_QUERY, DOMAINS, "en", "publishedAt", 40, page, apiKey)
                .enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsResponse> call, @NonNull Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getArticles();
                    if (articles != null && !articles.isEmpty()) {
                        List<Article> filteredArticles = filterFoodArticles(articles);
                        saveToRoom(filteredArticles);
                        mainHandler.post(() -> callback.onSuccess(filteredArticles));
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
                if (a.getUrl() == null || a.getUrl().isEmpty() || a.getTitle() == null || a.getTitle().isEmpty()) continue;
                entities.add(new NewsArticleEntity(
                    a.getUrl(), a.getTitle(), a.getAuthor(), a.getDescription(),
                    a.getUrlToImage(), a.getPublishedAt(), a.getContent(),
                    a.getSource() != null ? a.getSource().getName() : null, now
                ));
            }
            try {
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
        return filterFoodArticles(articles);
    }

    private List<Article> filterFoodArticles(List<Article> articles) {
        List<Article> filtered = new ArrayList<>();
        for (Article article : articles) {
            if (article == null || article.getUrl() == null || article.getUrl().isEmpty()) continue;
            if (article.getTitle() == null || article.getTitle().isEmpty()) continue;
            if (isFoodNews(article)) filtered.add(article);
        }
        return filtered;
    }

    private boolean isFoodNews(Article article) {
        String text = (
                safe(article.getTitle()) + " " +
                safe(article.getDescription()) + " " +
                safe(article.getContent()) + " " +
                (article.getSource() != null ? safe(article.getSource().getName()) : "")
        ).toLowerCase();

        for (String keyword : FOOD_KEYWORDS) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
