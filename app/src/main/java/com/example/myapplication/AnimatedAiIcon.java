package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class AnimatedAiIcon extends View {

    private Paint pulsePaint;
    private Paint corePaint;
    private float pulseRadius = 0f;
    private ValueAnimator animator;

    public AnimatedAiIcon(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(Color.parseColor("#7C4DFF")); // primaryColor

        animator = ValueAnimator.ofFloat(0.4f, 1.0f);
        animator.setDuration(1500);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            pulseRadius = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float baseRadius = Math.min(cx, cy) * 0.5f;

        // Draw pulse
        int color = Color.parseColor("#7C4DFF");
        int alpha = (int) (100 * (1.0f - (pulseRadius - 0.4f) / 0.6f));
        pulsePaint.setAlpha(Math.max(0, alpha));
        
        RadialGradient gradient = new RadialGradient(cx, cy, baseRadius * 2 * pulseRadius,
                new int[]{Color.parseColor("#7C4DFF"), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP);
        pulsePaint.setShader(gradient);
        
        canvas.drawCircle(cx, cy, baseRadius * 2 * pulseRadius, pulsePaint);

        // Draw core
        canvas.drawCircle(cx, cy, baseRadius * 0.8f, corePaint);
    }
}
