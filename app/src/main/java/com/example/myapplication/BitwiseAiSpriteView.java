package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class BitwiseAiSpriteView extends View {

    private final Paint planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float pulseScale = 1f;
    private float orbitRotation = 0f;

    private final RectF orbitRect = new RectF();

    public BitwiseAiSpriteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null); // smoother glow rendering

        planetPaint.setColor(Color.parseColor("#7C4DFF"));

        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setStrokeWidth(4f);
        orbitPaint.setColor(Color.parseColor("#00E5FF"));
        orbitPaint.setAlpha(180);

        starPaint.setColor(Color.WHITE);
        starPaint.setAlpha(220);

        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setAlpha(120);

        // Pulse animation
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0.92f, 1.08f);
        pulseAnimator.setDuration(1400);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();

        // Orbit rotation animation
        ValueAnimator orbitAnimator = ValueAnimator.ofFloat(0f, 360f);
        orbitAnimator.setDuration(5000);
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);
        orbitAnimator.addUpdateListener(animation -> {
            orbitRotation = (float) animation.getAnimatedValue();
            invalidate();
        });
        orbitAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float cx = width / 2f;
        float cy = height / 2f;

        // Important: keep everything INSIDE the square container
        float safeRadius = Math.min(width, height) * 0.18f;

        // ===== GLOW =====
        float glowRadius = safeRadius * 2.1f * pulseScale;

        RadialGradient gradient = new RadialGradient(
                cx,
                cy,
                glowRadius,
                new int[]{
                        Color.parseColor("#7C4DFF"),
                        Color.parseColor("#00E5FF"),
                        Color.TRANSPARENT
                },
                new float[]{0.15f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );

        glowPaint.setShader(gradient);

        canvas.drawCircle(
                cx,
                cy,
                glowRadius,
                glowPaint
        );

        // ===== ORBIT =====
        orbitRect.set(
                cx - safeRadius * 1.5f,
                cy - safeRadius * 0.9f,
                cx + safeRadius * 1.5f,
                cy + safeRadius * 0.9f
        );

        canvas.save();
        canvas.rotate(orbitRotation, cx, cy);
        canvas.drawOval(orbitRect, orbitPaint);

        // Orbiting moon
        float moonX = cx + safeRadius * 1.5f;

        canvas.drawCircle(
                moonX,
                cy,
                safeRadius * 0.14f,
                starPaint
        );

        canvas.restore();

        // ===== PLANET CORE =====
        canvas.drawCircle(
                cx,
                cy,
                safeRadius * pulseScale,
                planetPaint
        );

        // ===== PLANET HIGHLIGHT =====
        canvas.drawCircle(
                cx - safeRadius * 0.25f,
                cy - safeRadius * 0.25f,
                safeRadius * 0.22f,
                highlightPaint
        );
    }
}
