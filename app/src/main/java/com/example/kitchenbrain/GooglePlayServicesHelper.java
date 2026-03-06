package com.example.kitchenbrain;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class GooglePlayServicesHelper {
    private static final String TAG = "GooglePlayServicesHelper";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Check if Google Play Services is available and up to date
     * @param context Application context
     * @return true if Google Play Services is available, false otherwise
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.e(TAG, "Google Play Services not available. Error code: " + resultCode);
            } else {
                Log.e(TAG, "This device is not supported for Google Play Services.");
            }
            return false;
        }
        return true;
    }

    /**
     * Get error message for the given error code
     * @param context Application context
     * @param errorCode Error code from Google Play Services
     * @return Error message string
     */
    public static String getErrorMessage(Context context, int errorCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return apiAvailability.getErrorString(errorCode);
    }
}