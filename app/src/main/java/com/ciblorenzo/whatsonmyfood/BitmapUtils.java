package com.ciblorenzo.whatsonmyfood;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class BitmapUtils {

    /**
     * Converts an ImageProxy (YUV_420_888) to a Bitmap.
     * This version uses a highly compatible manual copy to ensure no stripes on any hardware.
     */
    public static Bitmap getBitmap(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            return null;
        }

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        // Standard NV21 is YYYY... VUVU...
        // Size = width * height (Y) + width * height / 2 (UV)
        byte[] nv21 = new byte[width * height * 3 / 2];

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uRowStride = planes[1].getRowStride();
        int vRowStride = planes[2].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vPixelStride = planes[2].getPixelStride();

        // 1. Copy Y plane
        int pos = 0;
        if (yRowStride == width) {
            yBuffer.get(nv21, 0, width * height);
            pos = width * height;
        } else {
            for (int y = 0; y < height; y++) {
                yBuffer.position(y * yRowStride);
                yBuffer.get(nv21, pos, width);
                pos += width;
            }
        }

        // 2. Interleave U and V planes for NV21
        // NV21 requires V then U
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = pos + row * width + col * 2;
                nv21[vuPos] = vBuffer.get(row * vRowStride + col * vPixelStride);
                nv21[vuPos + 1] = uBuffer.get(row * uRowStride + col * uPixelStride);
            }
        }

        try {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);

            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            return rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap cropCenter(Bitmap bitmap, float percentage) {
        if (bitmap == null) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (int) (width * percentage);
        int newHeight = (int) (height * percentage);
        int left = (width - newWidth) / 2;
        int top = (height - newHeight) / 2;
        return Bitmap.createBitmap(bitmap, left, top, newWidth, newHeight);
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) return new byte[0];

        int maxWidth = 800;
        int maxHeight = 800;
        float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());

        Bitmap resized;
        if (ratio < 1.0f) {
            int width = Math.round(ratio * bitmap.getWidth());
            int height = Math.round(ratio * bitmap.getHeight());
            resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
        } else {
            resized = bitmap;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 75, stream);
        return stream.toByteArray();
    }
}
