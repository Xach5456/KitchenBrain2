package com.example.kitchenbrain.repository;

import android.util.Log;
import com.example.kitchenbrain.BuildConfig;
import com.example.kitchenbrain.api.spoonacular.SpoonacularApiClient;
import com.example.kitchenbrain.api.spoonacular.SpoonacularApiService;
import com.example.kitchenbrain.api.spoonacular.SpoonacularRecipe;
import com.example.kitchenbrain.api.spoonacular.SpoonacularResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SpoonacularRepository {
    private static final String TAG = "SpoonacularRepo";
    private final SpoonacularApiService apiService;
    private final String apiKey;

    public SpoonacularRepository() {
        this.apiService = SpoonacularApiClient.getApiService();
        this.apiKey = BuildConfig.SPOONACULAR_API_KEY;
        
        // DEBUG: Check if API key is loaded
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "❌ SPOONACULAR_API_KEY is EMPTY! Check local.properties and rebuild.");
        } else {
            Log.d(TAG, "✅ API Key loaded: " + apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4));
        }
    }

    public interface SpoonacularCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void getRandomRecipes(int number, SpoonacularCallback<List<SpoonacularRecipe>> callback) {
        Log.d(TAG, "📡 Fetching random recipes, count: " + number);
        apiService.getRandomRecipes(number, apiKey).enqueue(new Callback<SpoonacularResponse>() {
            @Override
            public void onResponse(Call<SpoonacularResponse> call, Response<SpoonacularResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "✅ Success: received " + response.body().recipes.size() + " recipes");
                    callback.onSuccess(response.body().recipes);
                } else {
                    String errorMsg = "Error " + response.code() + ": " + response.message();
                    Log.e(TAG, "❌ API Error: " + errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<SpoonacularResponse> call, Throwable t) {
                Log.e(TAG, "❌ Network Failure", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void getRecipeInformation(int id, SpoonacularCallback<SpoonacularRecipe> callback) {
        apiService.getRecipeInformation(id, apiKey).enqueue(new Callback<SpoonacularRecipe>() {
            @Override
            public void onResponse(Call<SpoonacularRecipe> call, Response<SpoonacularRecipe> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
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
