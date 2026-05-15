package com.example.kitchenbrain.repository;

import android.content.Context;
import android.util.Log;
import com.example.kitchenbrain.BuildConfig;
import com.example.kitchenbrain.api.spoonacular.SpoonacularApiClient;
import com.example.kitchenbrain.api.spoonacular.SpoonacularApiService;
import com.example.kitchenbrain.api.spoonacular.SpoonacularRecipe;
import com.example.kitchenbrain.api.spoonacular.SpoonacularResponse;
import com.example.kitchenbrain.database.CachedRecipe;
import com.example.kitchenbrain.database.KitchenBrainDatabase;
import com.example.kitchenbrain.database.RecipeDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SpoonacularRepository {
    private static final String TAG = "SpoonacularRepo";
    private final SpoonacularApiService apiService;
    private final String apiKey;
    private final RecipeDao recipeDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SpoonacularRepository(Context context) {
        this.apiService = SpoonacularApiClient.getApiService();
        this.apiKey = BuildConfig.SPOONACULAR_API_KEY;
        this.recipeDao = KitchenBrainDatabase.Companion.getDatabase(context).recipeDao();
        
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "❌ SPOONACULAR_API_KEY is EMPTY!");
        }
    }

    public interface SpoonacularCallback<T> {
        void onSuccess(T result, boolean isFromCache);
        void onError(String error);
    }

    public void getRandomRecipes(int number, SpoonacularCallback<List<SpoonacularRecipe>> callback) {
        Log.d(TAG, "📡 Fetching random recipes, count: " + number);
        apiService.getRandomRecipes(number, apiKey).enqueue(new Callback<SpoonacularResponse>() {
            @Override
            public void onResponse(Call<SpoonacularResponse> call, Response<SpoonacularResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SpoonacularRecipe> recipes = response.body().recipes;
                    Log.d(TAG, "✅ Success: received " + recipes.size() + " recipes");
                    saveToCache(recipes);
                    callback.onSuccess(recipes, false);
                } else {
                    handleError(response.code(), response.message(), number, callback);
                }
            }

            @Override
            public void onFailure(Call<SpoonacularResponse> call, Throwable t) {
                Log.e(TAG, "❌ Network Failure", t);
                loadFromCache(number, callback, "Network failure: " + t.getMessage());
            }
        });
    }

    private void handleError(int code, String message, int number, SpoonacularCallback<List<SpoonacularRecipe>> callback) {
        String errorMsg;
        if (code == 402) {
            errorMsg = "Daily API quota reached (402). Showing cached recipes.";
        } else if (code == 401) {
            errorMsg = "Invalid API Key (401).";
        } else {
            errorMsg = "API Error " + code + ": " + message;
        }
        Log.e(TAG, "❌ API Error: " + errorMsg);
        loadFromCache(number, callback, errorMsg);
    }

    private void saveToCache(List<SpoonacularRecipe> recipes) {
        executor.execute(() -> {
            List<CachedRecipe> cachedRecipes = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (SpoonacularRecipe s : recipes) {
                List<String> ingredients = new ArrayList<>();
                if (s.extendedIngredients != null) {
                    for (SpoonacularRecipe.Ingredient i : s.extendedIngredients) {
                        ingredients.add(i.original);
                    }
                }
                
                cachedRecipes.add(new CachedRecipe(
                        "spoon_" + s.id,
                        "spoonacular",
                        "Spoonacular",
                        null,
                        s.title,
                        s.summary != null ? s.summary : "",
                        s.image,
                        ingredients,
                        Collections.singletonList(s.instructions != null ? s.instructions : ""),
                        s.readyInMinutes,
                        "Medium",
                        0,
                        s.servings,
                        new ArrayList<>(),
                        (long) s.aggregateLikes,
                        0L,
                        0L,
                        new HashMap<>(),
                        new HashMap<>(),
                        "global",
                        false,
                        false,
                        (double) s.aggregateLikes,
                        now,
                        now
                ));
            }
            try {
                recipeDao.insertRecipesSync(cachedRecipes);
                Log.d(TAG, "💾 Saved " + cachedRecipes.size() + " recipes to cache");
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to save to cache: " + e.getMessage());
            }
        });
    }

    private void loadFromCache(int number, SpoonacularCallback<List<SpoonacularRecipe>> callback, String originalError) {
        executor.execute(() -> {
            try {
                List<CachedRecipe> cached = recipeDao.getRandomRecipesSync(number);
                if (cached != null && !cached.isEmpty()) {
                    List<SpoonacularRecipe> mapped = new ArrayList<>();
                    for (CachedRecipe c : cached) {
                        mapped.add(mapCachedToSpoonacular(c));
                    }
                    Log.d(TAG, "📖 Loaded " + mapped.size() + " recipes from cache");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onSuccess(mapped, true)
                    );
                } else {
                    Log.w(TAG, "⚠️ Cache is empty, reporting original error");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onError(originalError)
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to load from cache: " + e.getMessage());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    callback.onError(originalError)
                );
            }
        });
    }

    private SpoonacularRecipe mapCachedToSpoonacular(CachedRecipe c) {
        SpoonacularRecipe s = new SpoonacularRecipe();
        try {
            if (c.getRecipeId().startsWith("spoon_")) {
                s.id = Integer.parseInt(c.getRecipeId().substring(6));
            } else {
                s.id = c.getRecipeId().hashCode();
            }
        } catch (NumberFormatException e) {
            s.id = c.getRecipeId().hashCode();
        }
        s.title = c.getTitle();
        s.image = c.getImageUrl();
        s.readyInMinutes = c.getCookTime();
        s.summary = c.getDescription();
        if (c.getSteps() != null && !c.getSteps().isEmpty()) {
            s.instructions = c.getSteps().get(0);
        }
        s.servings = c.getServings();
        s.aggregateLikes = (int) c.getLikes();
        
        if (c.getIngredients() != null) {
            s.extendedIngredients = new ArrayList<>();
            for (String ing : c.getIngredients()) {
                SpoonacularRecipe.Ingredient i = new SpoonacularRecipe.Ingredient();
                i.original = ing;
                s.extendedIngredients.add(i);
            }
        }
        return s;
    }

    public void getRecipeInformation(int id, SpoonacularCallback<SpoonacularRecipe> callback) {
        apiService.getRecipeInformation(id, apiKey).enqueue(new Callback<SpoonacularRecipe>() {
            @Override
            public void onResponse(Call<SpoonacularRecipe> call, Response<SpoonacularRecipe> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body(), false);
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SpoonacularRecipe> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}
