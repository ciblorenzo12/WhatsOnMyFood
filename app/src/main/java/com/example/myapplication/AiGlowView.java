package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Soft aurora treatment for Bitwise thinking states.
 * Draws a calm animated border and edge glow instead of a loud neon outline.
 */
public class AiGlowView extends View {
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Matrix matrix = new Matrix();
    private final Path sheenPath = new Path();
    private SweepGradient gradient;
    private long startTime;
    private boolean screenBorder;

    private final int[] colors = {
            0xFF7C8CFF,
            0xFF5EE6FF,
            0xFFE8D7FF,
            0xFFFF8FD8,
            0xFFFFD3A5,
            0xFF7C8CFF
    };

    public AiGlowView(Context context) {
        super(context);
        init();
    }

    public AiGlowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        bloomPaint.setStyle(Paint.Style.STROKE);
        bloomPaint.setStrokeCap(Paint.Cap.ROUND);
        bloomPaint.setStrokeJoin(Paint.Join.ROUND);

        sheenPaint.setStyle(Paint.Style.STROKE);
        sheenPaint.setStrokeCap(Paint.Cap.ROUND);
        sheenPaint.setStrokeWidth(2f);
        sheenPaint.setColor(0x88FFFFFF);

        startTime = System.currentTimeMillis();
    }

    public void setScreenBorder(boolean screenBorder) {
        this.screenBorder = screenBorder;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            gradient = null;
            strokePaint.setShader(null);
            bloomPaint.setShader(null);
            return;
        }
        gradient = new SweepGradient(w / 2f, h / 2f, colors, null);
        strokePaint.setShader(gradient);
        bloomPaint.setShader(gradient);
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gradient == null) return;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) return;
        float minSide = Math.min(width, height);
        boolean fullScreen = screenBorder || getId() == R.id.loading_glow || (width >= dp(280f) && height >= dp(420f));

        matrix.setRotate(elapsed * 18f, width / 2f, height / 2f);
        gradient.setLocalMatrix(matrix);

        float inset = fullScreen ? dp(10f) : dp(12f);
        float radius = fullScreen ? dp(30f) : Math.min(dp(28f), minSide * 0.18f);
        bounds.set(inset, inset, width - inset, height - inset);

        float breath = 0.5f + 0.5f * (float) Math.sin(elapsed * 1.6f);

        bloomPaint.setStrokeWidth(fullScreen ? dp(34f) : dp(18f));
        bloomPaint.setAlpha((int) (fullScreen ? 34 + breath * 18 : 74 + breath * 16));
        canvas.drawRoundRect(bounds, radius, radius, bloomPaint);

        bloomPaint.setStrokeWidth(fullScreen ? dp(18f) : dp(10f));
        bloomPaint.setAlpha((int) (fullScreen ? 42 + breath * 18 : 86 + breath * 18));
        canvas.drawRoundRect(bounds, radius, radius, bloomPaint);

        strokePaint.setStrokeWidth(fullScreen ? dp(2.6f) : dp(3.4f));
        strokePaint.setAlpha((int) (fullScreen ? 128 + breath * 48 : 150 + breath * 60));
        canvas.drawRoundRect(bounds, radius, radius, strokePaint);

        drawTravelingSheen(canvas, elapsed, bounds, radius, fullScreen);

        if (isShown()) {
            postInvalidateOnAnimation();
        }
    }

    private void drawTravelingSheen(Canvas canvas, float time, RectF rect, float radius, boolean fullScreen) {
        float perimeter = 2f * (rect.width() + rect.height());
        float distance = (time * (fullScreen ? dp(150f) : dp(110f))) % perimeter;
        float segment = fullScreen ? dp(210f) : dp(90f);

        sheenPaint.setStrokeWidth(fullScreen ? dp(2.4f) : dp(2f));
        sheenPaint.setAlpha(fullScreen ? 104 : 86);
        sheenPath.reset();

        addBorderSegment(sheenPath, rect, radius, distance, segment);
        canvas.drawPath(sheenPath, sheenPaint);
    }

    private void addBorderSegment(Path path, RectF rect, float radius, float start, float length) {
        float perimeter = 2f * (rect.width() + rect.height());
        float end = start + length;
        float step = dp(8f);

        for (float d = start; d <= end; d += step) {
            float normalized = d % perimeter;
            float x;
            float y;

            if (normalized < rect.width()) {
                x = rect.left + normalized;
                y = rect.top;
            } else if (normalized < rect.width() + rect.height()) {
                x = rect.right;
                y = rect.top + normalized - rect.width();
            } else if (normalized < rect.width() * 2f + rect.height()) {
                x = rect.right - (normalized - rect.width() - rect.height());
                y = rect.bottom;
            } else {
                x = rect.left;
                y = rect.bottom - (normalized - rect.width() * 2f - rect.height());
            }

            x = Math.max(rect.left + radius * 0.35f, Math.min(rect.right - radius * 0.35f, x));
            y = Math.max(rect.top + radius * 0.35f, Math.min(rect.bottom - radius * 0.35f, y));

            if (d == start) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
