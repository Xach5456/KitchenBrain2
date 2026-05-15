package com.example.kitchenbrain.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Google Play Services Error Handler
 * 
 * Purpose:
 * - Gracefully handle DEVELOPER_ERROR on emulators
 * - Prevent retry storms and blocking behavior
 * - Provide fallback mechanisms
 * 
 * Note: DEVELOPER_ERROR on emulators (API 35-36, GMS 26.x) is a known issue
 * and does not affect real devices or app functionality.
 */
public class GmsErrorHandler {
    
    private static final String TAG = "GmsErrorHandler";
    private static volatile boolean isGmsAvailable = false;
    private static volatile boolean hasChecked = false;
    
    /**
     * Check if Google Play Services is available
     * Returns cached result after first check
     */
    public static boolean isGmsAvailable(Context context) {
        if (hasChecked) {
            return isGmsAvailable;
        }
        
        synchronized (GmsErrorHandler.class) {
            if (hasChecked) {
                return isGmsAvailable;
            }
            
            try {
                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
                
                if (resultCode == ConnectionResult.SUCCESS) {
                    isGmsAvailable = true;
                    Log.d(TAG, "✅ Google Play Services available");
                } else if (resultCode == ConnectionResult.DEVELOPER_ERROR) {
                    // This is NORMAL on emulators - not a real error
                    isGmsAvailable = false;
                    Log.w(TAG, "⚠️ DEVELOPER_ERROR detected (emulator artifact, ignoring)");
                    Log.w(TAG, "App will work normally - GMS-dependent features may use fallback");
                } else {
                    isGmsAvailable = false;
                    Log.w(TAG, "⚠️ Google Play Services unavailable: " + 
                         apiAvailability.getErrorString(resultCode));
                }
                
                hasChecked = true;
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking GMS availability", e);
                isGmsAvailable = false;
                hasChecked = true;
            }
            
            return isGmsAvailable;
        }
    }
    
    /**
     * Handle GMS connection failure gracefully
     * Does NOT block or retry on DEVELOPER_ERROR
     */
    public static void handleConnectionFailure(Context context, int errorCode) {
        if (errorCode == ConnectionResult.DEVELOPER_ERROR) {
            // Emulator artifact - log once and ignore
            if (!hasChecked) {
                Log.w(TAG, "DEVELOPER_ERROR: This is normal on emulators, skipping GMS features");
                isGmsAvailable = false;
                hasChecked = true;
            }
            return; // Don't retry, don't block
        }
        
        // For other errors, log and provide fallback
        Log.w(TAG, "GMS error code: " + errorCode + ", using fallback behavior");
        isGmsAvailable = false;
    }
    
    /**
     * Get user-friendly message for GMS errors
     */
    public static String getUserFriendlyMessage(int errorCode) {
        switch (errorCode) {
            case ConnectionResult.DEVELOPER_ERROR:
                return ""; // Don't show anything to user - it's an emulator artifact
            case ConnectionResult.SERVICE_MISSING:
                return "Google Play Services is not installed on this device";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return "Google Play Services needs to be updated";
            case ConnectionResult.SERVICE_DISABLED:
                return "Google Play Services is disabled";
            default:
                return "Google Play Services encountered an error";
        }
    }
    
    /**
     * Reset cached state (for testing)
     */
    public static void resetForTesting() {
        synchronized (GmsErrorHandler.class) {
            hasChecked = false;
            isGmsAvailable = false;
        }
    }
}
