package com.example.kitchenbrain.api;

import com.example.kitchenbrain.models.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit interface for NewsAPI - Production Ready
 */
public interface NewsApiService {
    
    /**
     * Search for everything with specific food filters and domain restrictions
     * 
     * @param query The search query
     * @param queryInTitle Optional search query restricted to article titles
     * @param domains Comma-separated list of domains to restrict search to
     * @param language Language of the articles (en)
     * @param sortBy How to sort (publishedAt, relevancy, popularity)
     * @param pageSize Number of results per page
     * @param page Page number for pagination
     * @param apiKey NewsAPI key
     */
    @GET("v2/everything")
    Call<NewsResponse> getFoodNews(
        @Query("q") String query,
        @Query("qInTitle") String queryInTitle,
        @Query("domains") String domains,
        @Query("language") String language,
        @Query("sortBy") String sortBy,
        @Query("pageSize") int pageSize,
        @Query("page") int page,
        @Query("apiKey") String apiKey
    );
}
