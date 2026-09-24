package com.ciblorenzo.whatsonmyfood;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

public class AiGlowManager {

    public static void startGlow(Activity activity) {
        if (activity == null) return;

        activity.runOnUiThread(() -> startGlowOnMainThread(activity));
    }

    private static void startGlowOnMainThread(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        setSpriteAnimation(decorView, true);
        View existingGlow = decorView.findViewWithTag("AI_GLOW_VIEW");
        if (existingGlow != null) {
            existingGlow.animate().cancel();
            existingGlow.animate().alpha(0.85f).setDuration(450).start();
            return;
        }

        AiGlowView glowView = new AiGlowView(activity);
        glowView.setScreenBorder(true);
        glowView.setTag("AI_GLOW_VIEW");
        glowView.setAlpha(0f);
        glowView.setElevation(100f);
        glowView.setTranslationZ(100f);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        decorView.addView(glowView, params);
        
        glowView.animate().alpha(0.85f).setDuration(450).start();
    }

    public static void stopGlow(Activity activity) {
        if (activity == null) return;

        activity.runOnUiThread(() -> stopGlowOnMainThread(activity));
    }

    private static void stopGlowOnMainThread(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        setSpriteAnimation(decorView, false);
        View glowView = decorView.findViewWithTag("AI_GLOW_VIEW");
        if (glowView != null) {
            glowView.animate()
                    .alpha(0f)
                    .setDuration(450)
                    .withEndAction(() -> decorView.removeView(glowView))
                    .start();
        }
    }

    private static void setSpriteAnimation(View view, boolean animating) {
        if (view instanceof BitwiseAiSpriteView) {
            ((BitwiseAiSpriteView) view).setAnimating(animating);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setSpriteAnimation(group.getChildAt(i), animating);
            }
        }
    }
}
