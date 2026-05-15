package com.example.kitchenbrain.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response model for News API
 */
public class NewsResponse {
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("totalResults")
    private int totalResults;
    
    @SerializedName("articles")
    private List<Article> articles;

    /** Present when {@code status} is {@code "error"} (e.g. apiKeyInvalid). */
    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;
    
    /**
     * Get the status of the response
     * @return "ok" if successful, or "error" if failed
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Get total number of results available
     * @return Total count
     */
    public int getTotalResults() {
        return totalResults;
    }
    
    /**
     * Get list of articles
     * @return List of Article objects
     */
    public List<Article> getArticles() {
        return articles;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
