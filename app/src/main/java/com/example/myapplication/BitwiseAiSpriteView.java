package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BitwiseAiSpriteView extends View {

    private final Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ringRect = new RectF();
    private final Matrix matrix = new Matrix();

    private long startTimeMs;
    private RadialGradient auraGradient;
    private LinearGradient coreGradient;
    private RadialGradient highlightGradient;
    private SweepGradient ringGradient;

    public BitwiseAiSpriteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStrokeWidth(3.2f);

        highlightPaint.setStyle(Paint.Style.FILL);
        glintPaint.setStyle(Paint.Style.FILL);
        startTimeMs = SystemClock.uptimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            auraGradient = null;
            coreGradient = null;
            highlightGradient = null;
            ringGradient = null;
            auraPaint.setShader(null);
            corePaint.setShader(null);
            highlightPaint.setShader(null);
            ringPaint.setShader(null);
            return;
        }
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) * 0.22f;

        auraGradient = new RadialGradient(
                cx,
                cy,
                radius * 2.7f,
                new int[]{0x9957D8FF, 0x55D7B8FF, 0x00FFFFFF},
                new float[]{0f, 0.58f, 1f},
                Shader.TileMode.CLAMP
        );
        auraPaint.setShader(auraGradient);

        coreGradient = new LinearGradient(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius,
                new int[]{
                        Color.parseColor("#F7FBFF"),
                        Color.parseColor("#A9E7FF"),
                        Color.parseColor("#8D7CFF"),
                        Color.parseColor("#FF9DDA")
                },
                new float[]{0f, 0.34f, 0.7f, 1f},
                Shader.TileMode.CLAMP
        );
        corePaint.setShader(coreGradient);

        highlightGradient = new RadialGradient(
                cx - radius * 0.35f,
                cy - radius * 0.45f,
                radius * 0.75f,
                new int[]{0xDFFFFFFF, 0x40FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        highlightPaint.setShader(highlightGradient);

        ringGradient = new SweepGradient(
                cx,
                cy,
                new int[]{
                        Color.parseColor("#8EA2FF"),
                        Color.parseColor("#57D8FF"),
                        Color.parseColor("#F7C4FF"),
                        Color.parseColor("#FFD7A8"),
                        Color.parseColor("#8EA2FF")
                },
                null
        );
        ringPaint.setShader(ringGradient);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isShown()) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && isShown()) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) return;
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) * 0.22f;
        float elapsed = (SystemClock.uptimeMillis() - startTimeMs) / 1000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 2.85f);
        float rotation = (elapsed * 50f) % 360f;
        float breath = 1f + pulse * 0.08f;

        auraPaint.setAlpha((int) (190 + pulse * 45));
        canvas.drawCircle(cx, cy, radius * 2.1f * breath, auraPaint);

        canvas.drawCircle(cx, cy, radius * breath, corePaint);

        if (ringGradient != null) {
            matrix.setRotate(rotation, cx, cy);
            ringGradient.setLocalMatrix(matrix);
        }
        ringRect.set(cx - radius * 1.35f, cy - radius * 1.35f, cx + radius * 1.35f, cy + radius * 1.35f);
        ringPaint.setAlpha(180);
        canvas.drawArc(ringRect, rotation, 250f, false, ringPaint);

        canvas.drawCircle(cx - radius * 0.24f, cy - radius * 0.28f, radius * 0.62f, highlightPaint);

        glintPaint.setColor(0xCCFFFFFF);
        canvas.drawCircle(cx + radius * 0.48f, cy - radius * 0.45f, radius * (0.09f + pulse * 0.025f), glintPaint);

        if (isShown()) {
            postInvalidateOnAnimation();
        }
    }
}
