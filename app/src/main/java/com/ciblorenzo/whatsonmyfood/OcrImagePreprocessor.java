package com.ciblorenzo.whatsonmyfood;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/** Produces a higher-contrast grayscale fallback for low-quality imported labels. */
public final class OcrImagePreprocessor {

    private static final int TARGET_SHORT_EDGE = 1200;
    private static final int MAX_LONG_EDGE = 2400;

    private OcrImagePreprocessor() {
    }

    public static Bitmap enhance(Bitmap source) {
        if (source == null) return null;
        int width = source.getWidth();
        int height = source.getHeight();
        int shortEdge = Math.min(width, height);
        int longEdge = Math.max(width, height);
        float scale = Math.max(1f, (float) TARGET_SHORT_EDGE / Math.max(1, shortEdge));
        scale = Math.min(scale, 2f);
        scale = Math.min(scale, (float) MAX_LONG_EDGE / Math.max(1, longEdge));

        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        Bitmap scaled = (targetWidth == width && targetHeight == height)
                ? source
                : Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);

        Bitmap enhanced = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(enhanced);
        ColorMatrix grayscale = new ColorMatrix();
        grayscale.setSaturation(0f);
        float contrast = 1.28f;
        float offset = 128f * (1f - contrast) + 8f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, offset,
                0, contrast, 0, 0, offset,
                0, 0, contrast, 0, offset,
                0, 0, 0, 1, 0
        });
        grayscale.postConcat(contrastMatrix);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(grayscale));
        canvas.drawBitmap(scaled, 0, 0, paint);
        if (scaled != source) scaled.recycle();
        return enhanced;
    }
}
