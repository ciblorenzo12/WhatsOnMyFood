package com.example.myapplication;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScanningOverlayView extends View {

    private Paint scanPaint;
    private Paint glowPaint;
    private Paint particlePaint;
    private float scanLineY = 0f;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private boolean isScanning = false;

    public ScanningOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scanPaint.setStrokeWidth(6f);
        scanPaint.setStyle(Paint.Style.STROKE);
        scanPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStrokeWidth(20f);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        startScanAnimation();
    }

    private void startScanAnimation() {
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                scanLineY = interpolatedTime * getHeight();
                updateParticles();
                invalidate();
            }
        };
        anim.setDuration(2500);
        anim.setRepeatCount(Animation.INFINITE);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        startAnimation(anim);
    }

    public void startScanning() {
        isScanning = true;
        setVisibility(VISIBLE);
    }

    public void stopScanning() {
        isScanning = false;
        setVisibility(GONE);
    }

    private void updateParticles() {
        if (particles.size() < 40 && random.nextFloat() > 0.7) {
            particles.add(new Particle(random.nextFloat() * getWidth(), scanLineY));
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.y += (random.nextFloat() - 0.5f) * 8;
            p.alpha -= 4;
            if (p.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isScanning || getHeight() == 0 || canvas == null) return;

        int blue = Color.parseColor("#6366F1");
        int purple = Color.parseColor("#9b51e0");
        int pink = Color.parseColor("#e91e63");

        LinearGradient gradient = new LinearGradient(0, scanLineY - 30, 0, scanLineY + 30,
                new int[]{Color.TRANSPARENT, blue, purple, pink, Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP);
        
        // Draw double glow
        glowPaint.setShader(gradient);
        glowPaint.setAlpha(100);
        canvas.drawLine(0, scanLineY, getWidth(), scanLineY, glowPaint);
        
        scanPaint.setShader(gradient);
        scanPaint.setAlpha(255);
        canvas.drawLine(0, scanLineY, getWidth(), scanLineY, scanPaint);

        // Draw particles with variations
        for (Particle p : particles) {
            particlePaint.setColor(purple);
            particlePaint.setAlpha(p.alpha);
            float radius = 3 + random.nextFloat() * 3;
            canvas.drawCircle(p.x, p.y, radius, particlePaint);
        }
    }

    private static class Particle {
        float x, y;
        int alpha = 255;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
