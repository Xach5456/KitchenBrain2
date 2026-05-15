package com.example.kitchenbrain.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.kitchenbrain.R;

/**
 * Safe Image Loader Utility for Glide
 * Prevents common crashes like Resource ID #0x0, NullPointerException, etc.
 * 
 * Usage:
 *   ImageLoader.loadSafe(imageView, imageUrl);
 *   ImageLoader.loadResource(imageView, R.drawable.my_image);
 *   ImageLoader.loadCircular(imageView, profileUrl);
 */
public class ImageLoader {
    
    private static final String TAG = "ImageLoader";
    
    // Private constructor to prevent instantiation
    private ImageLoader() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
    
    /**
     * Safely load image from URL or resource ID with automatic validation
     * 
     * @param imageView Target ImageView
     * @param imageSource Can be String (URL) or Integer (resource ID)
     */
    public static void loadSafe(@NonNull ImageView imageView, @Nullable Object imageSource) {
        loadSafe(imageView, imageSource, R.drawable.ic_launcher_foreground);
    }
    
    /**
     * Safely load image with custom placeholder
     * 
     * @param imageView Target ImageView
     * @param imageSource Can be String (URL) or Integer (resource ID)
     * @param placeholderResId Placeholder drawable resource
     */
    public static void loadSafe(@NonNull ImageView imageView, @Nullable Object imageSource, 
                                @DrawableRes int placeholderResId) {
        Context context = imageView.getContext();
        
        // SAFETY CHECK #1: Validate context
        if (context == null) {
            LogWrapper.w(TAG, "Context is null, skipping image load");
            return;
        }
        
        // SAFETY CHECK #2: Handle null source
        if (imageSource == null) {
            loadWithPlaceholder(imageView, placeholderResId);
            return;
        }
        
        // SAFETY CHECK #3: Handle String URLs
        if (imageSource instanceof String) {
            String url = (String) imageSource;
            
            // Validate URL
            if (url.trim().isEmpty() || 
                url.equals("null") || 
                url.equals("http://") || 
                url.equals("https://")) {
                
                LogWrapper.d(TAG, "Invalid URL format: " + url + ", using placeholder");
                loadWithPlaceholder(imageView, placeholderResId);
                return;
            }
            
            // Load from URL
            Glide.with(context)
                 .load(url)
                 .placeholder(placeholderResId)
                 .error(placeholderResId)
                 .fallback(placeholderResId)
                 .centerCrop()
                 .into(imageView);
            return;
        }
        
        // SAFETY CHECK #4: Handle Integer resource IDs
        if (imageSource instanceof Integer) {
            int resId = (Integer) imageSource;
            
            // Validate resource ID - CRITICAL: Prevent 0x0 crash
            if (resId == 0 || resId == R.color.transparent) {
                LogWrapper.d(TAG, "Invalid resource ID: " + resId + ", using placeholder");
                loadWithPlaceholder(imageView, placeholderResId);
                return;
            }
            
            // Load from resource
            Glide.with(context)
                 .load(resId)
                 .placeholder(placeholderResId)
                 .error(placeholderResId)
                 .fallback(placeholderResId)
                 .centerCrop()
                 .into(imageView);
            return;
        }
        
        // Fallback for unsupported types
        LogWrapper.w(TAG, "Unsupported image source type: " + imageSource.getClass().getName());
        loadWithPlaceholder(imageView, placeholderResId);
    }
    
    /**
     * Load image from URL with circular crop (for profile pictures)
     * 
     * @param imageView Target ImageView
     * @param url Image URL
     */
    public static void loadCircular(@NonNull ImageView imageView, @Nullable String url) {
        loadCircular(imageView, url, R.drawable.ic_launcher_foreground);
    }
    
    /**
     * Load image from URL with circular crop and custom placeholder
     * 
     * @param imageView Target ImageView
     * @param url Image URL
     * @param placeholderResId Placeholder for loading/error states
     */
    public static void loadCircular(@NonNull ImageView imageView, @Nullable String url,
                                    @DrawableRes int placeholderResId) {
        Context context = imageView.getContext();
        
        if (context == null) {
            return;
        }
        
        // Validate URL
        if (url == null || url.trim().isEmpty() || url.equals("null")) {
            loadImageResource(imageView, placeholderResId);
            return;
        }
        
        Glide.with(context)
             .load(url)
             .placeholder(placeholderResId)
             .error(placeholderResId)
             .fallback(placeholderResId)
             .circleCrop()
             .into(imageView);
    }
    
    /**
     * Load image from resource ID with validation
     * 
     * @param imageView Target ImageView
     * @param resId Drawable resource ID
     */
    public static void loadResource(@NonNull ImageView imageView, @DrawableRes int resId) {
        loadResource(imageView, resId, R.drawable.ic_launcher_foreground);
    }
    
    /**
     * Load image from resource ID with custom placeholder
     * 
     * @param imageView Target ImageView
     * @param resId Drawable resource ID
     * @param placeholderResId Fallback if resId is invalid
     */
    public static void loadResource(@NonNull ImageView imageView, @DrawableRes int resId,
                                    @DrawableRes int placeholderResId) {
        Context context = imageView.getContext();
        
        if (context == null) {
            return;
        }
        
        // CRITICAL: Prevent 0x0 crash
        if (resId == 0) {
            LogWrapper.w(TAG, "Attempted to load resource ID 0, using placeholder instead");
            loadImageResource(imageView, placeholderResId);
            return;
        }
        
        Glide.with(context)
             .load(resId)
             .placeholder(placeholderResId)
             .error(placeholderResId)
             .fallback(placeholderResId)
             .centerCrop()
             .into(imageView);
    }
    
    /**
     * Load image with callback for completion/failure
     * 
     * @param imageView Target ImageView
     * @param imageSource URL or resource ID
     * @param listener Callback for load status
     */
    public static void loadWithCallback(@NonNull ImageView imageView, 
                                        @Nullable Object imageSource,
                                        @NonNull RequestListener<Drawable> listener) {
        Context context = imageView.getContext();
        
        if (context == null || imageSource == null) {
            return;
        }
        
        // Handle resource ID 0
        if (imageSource instanceof Integer && (Integer) imageSource == 0) {
            return;
        }
        
        Glide.with(context)
             .load(imageSource)
             .listener(listener)
             .placeholder(R.drawable.ic_launcher_foreground)
             .error(R.drawable.ic_launcher_foreground)
             .into(imageView);
    }
    
    /**
     * Clear Glide request from ImageView (prevents memory leaks)
     * 
     * @param imageView ImageView to clear
     */
    public static void clear(@NonNull ImageView imageView) {
        Context context = imageView.getContext();
        if (context != null) {
            Glide.with(context).clear(imageView);
        }
    }
    
    /**
     * Resume Glide requests (call in Fragment/Activity onResume)
     * 
     * @param context Context
     */
    public static void resumeRequests(@NonNull Context context) {
        Glide.with(context).resumeRequests();
    }
    
    /**
     * Pause Glide requests (call in Fragment/Activity onPause)
     * 
     * @param context Context
     */
    public static void pauseRequests(@NonNull Context context) {
        Glide.with(context).pauseRequests();
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Load simple placeholder image
     */
    private static void loadWithPlaceholder(@NonNull ImageView imageView, 
                                            @DrawableRes int placeholderResId) {
        Context context = imageView.getContext();
        if (context != null) {
            Glide.with(context)
                 .load(placeholderResId)
                 .centerCrop()
                 .into(imageView);
        }
    }
    
    /**
     * Load image resource directly (no Glide processing)
     */
    private static void loadImageResource(@NonNull ImageView imageView, 
                                          @DrawableRes int resId) {
        if (resId != 0) {
            imageView.setImageResource(resId);
        }
    }
    
    /**
     * Simple logging wrapper to avoid crashes if Log is unavailable
     */
    private static class LogWrapper {
        static void d(String tag, String message) {
            android.util.Log.d(tag, message);
        }
        
        static void w(String tag, String message) {
            android.util.Log.w(tag, message);
        }
        
        static void e(String tag, String message) {
            android.util.Log.e(tag, message);
        }
    }
}
