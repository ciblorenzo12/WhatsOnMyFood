package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class ScannerOverlayView extends View {

    private Paint backgroundPaint;
    private Paint windowPaint;
    private Paint borderPaint;
    private Paint laserPaint;
    private RectF windowRect;

    private float laserPosition = 0;
    private ValueAnimator laserAnimator;

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#99000000")); // Semi-transparent black

        windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        windowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR)); // To punch a hole

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserPaint.setColor(Color.GREEN);
        laserPaint.setStrokeWidth(8f);
        laserPaint.setShadowLayer(10.0f, 0f, 0f, Color.GREEN);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setupWindowRect(w, h);
        setupLaserAnimator();
    }

    private void setupWindowRect(int width, int height) {
        float windowWidth = width * 0.8f;
        float windowHeight = height * 0.3f;
        float left = (width - windowWidth) / 2;
        float top = (height - windowHeight) / 2;
        float right = left + windowWidth;
        float bottom = top + windowHeight;
        windowRect = new RectF(left, top, right, bottom);
    }

    private void setupLaserAnimator() {
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
        laserAnimator = ValueAnimator.ofFloat(0, 1);
        laserAnimator.setDuration(2000);
        laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.addUpdateListener(animation -> {
            laserPosition = (float) animation.getAnimatedValue();
            invalidate();
        });
        laserAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (windowRect == null) {
            return;
        }

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        canvas.drawRect(windowRect, windowPaint);
        drawCorners(canvas);

        float laserY = windowRect.top + windowRect.height() * laserPosition;
        canvas.drawLine(windowRect.left, laserY, windowRect.right, laserY, laserPaint);
    }

    private void drawCorners(Canvas canvas) {
        float cornerLength = windowRect.width() * 0.1f;

        // Top-left
        canvas.drawLine(windowRect.left - borderPaint.getStrokeWidth() / 2, windowRect.top, windowRect.left + cornerLength, windowRect.top, borderPaint);
        canvas.drawLine(windowRect.left, windowRect.top, windowRect.left, windowRect.top + cornerLength, borderPaint);

        // Top-right
        canvas.drawLine(windowRect.right + borderPaint.getStrokeWidth() / 2, windowRect.top, windowRect.right - cornerLength, windowRect.top, borderPaint);
        canvas.drawLine(windowRect.right, windowRect.top, windowRect.right, windowRect.top + cornerLength, borderPaint);

        // Bottom-left
        canvas.drawLine(windowRect.left - borderPaint.getStrokeWidth() / 2, windowRect.bottom, windowRect.left + cornerLength, windowRect.bottom, borderPaint);
        canvas.drawLine(windowRect.left, windowRect.bottom, windowRect.left, windowRect.bottom - cornerLength, borderPaint);

        // Bottom-right
        canvas.drawLine(windowRect.right + borderPaint.getStrokeWidth() / 2, windowRect.bottom, windowRect.right - cornerLength, windowRect.bottom, borderPaint);
        canvas.drawLine(windowRect.right, windowRect.bottom, windowRect.right, windowRect.bottom - cornerLength, borderPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
    }
}
