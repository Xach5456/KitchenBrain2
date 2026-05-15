package com.example.kitchenbrain;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Glide configuration module
 * Required to fix "Failed to find GeneratedAppGlideModule" error
 */
@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Set memory cache
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2)
                .build();
        
        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
        
        // Set bitmap pool
        int sizeMultiplier = 4;
        builder.setBitmapPool(new LruBitmapPool(
                calculator.getBitmapPoolSize() * sizeMultiplier));
        
        // Set disk cache
        int diskCacheSizeBytes = 1024 * 1024 * 100; // 100 MB
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));
        
        // Set default request options
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .disallowHardwareConfig()
                        .fallback(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
        );
        
        // Disable manifest parsing for better performance
        builder.setLogLevel(android.util.Log.DEBUG);
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
