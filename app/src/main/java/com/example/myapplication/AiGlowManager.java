package com.example.myapplication;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

public class AiGlowManager {

    public static void startGlow(Activity activity) {
        if (activity == null) return;

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View existingGlow = decorView.findViewWithTag("AI_GLOW_VIEW");
        if (existingGlow != null) return;

        AiGlowView glowView = new AiGlowView(activity);
        glowView.setTag("AI_GLOW_VIEW");
        glowView.setAlpha(0f);
        glowView.setElevation(100f); 
        glowView.setTranslationZ(100f);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        decorView.addView(glowView, params);
        
        glowView.animate().alpha(1.0f).setDuration(500).start();
    }

    public static void stopGlow(Activity activity) {
        if (activity == null) return;

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View glowView = decorView.findViewWithTag("AI_GLOW_VIEW");
        if (glowView != null) {
            glowView.animate()
                    .alpha(0f)
                    .setDuration(600)
                    .withEndAction(() -> decorView.removeView(glowView))
                    .start();
        }
    }
}
