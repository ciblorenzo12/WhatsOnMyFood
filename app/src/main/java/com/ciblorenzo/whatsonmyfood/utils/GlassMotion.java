package com.ciblorenzo.whatsonmyfood.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public final class GlassMotion {
    private static final long ENTER_DURATION_MS = 420L;
    private static final long PRESS_DURATION_MS = 120L;
    private static final DecelerateInterpolator ENTER_EASE = new DecelerateInterpolator(1.7f);

    private GlassMotion() {
    }

    public static void enter(View view) {
        enter(view, 0L);
    }

    public static void enter(View view, long delayMs) {
        if (view == null) return;
        view.post(() -> {
            view.animate().cancel();
            view.setAlpha(0f);
            view.setTranslationY(dp(view, 18f));
            view.setScaleX(0.985f);
            view.setScaleY(0.985f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delayMs)
                    .setDuration(ENTER_DURATION_MS)
                    .setInterpolator(ENTER_EASE)
                    .start();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attachPress(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (!v.isEnabled()) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate()
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .setDuration(PRESS_DURATION_MS)
                        .start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(PRESS_DURATION_MS)
                        .setInterpolator(ENTER_EASE)
                        .start();
            }
            return false;
        });
    }

    private static float dp(View view, float value) {
        return value * view.getResources().getDisplayMetrics().density;
    }
}
