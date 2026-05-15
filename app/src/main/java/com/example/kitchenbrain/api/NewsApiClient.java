package com.example.kitchenbrain.api;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.kitchenbrain.BuildConfig;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit client for NewsAPI
 * 
 * ✅ Features:
 * - OkHttp logging interceptor for debugging
 * - Timeout handling (30s connect, 30s read)
 * - Singleton pattern
 */
public class NewsApiClient {
    
    private static final String BASE_URL = "https://newsapi.org/";
    private static NewsApiService apiService;
    
    /**
     * Get singleton instance of NewsApiService
     */
    public static synchronized NewsApiService getApiService() {
        if (apiService == null) {
            // ✅ Add logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                android.util.Log.d("NEWS_API_HTTP", message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // ✅ Add timeout handling + NO API Key interceptor (using query parameter)
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        // 🔥 DEBUG: Log request details (NO header manipulation)
                        Request request = chain.request();
                        String finalUrl = request.url().toString();
                        Log.d("NEWS_API", "🌐 URL: " + finalUrl);
                        Log.d("NEWS_API", "📋 METHOD: " + request.method());
                        Log.d("NEWS_API", "📋 AUTH: Query parameter (apiKey=...) in URL");
                        Log.d("NEWS_API", "🔍 FINAL URL = " + finalUrl);
                        
                        return chain.proceed(request);
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true) // ✅ Auto-retry on connection failure
                    .build();
            
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            
            apiService = retrofit.create(NewsApiService.class);
        }
        return apiService;
    }
}
