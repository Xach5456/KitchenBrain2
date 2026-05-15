package com.example.kitchenbrain;

import android.util.Log;

/**
 * Production-safe diagnostic manager
 * Runs diagnostics only in DEBUG mode
 */
public class DiagnosticManager {
    private static final String TAG = "DiagnosticManager";
    
    /**
     * Run all diagnostics - DEV ONLY
     */
    public static void runAllDiagnostics() {
        if (!BuildConfig.DEBUG) {
            Log.d(TAG, "🔥 PROD MODE - Skipping diagnostics");
            return;
        }
        
        Log.d(TAG, "🔥 DEV MODE - Running diagnostics...");
        FirestoreIndexHelper.checkUsernameLowerIndex();
        FirestoreIndexHelper.checkMissingUsernameLower();
        FirestoreIndexHelper.testSearchFunctionality();
        Log.d(TAG, "🔥 DEV MODE - Diagnostics complete");
    }
    
    /**
     * Quick index check - can be called manually
     */
    public static void quickIndexCheck() {
        if (BuildConfig.DEBUG) {
            FirestoreIndexHelper.checkUsernameLowerIndex();
        }
    }
    
    /**
     * Manual data consistency check
     */
    public static void checkDataConsistency() {
        if (BuildConfig.DEBUG) {
            FirestoreIndexHelper.checkMissingUsernameLower();
        }
    }
}
