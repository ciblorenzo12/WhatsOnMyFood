package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScanningOverlayView extends View {

    public enum OverlayMode {
        BARCODE,
        INGREDIENTS
    }

    private static final int BARCODE_GREEN = Color.rgb(46, 255, 90);
    private static final int INGREDIENT_PURPLE = Color.rgb(197, 82, 255);
    private static final int INGREDIENT_BLUE = Color.rgb(79, 178, 255);

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textMapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textMapFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix transformMatrix = new Matrix();
    private final RectF focusRect = new RectF();
    private final RectF activeBarcodeRect = new RectF();
    private final RectF tempRect = new RectF();
    private final List<RectF> textRects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private OverlayMode mode = OverlayMode.BARCODE;
    private ValueAnimator animator;
    private float scanProgress;
    private int imageWidth;
    private int imageHeight;
    private boolean isImageFlipped;
    private int bestTextIndex = -1;
    private boolean barcodeLocked;

    public ScanningOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        dimPaint.setColor(Color.argb(92, 0, 0, 0));

        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(5f);
        framePaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(18f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setMaskFilter(new BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL));

        scanPaint.setStyle(Paint.Style.STROKE);
        scanPaint.setStrokeWidth(5f);
        scanPaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        particlePaint.setStyle(Paint.Style.FILL);

        textMapPaint.setStyle(Paint.Style.STROKE);
        textMapPaint.setStrokeWidth(3f);
        textMapPaint.setStrokeCap(Paint.Cap.ROUND);

        textMapFillPaint.setStyle(Paint.Style.FILL);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            scanProgress = (float) animation.getAnimatedValue();
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    public void setMode(OverlayMode mode) {
        if (this.mode == mode) {
            return;
        }
        this.mode = mode;
        activeBarcodeRect.setEmpty();
        textRects.clear();
        bestTextIndex = -1;
        barcodeLocked = false;
        invalidate();
    }

    public void setImageSourceInfo(int imageWidth, int imageHeight, boolean isFlipped) {
        if (this.imageWidth == imageWidth && this.imageHeight == imageHeight && this.isImageFlipped == isFlipped) {
            return;
        }
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.isImageFlipped = isFlipped;
        updateTransformationMatrix();
        invalidate();
    }

    public void updateBarcode(@Nullable Rect barcodeRect, boolean locked) {
        barcodeLocked = locked;
        if (barcodeRect == null) {
            activeBarcodeRect.setEmpty();
        } else {
            tempRect.set(barcodeRect);
            transformMatrix.mapRect(tempRect);
            tempRect.inset(-20f, -20f);
            activeBarcodeRect.set(tempRect);
        }
        invalidate();
    }

    public void updateTextBlocks(List<Rect> blocks, int bestIndex) {
        textRects.clear();
        for (Rect block : blocks) {
            tempRect.set(block);
            transformMatrix.mapRect(tempRect);
            tempRect.inset(-8f, -8f);
            textRects.add(new RectF(tempRect));
        }
        bestTextIndex = bestIndex;
        invalidate();
    }

    public void clearDetections() {
        activeBarcodeRect.setEmpty();
        textRects.clear();
        bestTextIndex = -1;
        barcodeLocked = false;
        invalidate();
    }

    public void startScanning() {
        setVisibility(VISIBLE);
        if (animator != null && !animator.isStarted()) {
            animator.start();
        }
    }

    public void stopScanning() {
        clearDetections();
        setVisibility(GONE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateTransformationMatrix();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        if (mode == OverlayMode.BARCODE) {
            drawBarcodeScanner(canvas);
        } else {
            drawIngredientMapper(canvas);
        }
    }

    private void drawBarcodeScanner(Canvas canvas) {
        getBarcodeFocusRect(focusRect);
        int color = barcodeLocked ? Color.WHITE : BARCODE_GREEN;

        drawCornerFrame(canvas, focusRect, color, 6f, 0.12f);
        drawBarcodeScanLine(canvas, focusRect);

        RectF target = activeBarcodeRect.isEmpty() ? focusRect : activeBarcodeRect;
        if (!activeBarcodeRect.isEmpty()) {
            fillPaint.setColor(Color.argb(34, 46, 255, 90));
            canvas.drawRoundRect(activeBarcodeRect, 18f, 18f, fillPaint);
            drawCornerFrame(canvas, activeBarcodeRect, BARCODE_GREEN, barcodeLocked ? 9f : 7f, 0.2f);
            drawBarcodeRungs(canvas, activeBarcodeRect);
        }

        drawSweepSparkles(canvas, target, BARCODE_GREEN);
    }

    private void drawIngredientMapper(Canvas canvas) {
        getIngredientFocusRect(focusRect);
        drawCornerFrame(canvas, focusRect, Color.WHITE, 5f, 0.08f);
        drawIngredientScanLine(canvas, focusRect);

        for (int i = 0; i < textRects.size(); i++) {
            RectF rect = textRects.get(i);
            boolean best = i == bestTextIndex;
            int strokeColor = best ? INGREDIENT_PURPLE : INGREDIENT_BLUE;
            int fillColor = best ? Color.argb(40, 197, 82, 255) : Color.argb(26, 79, 178, 255);

            textMapFillPaint.setColor(fillColor);
            canvas.drawRoundRect(rect, 8f, 8f, textMapFillPaint);

            textMapPaint.setColor(strokeColor);
            textMapPaint.setAlpha(best ? 245 : 160);
            textMapPaint.setStrokeWidth(best ? 4f : 2.5f);
            canvas.drawRoundRect(rect, 8f, 8f, textMapPaint);

            if (best) {
                drawCornerFrame(canvas, rect, INGREDIENT_PURPLE, 6f, 0.16f);
            }
        }

        drawTextMapNodes(canvas);
        drawSweepSparkles(canvas, focusRect, INGREDIENT_PURPLE);
    }

    private void drawBarcodeScanLine(Canvas canvas, RectF rect) {
        float y = rect.top + (rect.height() * scanProgress);
        LinearGradient gradient = new LinearGradient(rect.left, y, rect.right, y,
                new int[]{Color.TRANSPARENT, BARCODE_GREEN, Color.WHITE, BARCODE_GREEN, Color.TRANSPARENT},
                new float[]{0f, 0.22f, 0.5f, 0.78f, 1f},
                Shader.TileMode.CLAMP);

        glowPaint.setShader(gradient);
        glowPaint.setColor(BARCODE_GREEN);
        glowPaint.setAlpha(110);
        canvas.drawLine(rect.left + 12f, y, rect.right - 12f, y, glowPaint);

        scanPaint.setShader(gradient);
        scanPaint.setAlpha(255);
        canvas.drawLine(rect.left + 12f, y, rect.right - 12f, y, scanPaint);
        scanPaint.setShader(null);
        glowPaint.setShader(null);
    }

    private void drawIngredientScanLine(Canvas canvas, RectF rect) {
        float y = rect.top + (rect.height() * scanProgress);
        LinearGradient gradient = new LinearGradient(rect.left, y, rect.right, y,
                new int[]{Color.TRANSPARENT, INGREDIENT_BLUE, INGREDIENT_PURPLE, Color.WHITE, INGREDIENT_PURPLE, Color.TRANSPARENT},
                null,
                Shader.TileMode.CLAMP);

        glowPaint.setShader(gradient);
        glowPaint.setAlpha(115);
        canvas.drawLine(rect.left, y, rect.right, y, glowPaint);

        scanPaint.setShader(gradient);
        scanPaint.setAlpha(255);
        canvas.drawLine(rect.left, y, rect.right, y, scanPaint);
        scanPaint.setShader(null);
        glowPaint.setShader(null);
    }

    private void drawBarcodeRungs(Canvas canvas, RectF rect) {
        scanPaint.setShader(null);
        scanPaint.setColor(Color.argb(150, 46, 255, 90));
        scanPaint.setStrokeWidth(2.5f);
        float gap = Math.max(10f, rect.width() / 18f);
        for (float x = rect.left + gap; x < rect.right - gap; x += gap) {
            float top = rect.top + rect.height() * 0.24f;
            float bottom = rect.bottom - rect.height() * 0.24f;
            canvas.drawLine(x, top, x, bottom, scanPaint);
        }
        scanPaint.setStrokeWidth(5f);
    }

    private void drawTextMapNodes(Canvas canvas) {
        particlePaint.setColor(Color.WHITE);
        particlePaint.setAlpha(180);
        for (RectF rect : textRects) {
            canvas.drawCircle(rect.left, rect.centerY(), 3f, particlePaint);
            canvas.drawCircle(rect.right, rect.centerY(), 3f, particlePaint);
        }
    }

    private void drawSweepSparkles(Canvas canvas, RectF rect, int color) {
        particlePaint.setColor(color);
        for (Particle particle : particles) {
            if (!rect.contains(particle.x, particle.y)) {
                continue;
            }
            particlePaint.setAlpha(particle.alpha);
            canvas.drawCircle(particle.x, particle.y, particle.radius, particlePaint);
        }
    }

    private void drawCornerFrame(Canvas canvas, RectF rect, int color, float strokeWidth, float cornerRatio) {
        framePaint.setColor(color);
        framePaint.setAlpha(230);
        framePaint.setStrokeWidth(strokeWidth);
        framePaint.setShadowLayer(18f, 0f, 0f, color);

        float corner = Math.min(rect.width(), rect.height()) * cornerRatio;
        canvas.drawLine(rect.left, rect.top, rect.left + corner, rect.top, framePaint);
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + corner, framePaint);

        canvas.drawLine(rect.right, rect.top, rect.right - corner, rect.top, framePaint);
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + corner, framePaint);

        canvas.drawLine(rect.left, rect.bottom, rect.left + corner, rect.bottom, framePaint);
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - corner, framePaint);

        canvas.drawLine(rect.right, rect.bottom, rect.right - corner, rect.bottom, framePaint);
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - corner, framePaint);
        framePaint.clearShadowLayer();
    }

    private void getBarcodeFocusRect(RectF rect) {
        float width = getWidth() * 0.84f;
        float height = Math.max(150f, getHeight() * 0.22f);
        float left = (getWidth() - width) / 2f;
        float top = (getHeight() - height) / 2f;
        rect.set(left, top, left + width, top + height);
    }

    private void getIngredientFocusRect(RectF rect) {
        float width = getWidth() * 0.9f;
        float height = getHeight() * 0.58f;
        float left = (getWidth() - width) / 2f;
        float top = getHeight() * 0.23f;
        rect.set(left, top, left + width, top + height);
    }

    private void updateParticles() {
        RectF rect = new RectF();
        if (mode == OverlayMode.BARCODE) {
            getBarcodeFocusRect(rect);
        } else {
            getIngredientFocusRect(rect);
        }

        float y = rect.top + rect.height() * scanProgress;
        if (particles.size() < 46 && random.nextFloat() > 0.48f) {
            particles.add(new Particle(
                    rect.left + random.nextFloat() * rect.width(),
                    y + (random.nextFloat() - 0.5f) * 22f,
                    2.2f + random.nextFloat() * 3.8f
            ));
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.y += (random.nextFloat() - 0.5f) * 8f;
            particle.x += (random.nextFloat() - 0.5f) * 5f;
            particle.alpha -= 9;
            if (particle.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    private void updateTransformationMatrix() {
        if (imageWidth <= 0 || imageHeight <= 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;
        float scale = Math.max(scaleX, scaleY);
        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;
        float dx = (viewWidth - scaledWidth) / 2f;
        float dy = (viewHeight - scaledHeight) / 2f;

        transformMatrix.reset();
        transformMatrix.postScale(scale, scale);
        transformMatrix.postTranslate(dx, dy);

        if (isImageFlipped) {
            transformMatrix.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f);
        }
    }

    private static class Particle {
        final float radius;
        float x;
        float y;
        int alpha = 255;

        Particle(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }
}
