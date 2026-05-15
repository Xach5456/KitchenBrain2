package com.example.kitchenbrain.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Network Utility Class
 * Provides methods to check network connectivity status
 */
public class NetworkUtils {

    /**
     * Check if device has internet connection
     * @param context Application or Activity context
     * @return true if connected to internet, false otherwise
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        
        return false;
    }

    /**
     * Check if connection is Wi-Fi
     * @param context Application or Activity context
     * @return true if connected via Wi-Fi
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        
        return false;
    }

    /**
     * Check if connection is Mobile Data
     * @param context Application or Activity context
     * @return true if connected via mobile data
     */
    public static boolean isMobileConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && 
                   networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
        
        return false;
    }
}
