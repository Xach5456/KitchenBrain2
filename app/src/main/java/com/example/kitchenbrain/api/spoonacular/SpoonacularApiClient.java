package com.example.kitchenbrain.api.spoonacular;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class SpoonacularApiClient {
    private static final String TAG = "Spoonacular_HTTP";
    private static final String BASE_URL = "https://api.spoonacular.com/";
    private static SpoonacularApiService apiService;

    public static synchronized SpoonacularApiService getApiService() {
        if (apiService == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                Log.d(TAG, message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        // 🔥 DEBUG URL
                        Log.d(TAG, "🌐 Request URL: " + request.url());
                        return chain.proceed(request);
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(SpoonacularApiService.class);
        }
        return apiService;
    }
}
