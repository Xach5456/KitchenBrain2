package com.example.kitchenbrain.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class AnimationUtils {

    public static void animateViewAppear(View view, int delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(delayMs)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    public static void animateButtonClick(View view) {
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(150)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    public static void animateShimmer(View shimmerView) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(shimmerView, "alpha", 0.3f, 0.7f);
        animator.setDuration(1000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    public static void animateSuccess(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    public static void animateErrorShake(View view) {
        view.setTranslationX(0f);
        
        view.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction(() -> view.animate()
                .translationX(10f)
                .setDuration(50)
                .withEndAction(() -> view.animate()
                    .translationX(-10f)
                    .setDuration(50)
                    .withEndAction(() -> view.animate()
                        .translationX(10f)
                        .setDuration(50)
                        .withEndAction(() -> view.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start())
                        .start())
                    .start())
                .start())
            .start();
    }
}
