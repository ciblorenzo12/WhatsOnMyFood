package com.ciblorenzo.whatsonmyfood;

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

public class PantryArGraphOverlayView extends View {
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private List<PantryRiskScorer.RiskItem> items = new ArrayList<>();
    private PantryRiskScorer.RiskStats stats = PantryRiskScorer.stats(items);

    public PantryArGraphOverlayView(Context context) {
        super(context);
        init();
    }

    public PantryArGraphOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        panelPaint.setColor(Color.argb(220, 255, 255, 255));
        linePaint.setColor(Color.argb(150, 255, 255, 255));
        linePaint.setStrokeWidth(dp(1));
        titlePaint.setColor(Color.rgb(17, 24, 39));
        titlePaint.setTextSize(sp(18));
        titlePaint.setFakeBoldText(true);
        bodyPaint.setColor(Color.rgb(31, 41, 55));
        bodyPaint.setTextSize(sp(12));
        setWillNotDraw(false);
    }

    public void setItems(List<PantryRiskScorer.RiskItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.stats = PantryRiskScorer.stats(this.items);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        drawAnchorGrid(canvas, width, height);
        float panelLeft = dp(18);
        float panelTop = Math.max(dp(72), height * 0.18f);
        float panelRight = width - dp(18);
        float panelBottom = Math.min(height - dp(48), panelTop + dp(360));
        rect.set(panelLeft, panelTop, panelRight, panelBottom);
        canvas.drawRoundRect(rect, dp(22), dp(22), panelPaint);

        float x = panelLeft + dp(18);
        float y = panelTop + dp(34);
        canvas.drawText("Pantry Risk AR", x, y, titlePaint);
        y += dp(28);
        canvas.drawText("Combined avg " + stats.averageCombinedRisk + " | High risk " + stats.highRiskCount, x, y, bodyPaint);

        if (items.isEmpty()) {
            y += dp(36);
            canvas.drawText(getContext().getString(R.string.no_pantry_scores), x, y, bodyPaint);
            return;
        }

        y += dp(24);
        float barLeft = x;
        float barRight = panelRight - dp(20);
        int count = Math.min(5, items.size());
        for (int i = 0; i < count; i++) {
            PantryRiskScorer.RiskItem item = items.get(i);
            String name = item.product.productName == null ? "Pantry item" : item.product.productName;
            canvas.drawText(trim(name, 24), barLeft, y, bodyPaint);
            y += dp(8);
            rect.set(barLeft, y, barRight, y + dp(16));
            barPaint.setColor(Color.argb(45, 17, 24, 39));
            canvas.drawRoundRect(rect, dp(8), dp(8), barPaint);
            barPaint.setColor(colorForRisk(item.combinedRisk));
            rect.right = barLeft + ((barRight - barLeft) * item.combinedRisk / 100f);
            canvas.drawRoundRect(rect, dp(8), dp(8), barPaint);
            canvas.drawText(String.valueOf(item.combinedRisk), barRight - dp(28), y + dp(13), bodyPaint);
            y += dp(34);
        }
    }

    private void drawAnchorGrid(Canvas canvas, float width, float height) {
        for (int i = 1; i < 4; i++) {
            float x = width * i / 4f;
            canvas.drawLine(x, dp(72), x, height - dp(36), linePaint);
        }
        for (int i = 1; i < 5; i++) {
            float y = height * i / 5f;
            canvas.drawLine(dp(16), y, width - dp(16), y, linePaint);
        }
    }

    private String trim(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
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
