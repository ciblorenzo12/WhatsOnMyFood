package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.List;

public final class PantryRiskBitmapRenderer {
    private PantryRiskBitmapRenderer() {
    }

    public static Bitmap render(Context context, List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats) {
        int width = 1080;
        int height = 620;
        float density = context.getResources().getDisplayMetrics().density;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.rgb(248, 249, 255));
        canvas.drawColor(backgroundPaint.getColor());

        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.rgb(17, 24, 39));
        titlePaint.setTextSize(34 * density);
        titlePaint.setFakeBoldText(true);

        TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.rgb(75, 85, 99));
        labelPaint.setTextSize(18 * density);

        TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.rgb(17, 24, 39));
        valuePaint.setTextSize(24 * density);
        valuePaint.setFakeBoldText(true);

        Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelPaint.setColor(Color.WHITE);
        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(Color.rgb(229, 231, 235));
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF();

        canvas.drawText("Pantry Risk Data Visualization", 48, 62, titlePaint);
        canvas.drawText("Health scores, daily calories, and user ingredient concern", 48, 98, labelPaint);

        drawMetric(canvas, panelPaint, valuePaint, labelPaint, rect, 48, 130, "Combined", stats.averageCombinedRisk + "/100", Color.rgb(239, 68, 68));
        drawMetric(canvas, panelPaint, valuePaint, labelPaint, rect, 304, 130, "Health avg", stats.healthScoreCount == 0 ? "--" : stats.averageHealthScore + "/100", Color.rgb(5, 150, 105));
        drawMetric(canvas, panelPaint, valuePaint, labelPaint, rect, 560, 130, "User avg", stats.userRatedCount == 0 ? "--" : stats.averageUserRisk + "/100", Color.rgb(245, 158, 11));
        drawMetric(canvas, panelPaint, valuePaint, labelPaint, rect, 816, 130, "Daily kcal", stats.calorieCount == 0 ? "--" : stats.dailyCaloriesPercent + "%", Color.rgb(124, 77, 255));

        canvas.drawText("Total pantry kcal: " + stats.totalCalories + " / " + PantryRiskScorer.RECOMMENDED_DAILY_CALORIES, 82, 270, labelPaint);

        int low = 0;
        int moderate = 0;
        int high = 0;
        if (items != null) {
            for (PantryRiskScorer.RiskItem item : items) {
                if (item.combinedRisk >= 70) {
                    high++;
                } else if (item.combinedRisk >= 40) {
                    moderate++;
                } else {
                    low++;
                }
            }
        }

        rect.set(48, 292, 1032, 570);
        canvas.drawRoundRect(rect, 28, 28, panelPaint);
        canvas.drawText("Risk distribution", 82, 340, valuePaint);

        int maxBucket = Math.max(1, Math.max(low, Math.max(moderate, high)));
        drawBucket(canvas, trackPaint, barPaint, labelPaint, valuePaint, rect, 110, 394, 170, low, maxBucket, "Low", Color.rgb(5, 150, 105));
        drawBucket(canvas, trackPaint, barPaint, labelPaint, valuePaint, rect, 448, 394, 170, moderate, maxBucket, "Moderate", Color.rgb(245, 158, 11));
        drawBucket(canvas, trackPaint, barPaint, labelPaint, valuePaint, rect, 786, 394, 170, high, maxBucket, "High", Color.rgb(239, 68, 68));

        return bitmap;
    }

    private static void drawMetric(Canvas canvas, Paint panelPaint, TextPaint valuePaint, TextPaint labelPaint, RectF rect, float left, float top, String label, String value, int accentColor) {
        rect.set(left, top, left + 216, top + 112);
        canvas.drawRoundRect(rect, 24, 24, panelPaint);
        valuePaint.setColor(accentColor);
        canvas.drawText(value, left + 24, top + 52, valuePaint);
        canvas.drawText(label, left + 24, top + 86, labelPaint);
        valuePaint.setColor(Color.rgb(17, 24, 39));
    }

    private static void drawBucket(Canvas canvas, Paint trackPaint, Paint barPaint, TextPaint labelPaint, TextPaint valuePaint, RectF rect, float left, float top, float maxHeight, int count, int maxBucket, String label, int color) {
        float barHeight = maxHeight * count / maxBucket;
        rect.set(left, top, left + 150, top + maxHeight);
        canvas.drawRoundRect(rect, 18, 18, trackPaint);
        barPaint.setColor(color);
        rect.set(left, top + maxHeight - barHeight, left + 150, top + maxHeight);
        canvas.drawRoundRect(rect, 18, 18, barPaint);
        canvas.drawText(String.valueOf(count), left + 56, top + maxHeight - barHeight - 16, valuePaint);
        canvas.drawText(label, left + 24, top + maxHeight + 42, labelPaint);
    }
}
