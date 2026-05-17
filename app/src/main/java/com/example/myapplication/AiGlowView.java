package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

/**
 * Ultra-vivid "Liquid Neon" effect inspired by premium AI interfaces.
 * Uses high-saturation colors and hardware-accelerated drawing.
 */
public class AiGlowView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private SweepGradient shader;
    private long startTime;

    // HIGH-VIBRANCY VIVID PALETTE - Pure neon hues for maximum "pop"
    private final int[] colors = {
            0xFFFF00FF, // Neon Magenta
            0xFF007AFF, // Electric Blue (Apple style)
            0xFF00FFFF, // Vivid Cyan
            0xFFAF52DE, // Vibrant Purple
            0xFFFF2D55, // Vivid Pink/Red
            0xFF00FF00, // Electric Green
            0xFFFF00FF  // Back to Magenta
    };

    public AiGlowView(Context context) {
        super(context);
        init();
    }

    public AiGlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        shader = new SweepGradient(w / 2f, h / 2f, colors, null);
        paint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long elapsed = System.currentTimeMillis() - startTime;
        float time = elapsed / 1000f;

        float w = getWidth();
        float h = getHeight();

        // 1. Energetic rotation for vivid color mixing
        matrix.setRotate(time * 45f, w / 2f, h / 2f);
        shader.setLocalMatrix(matrix);

        // 2. Pronounced "Breathing" scale
        float scale = 1.0f + (float) Math.sin(time * 1.5f) * 0.03f;
        canvas.scale(scale, scale, w / 2f, h / 2f);

        // --- Intensified Multi-Pass Vivid Glow ---
        
        // Pass 1: The "Bloom" (Wide, higher opacity for vivid bleed)
        drawOrganicPath(canvas, time, 160f, 80, 50f);

        // Pass 2: The "Secondary Core" (Medium, vibrant saturation)
        drawOrganicPath(canvas, time * 1.3f, 70f, 160, 30f);

        // Pass 3: The "Vivid Edge" (Thick, fully opaque neon line)
        drawOrganicPath(canvas, time * 0.9f, 20f, 255, 18f);

        postInvalidateOnAnimation();
    }

    private void drawOrganicPath(Canvas canvas, float time, float strokeWidth, int alpha, float amplitude) {
        paint.setStrokeWidth(strokeWidth);
        paint.setAlpha(alpha);
        
        path.reset();
        float w = getWidth();
        float h = getHeight();
        float step = 50f; 

        // Generate smooth liquid boundary
        path.moveTo(0, getModernWave(0, time, amplitude));
        for (float x = 0; x <= w; x += step) {
            path.lineTo(x, getModernWave(x, time, amplitude));
        }
        
        for (float y = 0; y <= h; y += step) {
            path.lineTo(w - getModernWave(y, time + 1.2f, amplitude), y);
        }
        
        for (float x = w; x >= 0; x -= step) {
            path.lineTo(x, h - getModernWave(x, time + 2.4f, amplitude));
        }
        
        for (float y = h; y >= 0; y -= step) {
            path.lineTo(getModernWave(y, time + 3.6f, amplitude), y);
        }

        path.close();
        canvas.drawPath(path, paint);
    }

    private float getModernWave(float pos, float time, float amp) {
        return (float) ((Math.sin(pos * 0.006 + time * 1.3) * 0.7 + 
                         Math.cos(pos * 0.01 - time * 1.1) * 0.3) * amp + amp * 1.5f);
    }
}
