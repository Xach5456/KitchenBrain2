package com.example.kitchenbrain;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Check Google Play Services availability
        if (!GooglePlayServicesHelper.isGooglePlayServicesAvailable(this)) {
            Log.w(TAG, "Google Play Services not available. App may have limited functionality.");
        }
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Initialize Firebase App Check with debug provider for development
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        );
    }
}