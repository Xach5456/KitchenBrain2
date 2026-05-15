package com.example.kitchenbrain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kitchenbrain.R;
import com.google.android.material.button.MaterialButton;

/**
 * ApiUpgradeFragment - Handle API Limit Reached
 * 
 * ✅ PRODUCTION ARCHITECTURE:
 * - Shows upgrade options when API limit is reached
 * - Provides links to Spoonacular dashboard
 * - Allows waiting for limit reset
 */
public class ApiUpgradeFragment extends Fragment {

    private static final String TAG = "ApiUpgradeFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_upgrade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListeners(view);
        
        Log.d(TAG, "🚫 API Upgrade screen displayed");
    }

    private void setupClickListeners(View view) {
        MaterialButton btnUpgrade = view.findViewById(R.id.btnUpgrade);
        MaterialButton btnCheckUsage = view.findViewById(R.id.btnCheckUsage);
        View btnWait = view.findViewById(R.id.btnWait);

        btnUpgrade.setOnClickListener(v -> {
            Log.d(TAG, "🚀 Upgrade button clicked");
            openSpoonacularDashboard();
        });

        btnCheckUsage.setOnClickListener(v -> {
            Log.d(TAG, "📊 Check usage button clicked");
            openSpoonacularUsage();
        });

        btnWait.setOnClickListener(v -> {
            Log.d(TAG, "⏰ Wait button clicked");
            showWaitingInfo();
        });
    }

    private void openSpoonacularDashboard() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://spoonacular.com/food-api/console"));
            startActivity(intent);
            Log.d(TAG, "✅ Opening Spoonacular dashboard");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to open dashboard", e);
            Toast.makeText(getContext(), "Failed to open dashboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSpoonacularUsage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://spoonacular.com/food-api/console#usage"));
            startActivity(intent);
            Log.d(TAG, "✅ Opening Spoonacular usage");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to open usage", e);
            Toast.makeText(getContext(), "Failed to open usage", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWaitingInfo() {
        Toast.makeText(getContext(), "API limit resets daily. Check back tomorrow!", Toast.LENGTH_LONG).show();
        Log.d(TAG, "ℹ️ Showed waiting info");
    }
}
