package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PantryRiskBitmapRenderer {
    private PantryRiskBitmapRenderer() {
    }

    public static Bitmap render(Context context, List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats) {
        int width = 1080;
        int height = 860;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.rgb(248, 249, 255));
        canvas.drawColor(backgroundPaint.getColor());

        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.rgb(17, 24, 39));
        titlePaint.setTextSize(38);
        titlePaint.setFakeBoldText(true);

        TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.rgb(75, 85, 99));
        labelPaint.setTextSize(22);

        TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.rgb(17, 24, 39));
        valuePaint.setTextSize(28);
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

        rect.set(48, 610, 1032, 820);
        canvas.drawRoundRect(rect, 28, 28, panelPaint);
        canvas.drawText("Calories by product", 82, 660, valuePaint);
        drawCalories(canvas, trackPaint, barPaint, labelPaint, valuePaint, rect, items, stats, 82, 694, 900);

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

    private static void drawCalories(Canvas canvas, Paint trackPaint, Paint barPaint, TextPaint labelPaint, TextPaint valuePaint, RectF rect, List<PantryRiskScorer.RiskItem> items, PantryRiskScorer.RiskStats stats, float left, float top, float width) {
        if (items == null || items.isEmpty() || stats.totalCalories <= 0) {
            canvas.drawText("No calorie data available yet.", left, top + 38, labelPaint);
            return;
        }

        List<PantryRiskScorer.RiskItem> calorieItems = new ArrayList<>(items);
        Collections.sort(calorieItems, new Comparator<PantryRiskScorer.RiskItem>() {
            @Override
            public int compare(PantryRiskScorer.RiskItem left, PantryRiskScorer.RiskItem right) {
                double leftCalories = left.caloriesPer100g == null ? 0 : left.caloriesPer100g;
                double rightCalories = right.caloriesPer100g == null ? 0 : right.caloriesPer100g;
                return Double.compare(rightCalories, leftCalories);
            }
        });

        int drawn = 0;
        for (PantryRiskScorer.RiskItem item : calorieItems) {
            if (item.caloriesPer100g == null || item.product == null) continue;
            float rowTop = top + drawn * 42;
            int calories = (int) Math.round(item.caloriesPer100g);
            int percent = Math.round(calories * 100f / stats.totalCalories);
            String name = item.product.productName == null ? "Pantry item" : item.product.productName;
            canvas.drawText(ellipsize(name, labelPaint, 280), left, rowTop + 24, labelPaint);
            rect.set(left + 310, rowTop + 4, left + width, rowTop + 30);
            canvas.drawRoundRect(rect, 12, 12, trackPaint);
            barPaint.setColor(Color.rgb(124, 77, 255));
            rect.right = left + 310 + ((width - 310) * Math.max(2, percent) / 100f);
            canvas.drawRoundRect(rect, 12, 12, barPaint);
            canvas.drawText(calories + " kcal  " + percent + "%", left + width - 190, rowTop + 24, labelPaint);
            drawn++;
            if (drawn >= 3) break;
        }
    }

    private static String ellipsize(String text, TextPaint paint, float maxWidth) {
        if (text == null) return "";
        if (paint.measureText(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }
}
