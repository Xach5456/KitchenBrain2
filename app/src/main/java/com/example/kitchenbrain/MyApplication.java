package com.example.kitchenbrain;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.example.kitchenbrain.utils.CloudinaryHelper;
import com.example.kitchenbrain.worker.NewsSyncWorker;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Crash Handler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH_HANDLER", "UNCAUGHT EXCEPTION: " + throwable.getMessage(), throwable);
        });
        
        // 2. Firebase Early Init
        try {
            FirebaseApp.initializeApp(this);
            
            // 🔥 КРИТИЧЕСКИЙ ФИКС: Настройки ДОЛЖНЫ быть установлены сразу в onCreate
            // Это предотвращает ошибку "Firestore has already been started"
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            Log.d(TAG, "Firebase and Firestore settings initialized in onCreate");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }

        // 3. Background Services
        try {
            CloudinaryHelper.INSTANCE.init(this);
            NewsSyncWorker.Companion.schedule(this);
        } catch (Exception e) {
            Log.e(TAG, "Services init failed", e);
        }
    }

    /**
     * Stub method to maintain compatibility with MainActivity and other components.
     * Logic moved to onCreate for thread safety and Firestore requirements.
     */
    public void initializeInteractiveServices() {
        Log.d(TAG, "initializeInteractiveServices called (logic already handled in onCreate)");
    }
}
