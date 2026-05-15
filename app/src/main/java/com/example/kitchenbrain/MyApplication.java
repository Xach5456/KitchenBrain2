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
import com.example.kitchenbrain.worker.NewsSyncWorker;

import com.example.kitchenbrain.BuildConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    
    // Background executor for heavy initialization
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);

    @Override
    public void onCreate() {
        super.onCreate();
        
        // CRASH DIAGNOSTIC
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH_HANDLER", "UNCAUGHT EXCEPTION in thread: " + thread.getName());
            Log.e("CRASH_HANDLER", "Exception: " + throwable.getMessage(), throwable);
        });
        
        Log.d(TAG, "Application starting...");
        
        // 1. Firebase must be initialized on Main Thread early
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }

        // 2. Offload heavy/IO tasks to background to fix "Skipped frames"
        backgroundExecutor.execute(() -> {
            try {
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
                
                FirebaseFirestore.getInstance().setFirestoreSettings(settings);
                Log.d(TAG, "Firestore settings initialized in background");
            } catch (Exception e) {
                Log.e(TAG, "Firestore settings failed", e);
            }
            
            try {
                CloudinaryHelper.INSTANCE.init(this);
                Log.d(TAG, "Cloudinary initialized in background");
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary failed", e);
            }

            // Schedule News Sync
            NewsSyncWorker.Companion.schedule(this);
        });
        
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectNetwork()
                .penaltyLog()
                .build());
            
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
        }
        
        Log.d(TAG, "Application main-thread init complete");
    }
}
