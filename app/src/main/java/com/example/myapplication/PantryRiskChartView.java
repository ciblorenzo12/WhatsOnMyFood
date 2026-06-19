package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PantryRiskChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private List<PantryRiskScorer.RiskItem> items = new ArrayList<>();

    public PantryRiskChartView(Context context) {
        super(context);
        init();
    }

    public PantryRiskChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setColor(Color.argb(42, 17, 24, 39));
        guidePaint.setColor(Color.argb(84, 17, 24, 39));
        guidePaint.setStrokeWidth(dp(1));
        textPaint.setColor(Color.rgb(26, 28, 30));
        textPaint.setTextSize(sp(12));
        textPaint.setFakeBoldText(true);
    }

    public void setItems(List<PantryRiskScorer.RiskItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        if (width <= 0 || height <= 0) return;

        if (items.isEmpty()) {
            textPaint.setTextSize(sp(14));
            canvas.drawText(getContext().getString(R.string.no_pantry_scores), left, top + height / 2f, textPaint);
            textPaint.setTextSize(sp(12));
            return;
        }

        int count = Math.min(items.size(), 5);
        float gap = dp(14);
        float rowHeight = (height - (gap * (count - 1))) / count;
        float labelWidth = Math.min(dp(142), width * 0.34f);
        float barLeft = left + labelWidth + dp(10);
        float barWidth = width - labelWidth - dp(10);

        drawRiskGuides(canvas, barLeft, top, barWidth, height);

        for (int index = 0; index < count; index++) {
            PantryRiskScorer.RiskItem item = items.get(index);
            float rowTop = top + index * (rowHeight + gap);
            String name = item.product.productName == null ? "Pantry item" : item.product.productName;
            textPaint.setFakeBoldText(true);
            textPaint.setTextSize(sp(12));
            canvas.drawText(ellipsize(name, labelWidth), left, rowTop + dp(22), textPaint);

            String details = "Health " + (item.product.healthScore == null ? "--" : item.product.healthScore)
                    + " | " + (item.caloriesPer100g == null ? "-- kcal" : Math.round(item.caloriesPer100g) + " kcal");
            textPaint.setFakeBoldText(false);
            textPaint.setTextSize(sp(10));
            canvas.drawText(ellipsize(details, labelWidth), left, rowTop + dp(42), textPaint);

            float barTop = rowTop + dp(10);
            float barBottom = Math.min(rowTop + rowHeight - dp(10), barTop + dp(34));
            rect.set(barLeft, barTop, barLeft + barWidth, barBottom);
            canvas.drawRoundRect(rect, dp(8), dp(8), trackPaint);
            barPaint.setColor(colorForRisk(item.combinedRisk));
            rect.right = barLeft + (barWidth * item.combinedRisk / 100f);
            canvas.drawRoundRect(rect, dp(8), dp(8), barPaint);

            String score = String.valueOf(item.combinedRisk);
            textPaint.setTextSize(sp(12));
            textPaint.setFakeBoldText(true);
            float scoreY = barTop + ((barBottom - barTop) / 2f) + dp(5);
            canvas.drawText(score, barLeft + barWidth - textPaint.measureText(score) - dp(8), scoreY, textPaint);
            textPaint.setFakeBoldText(false);
            textPaint.setTextSize(sp(10));
            canvas.drawText("Combined risk", barLeft, Math.min(rowTop + rowHeight - dp(2), barBottom + dp(18)), textPaint);
            drawComponentLabels(canvas, item, barLeft + dp(88), Math.min(rowTop + rowHeight - dp(2), barBottom + dp(18)));
            textPaint.setTextSize(sp(12));
            textPaint.setFakeBoldText(true);
        }
    }

    private void drawRiskGuides(Canvas canvas, float barLeft, float top, float barWidth, float height) {
        float moderateX = barLeft + barWidth * 0.40f;
        float highX = barLeft + barWidth * 0.70f;
        canvas.drawLine(moderateX, top, moderateX, top + height, guidePaint);
        canvas.drawLine(highX, top, highX, top + height, guidePaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(9));
        textPaint.setColor(Color.rgb(75, 85, 99));
        canvas.drawText("40", moderateX + dp(3), top + dp(9), textPaint);
        canvas.drawText("70", highX + dp(3), top + dp(9), textPaint);
        textPaint.setColor(Color.rgb(26, 28, 30));
    }

    private void drawComponentLabels(Canvas canvas, PantryRiskScorer.RiskItem item, float x, float y) {
        String ai = item.aiRisk == null ? "Health --" : "Health risk " + item.aiRisk;
        String user = item.userRisk == 0 ? "User --" : "User " + item.userRisk;
        String calorie = item.calorieRisk == null ? "Kcal --" : "Kcal risk " + item.calorieRisk;
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(10));
        textPaint.setColor(Color.rgb(75, 85, 99));
        canvas.drawText(ellipsize(ai + "  |  " + user + "  |  " + calorie, getWidth() - x - getPaddingRight()), x, y, textPaint);
        textPaint.setColor(Color.rgb(26, 28, 30));
    }

    private String ellipsize(String text, float maxWidth) {
        if (textPaint.measureText(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && textPaint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end == 0 ? suffix : text.substring(0, end) + suffix;
    }

    private int colorForRisk(int risk) {
        if (risk >= 70) return Color.rgb(239, 68, 68);
        if (risk >= 40) return Color.rgb(245, 158, 11);
        return Color.rgb(5, 150, 105);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
