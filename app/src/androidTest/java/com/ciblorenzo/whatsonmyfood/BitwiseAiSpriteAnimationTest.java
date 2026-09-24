package com.ciblorenzo.whatsonmyfood;

import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BitwiseAiSpriteAnimationTest {

    @Test
    public void spriteAnimatesOnlyWhileAiGlowIsActive() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario =
                     ActivityScenario.launch(ProductDetailLayoutPreviewActivity.class)) {
            scenario.onActivity(activity -> {
                BitwiseAiSpriteView sprite = findSprite(activity.getWindow().getDecorView());
                assertNotNull(sprite);
                assertFalse("Completed explanations should leave the icon still", sprite.isAnimating());

                AiGlowManager.startGlow(activity);
                assertTrue("AI loading should animate the icon", sprite.isAnimating());

                AiGlowManager.stopGlow(activity);
                assertFalse("Finishing AI should stop the animation", sprite.isAnimating());
            });
        }
    }

    private static BitwiseAiSpriteView findSprite(View view) {
        if (view instanceof BitwiseAiSpriteView) return (BitwiseAiSpriteView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                BitwiseAiSpriteView sprite = findSprite(group.getChildAt(i));
                if (sprite != null) return sprite;
            }
        }
        return null;
    }
}
