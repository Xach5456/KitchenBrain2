package com.example.kitchenbrain;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.example.kitchenbrain.utils.CloudinaryHelper;

import com.example.kitchenbrain.BuildConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    
    // Background executor for heavy initialization
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Multidex not needed - minSdk >= 21 has native support
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // CRITICAL: Enable StrictMode AFTER Firebase initialization
        // to avoid false DiskReadViolation warnings from Firebase startup
        
        // CRASH DIAGNOSTIC: Global exception handler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH_HANDLER", "========================================");
            Log.e("CRASH_HANDLER", "UNCAUGHT EXCEPTION in thread: " + thread.getName());
            Log.e("CRASH_HANDLER", "Exception: " + throwable.getMessage());
            Log.e("CRASH_HANDLER", "Stack trace:", throwable);
            Log.e("CRASH_HANDLER", "========================================");
        });
        
        Log.d(TAG, "Application starting...");
        
        // CRITICAL: Initialize Firebase on main thread (lightweight)
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
        
        // 🔥 CRITICAL: Configure Firestore settings IMMEDIATELY after Firebase init
        // MUST be done BEFORE any Firestore usage (queries, listeners, etc.)
        // This is the ONLY place where setFirestoreSettings() should be called
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
            
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            Log.d(TAG, "Firestore settings configured (offline persistence enabled)");
        } catch (Exception e) {
            Log.e(TAG, "Firestore settings configuration failed", e);
        }
        
        // Initialize Cloudinary for media uploads
        try {
            CloudinaryHelper.INSTANCE.init(this);
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cloudinary initialization failed", e);
        }
        
        // Enable StrictMode AFTER Firebase initialization (debug builds only)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                // REMOVED: .detectDiskReads() - Firebase needs disk access during startup
                // REMOVED: .detectDiskWrites() - Firebase writes to cache
                .detectNetwork()  // Network on main thread is still bad
                .penaltyLog()
                .build());
            
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
            
            Log.d(TAG, "StrictMode enabled (logging only, no crashes)");
        }
        
        // ✅ REMOVED: Firestore settings now configured synchronously above
        // Background thread was causing race condition - settings must be set BEFORE any usage
        
        Log.d(TAG, "Application started successfully");
    }
    
}
