package com.ciblorenzo.whatsonmyfood;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.mlkit.vision.barcode.common.Barcode;

public class BarcodeGraphic extends GraphicOverlay.Graphic {

    private static final float STROKE_WIDTH = 12.0f;
    private static final int ANIMATION_DURATION_MS = 75;
    private static final float PADDING = 20.0f;

    private final Paint paint;
    private final RectF animatedBoundingBox;
    private ValueAnimator boxAnimator;

    public BarcodeGraphic(GraphicOverlay overlay) {
        super(overlay);

        paint = new Paint();
        paint.setColor(Color.parseColor("#4CAF50")); // Modern green
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setShadowLayer(10.0f, 0f, 0f, Color.argb(128, 76, 175, 80));

        animatedBoundingBox = new RectF();
    }

    public void update(Barcode barcode) {
        if (barcode == null) return;

        RectF newBoundingBox = new RectF(barcode.getBoundingBox());
        getOverlay().getTransformMatrix().mapRect(newBoundingBox);
        newBoundingBox.inset(-PADDING, -PADDING);

        if (animatedBoundingBox.isEmpty()) {
            animatedBoundingBox.set(newBoundingBox);
        }

        if (boxAnimator != null) {
            boxAnimator.cancel();
        }

        boxAnimator = ValueAnimator.ofObject(new RectFEvaluator(), new RectF(animatedBoundingBox), newBoundingBox);
        boxAnimator.setDuration(ANIMATION_DURATION_MS);
        boxAnimator.addUpdateListener(animation -> {
            animatedBoundingBox.set((RectF) animation.getAnimatedValue());
            getOverlay().postInvalidate();
        });
        boxAnimator.start();
    }

    public void hide() {
        animatedBoundingBox.setEmpty();
        getOverlay().postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!animatedBoundingBox.isEmpty()) {
            float cornerSize = Math.min(animatedBoundingBox.width(), animatedBoundingBox.height()) * 0.25f; // 25% of the smaller side

            // Top-left corner
            canvas.drawLine(animatedBoundingBox.left, animatedBoundingBox.top, animatedBoundingBox.left + cornerSize, animatedBoundingBox.top, paint);
            canvas.drawLine(animatedBoundingBox.left, animatedBoundingBox.top, animatedBoundingBox.left, animatedBoundingBox.top + cornerSize, paint);

            // Top-right corner
            canvas.drawLine(animatedBoundingBox.right, animatedBoundingBox.top, animatedBoundingBox.right - cornerSize, animatedBoundingBox.top, paint);
            canvas.drawLine(animatedBoundingBox.right, animatedBoundingBox.top, animatedBoundingBox.right, animatedBoundingBox.top + cornerSize, paint);

            // Bottom-left corner
            canvas.drawLine(animatedBoundingBox.left, animatedBoundingBox.bottom, animatedBoundingBox.left + cornerSize, animatedBoundingBox.bottom, paint);
            canvas.drawLine(animatedBoundingBox.left, animatedBoundingBox.bottom, animatedBoundingBox.left, animatedBoundingBox.bottom - cornerSize, paint);

            // Bottom-right corner
            canvas.drawLine(animatedBoundingBox.right, animatedBoundingBox.bottom, animatedBoundingBox.right - cornerSize, animatedBoundingBox.bottom, paint);
            canvas.drawLine(animatedBoundingBox.right, animatedBoundingBox.bottom, animatedBoundingBox.right, animatedBoundingBox.bottom - cornerSize, paint);
        }
    }
}
