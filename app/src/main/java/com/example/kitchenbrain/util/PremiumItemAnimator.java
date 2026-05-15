package com.example.kitchenbrain.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.recyclerview.widget.RecyclerView;

public class PremiumItemAnimator extends RecyclerView.ItemAnimator {

    @Override
    public boolean animateDisappearance(RecyclerView.ViewHolder viewHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        View view = viewHolder.itemView;
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f);
        
        alpha.setDuration(200);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        alpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchAnimationFinished(viewHolder);
            }
        });
        
        alpha.start();
        scaleX.start();
        scaleY.start();
        return true;
    }

    @Override
    public boolean animateAppearance(RecyclerView.ViewHolder viewHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        View view = viewHolder.itemView;
        view.setAlpha(0f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.setTranslationY(30f);
        
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 30f, 0f);
        
        alpha.setDuration(350);
        scaleX.setDuration(350);
        scaleY.setDuration(350);
        translationY.setDuration(350);
        
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        translationY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        alpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchAnimationFinished(viewHolder);
            }
        });
        
        alpha.start();
        scaleX.start();
        scaleY.start();
        translationY.start();
        return true;
    }

    @Override
    public boolean animatePersistence(RecyclerView.ViewHolder viewHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        return false;
    }

    @Override
    public void runPendingAnimations() {
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        item.itemView.animate().cancel();
    }

    @Override
    public void endAnimations() {
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
