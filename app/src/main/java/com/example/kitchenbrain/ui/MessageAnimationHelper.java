package com.example.kitchenbrain.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 🔥 MESSAGE ANIMATION HELPER - Telegram Feel
 * Features: Smooth enter animations + slide effects + no jitter
 */
public class MessageAnimationHelper {
    
    private static final long ANIMATION_DURATION = 200;
    private static final long ANIMATION_DELAY = 50;
    private static final float INITIAL_ALPHA = 0f;
    private static final float INITIAL_TRANSLATION_Y = 30f;
    
    /**
     * 🎯 ANIMATE MESSAGE ENTRY - Smooth fade + slide
     */
    public static void animateMessageEntry(View itemView, int position) {
        // 🔥 NULL GUARD - Prevent UI thread crashes
        if (itemView == null) {
            Log.e("MessageAnimationHelper", "❌ Cannot animate: itemView is null");
            return;
        }
        
        // Set initial state
        itemView.setAlpha(INITIAL_ALPHA);
        itemView.setTranslationY(INITIAL_TRANSLATION_Y);
        itemView.setScaleX(0.95f);
        itemView.setScaleY(0.95f);
        
        // Stagger animation based on position
        long delay = position * ANIMATION_DELAY;
        
        // Animate to final state
        itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay(delay)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Ensure final state
                    itemView.setAlpha(1f);
                    itemView.setTranslationY(0f);
                    itemView.setScaleX(1f);
                    itemView.setScaleY(1f);
                })
                .start();
    }
    
    /**
     * 🎯 ANIMATE MESSAGE UPDATE - Status change animation
     */
    public static void animateMessageUpdate(View itemView) {
        // 🔥 NULL GUARD - Prevent UI thread crashes
        if (itemView == null) {
            Log.e("MessageAnimationHelper", "❌ Cannot animate update: itemView is null");
            return;
        }
        
        itemView.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(100)
                .withEndAction(() -> {
                    itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }
    
    /**
     * 🎯 ANIMATE MESSAGE REMOVAL - Smooth fade out
     */
    public static void animateMessageRemoval(View itemView, Runnable onComplete) {
        // 🔥 NULL GUARD - Prevent UI thread crashes
        if (itemView == null) {
            Log.e("MessageAnimationHelper", "❌ Cannot animate removal: itemView is null");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        itemView.animate()
                .alpha(0f)
                .translationY(-20f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .start();
    }
    
    /**
     * 🎯 ANIMATE BUBBLE PRESS - Micro interaction
     */
    public static void animateBubblePress(View itemView) {
        // 🔥 NULL GUARD - Prevent UI thread crashes
        if (itemView == null) {
            Log.e("MessageAnimationHelper", "❌ Cannot animate bubble press: itemView is null");
            return;
        }
        
        itemView.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(80)
                .withEndAction(() -> {
                    itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .start();
                })
                .start();
    }
    
    /**
     * 🎯 ANIMATE LONG PRESS - Haptic feedback + scale
     */
    public static void animateLongPress(View itemView) {
        // 🔥 NULL GUARD - Prevent UI thread crashes
        if (itemView == null) {
            Log.e("MessageAnimationHelper", "❌ Cannot animate long press: itemView is null");
            return;
        }
        
        itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        
        itemView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }
    
    /**
     * 🎯 ANIMATE STATUS ICON - Pulse effect
     */
    public static void animateStatusIcon(View statusIcon) {
        statusIcon.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(0.7f)
                .setDuration(150)
                .withEndAction(() -> {
                    statusIcon.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }
    
    /**
     * 🎯 RECYCLERVIEW ANIMATION SETUP
     */
    public static void setupRecyclerViewAnimations(RecyclerView recyclerView) {
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator() {
            @Override
            public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, 
                                       int fromX, int fromY, int toX, int toY) {
                return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
            }
            
            @Override
            public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
                return super.animateMove(holder, fromX, fromY, toX, toY);
            }
            
            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                animateMessageEntry(holder.itemView, holder.getAdapterPosition());
                return super.animateAdd(holder);
            }
            
            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                animateMessageRemoval(holder.itemView, null);
                return super.animateRemove(holder);
            }
        });
    }
}
