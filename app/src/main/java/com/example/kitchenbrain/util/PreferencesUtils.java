package com.example.kitchenbrain.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Preferences Utility Class
 * Handles local data caching for offline support
 */
public class PreferencesUtils {
    private static final String TAG = "PreferencesUtils";

    private static final String PREF_NAME = "news_preferences";
    private static final String KEY_LAST_UPDATE = "last_update_time";
    private static final String KEY_CACHED_NEWS = "cached_news_json";
    
    // Cache expiration time (2 hours) - PREVENT RATE LIMIT
    public static final long CACHE_EXPIRATION_MS = 2 * 60 * 60 * 1000L; // 2 hours

    /**
     * Save last update timestamp
     * @param context Application context
     * @param timestamp Time in milliseconds
     */
    public static void saveLastUpdateTime(Context context, long timestamp) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot save update time");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_UPDATE, timestamp).apply();
    }

    /**
     * Get last update timestamp
     * @param context Application context
     * @return Last update time in milliseconds, 0 if never updated
     */
    public static long getLastUpdateTime(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot get update time");
            return 0;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }

    /**
     * Cache news JSON data for offline viewing
     * @param context Application context
     * @param newsJson JSON string of news articles
     */
    public static void cacheNewsData(Context context, String newsJson) {
        // CRITICAL: Check for null context to prevent ANR/crash
        if (context == null) {
            Log.e(TAG, "Context is null, cannot cache news - skipping to prevent crash");
            return;
        }
        
        // Additional safety: check for valid JSON
        if (newsJson == null || newsJson.trim().isEmpty()) {
            Log.w(TAG, "News JSON is null or empty, skipping cache");
            return;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CACHED_NEWS, newsJson).apply();
            Log.d(TAG, "Successfully cached news data");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache news data", e);
        }
    }

    /**
     * Get cached news data
     * @param context Application context
     * @return Cached news JSON string, null if no cache
     */
    public static String getCachedNewsData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CACHED_NEWS, null);
    }

    /**
     * Clear all cached data
     * @param context Application context
     */
    public static void clearCache(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot clear cache");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Check if cache is available and not older than specified hours
     * @param context Application context
     * @param maxAgeHours Maximum age in hours
     * @return true if fresh cache exists
     */
    public static boolean isCacheFresh(Context context, int maxAgeHours) {
        long lastUpdate = getLastUpdateTime(context);
        if (lastUpdate == 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - lastUpdate;
        long maxAgeInMillis = maxAgeHours * 60 * 60 * 1000L;
        
        return ageInMillis < maxAgeInMillis;
    }
    
    /**
     * Check if cache has expired (older than 2 hours)
     * @param context Application context
     * @return true if cache is expired or doesn't exist
     */
    public static boolean isCacheExpired(Context context) {
        long lastUpdate = getLastUpdateTime(context);
        if (lastUpdate == 0) {
            return true; // No cache = expired
        }
        
        long currentTime = System.currentTimeMillis();
        long ageInMillis = currentTime - lastUpdate;
        
        return ageInMillis >= CACHE_EXPIRATION_MS;
    }
    
    /**
     * Get cached news data with expiration check
     * @param context Application context
     * @return Cached news JSON string, null if no cache or expired
     */
    public static String getCachedNewsDataIfValid(Context context) {
        if (isCacheExpired(context)) {
            Log.d(TAG, "Cache expired, returning null");
            return null;
        }
        return getCachedNewsData(context);
    }
}
