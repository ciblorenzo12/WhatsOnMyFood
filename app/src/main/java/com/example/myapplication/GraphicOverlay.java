package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    private final Matrix transformMatrix = new Matrix();

    private int imageWidth;
    private int imageHeight;
    private boolean isImageFlipped;

    private Paint backgroundPaint;
    private Paint windowPaint;
    private Paint laserPaint;
    private Paint cornerPaint;
    private RectF windowRect;
    private ValueAnimator laserAnimator;
    private boolean drawStaticFrame = true;

    public abstract static class Graphic {
        private final GraphicOverlay graphicOverlay;

        public Graphic(GraphicOverlay overlay) {
            this.graphicOverlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public GraphicOverlay getOverlay() {
            return graphicOverlay;
        }

        public Context getApplicationContext() {
            return graphicOverlay.getContext().getApplicationContext();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#99000000")); // Semi-transparent black

        windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        windowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR)); // To punch a hole

        laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserPaint.setColor(Color.GREEN);
        laserPaint.setStrokeWidth(12f);
        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeCap(Paint.Cap.ROUND);
        laserPaint.setShadowLayer(30.0f, 0f, 0f, Color.GREEN);

        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(8f);

        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            setupWindowRect(getWidth(), getHeight());
            setupLaserAnimator();
        });
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
            if (drawStaticFrame) {
                invalidate(); // Redraw on each frame of the laser animation
            }
        });
        laserAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    public void setImageSourceInfo(int imageWidth, int imageHeight, boolean isFlipped) {
        if (this.imageWidth != imageWidth || this.imageHeight != imageHeight || this.isImageFlipped != isFlipped) {
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.isImageFlipped = isFlipped;
            postInvalidate();
        }
    }

    public void setDrawStaticFrame(boolean draw) {
        this.drawStaticFrame = draw;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawStaticFrame) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
            if (windowRect != null) {
                canvas.drawRect(windowRect, windowPaint);
                drawViewfinderCorners(canvas);
                
                if (laserAnimator != null) {
                    float laserPosition = (float) laserAnimator.getAnimatedValue();
                    float laserY = windowRect.top + windowRect.height() * laserPosition;
                    canvas.drawLine(windowRect.left, laserY, windowRect.right, laserY, laserPaint);
                }
            }
        }

        synchronized (lock) {
            updateTransformationMatrix();
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

    private void drawViewfinderCorners(Canvas canvas) {
        if (windowRect == null) return;
        
        float cornerLength = windowRect.width() * 0.1f; // 10% of the width
        float strokeWidth = cornerPaint.getStrokeWidth();

        // Top-left
        canvas.drawLine(windowRect.left - strokeWidth / 2, windowRect.top, windowRect.left + cornerLength, windowRect.top, cornerPaint);
        canvas.drawLine(windowRect.left, windowRect.top, windowRect.left, windowRect.top + cornerLength, cornerPaint);

        // Top-right
        canvas.drawLine(windowRect.right + strokeWidth / 2, windowRect.top, windowRect.right - cornerLength, windowRect.top, cornerPaint);
        canvas.drawLine(windowRect.right, windowRect.top, windowRect.right, windowRect.top + cornerLength, cornerPaint);

        // Bottom-left
        canvas.drawLine(windowRect.left - strokeWidth / 2, windowRect.bottom, windowRect.left + cornerLength, windowRect.bottom, cornerPaint);
        canvas.drawLine(windowRect.left, windowRect.bottom, windowRect.left, windowRect.bottom - cornerLength, cornerPaint);

        // Bottom-right
        canvas.drawLine(windowRect.right + strokeWidth / 2, windowRect.bottom, windowRect.right - cornerLength, windowRect.bottom, cornerPaint);
        canvas.drawLine(windowRect.right, windowRect.bottom, windowRect.right, windowRect.bottom - cornerLength, cornerPaint);
    }

    private void updateTransformationMatrix() {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float viewWidth = (float) getWidth();
        float viewHeight = (float) getHeight();

        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;

        float scale = Math.max(scaleX, scaleY);

        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;

        float dx = (viewWidth - scaledWidth) / 2;
        float dy = (viewHeight - scaledHeight) / 2;

        transformMatrix.reset();
        transformMatrix.postScale(scale, scale);
        transformMatrix.postTranslate(dx, dy);

        if (isImageFlipped) {
            transformMatrix.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f);
        }
    }

    public Matrix getTransformMatrix() {
        return transformMatrix;
    }
}