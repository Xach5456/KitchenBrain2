package com.example.kitchenbrain;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 🚀 "Fluid Silk" Cinematic Splash Screen - Fixed NPE Edition
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final View logo = findViewById(R.id.appLogo);
        final View glow = findViewById(R.id.logoGlow);
        final View name = findViewById(R.id.appName);
        final View sub = findViewById(R.id.subTitle);
        final View progress = findViewById(R.id.progressBar);

        PathInterpolator fluidInterpolator = new PathInterpolator(0.4f, 0f, 0.2f, 1f);

        if (logo != null) {
            logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1200)
                    .setInterpolator(fluidInterpolator)
                    .start();
            startAmbientDrift(logo);
        }

        if (glow != null) {
            glow.animate()
                    .alpha(0.15f)
                    .scaleX(2.0f)
                    .scaleY(2.0f)
                    .setDuration(1500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            startPulsingAura(glow);
        }

        if (name != null) {
            name.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(300)
                    .setInterpolator(fluidInterpolator)
                    .start();
        }

        if (sub != null) {
            sub.animate()
                    .alpha(0.6f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(500)
                    .setInterpolator(fluidInterpolator)
                    .start();
        }

        if (progress != null) {
            progress.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(100)
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 3000);
    }

    private void startAmbientDrift(View view) {
        if (view == null) return;
        view.animate()
                .translationY(-10f)
                .rotation(0.5f)
                .setDuration(2500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (!isFinishing()) {
                        view.animate()
                                .translationY(10f)
                                .rotation(-0.5f)
                                .setDuration(2500)
                                .withEndAction(() -> startAmbientDrift(view))
                                .start();
                    }
                })
                .start();
    }

    private void startPulsingAura(View view) {
        if (view == null) return;
        view.animate()
                .alpha(0.05f)
                .scaleX(1.8f)
                .scaleY(1.8f)
                .setDuration(3000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (!isFinishing()) {
                        view.animate()
                                .alpha(0.2f)
                                .scaleX(2.4f)
                                .scaleY(2.4f)
                                .setDuration(3000)
                                .withEndAction(() -> startPulsingAura(view))
                                .start();
                    }
                })
                .start();
    }
}
