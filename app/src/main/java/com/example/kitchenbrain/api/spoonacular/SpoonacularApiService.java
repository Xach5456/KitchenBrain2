package com.example.kitchenbrain.api.spoonacular;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpoonacularApiService {

    @GET("recipes/random")
    Call<SpoonacularResponse> getRandomRecipes(
        @Query("number") int number,
        @Query("apiKey") String apiKey
    );

    @GET("recipes/{id}/information")
    Call<SpoonacularRecipe> getRecipeInformation(
        @Path("id") int id,
        @Query("apiKey") String apiKey
    );
}
