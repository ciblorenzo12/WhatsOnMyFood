package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class PantryHealthProfileView extends View {
    private static final String[] LABELS = {
            "Health",
            "Risk control",
            "User confidence",
            "Calorie budget",
            "Data coverage"
    };

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path gridPath = new Path();
    private final Path profilePath = new Path();
    private float[] scores = new float[] {0f, 0f, 0f, 0f, 0f};
    private PantryRiskScorer.RiskStats stats;

    public PantryHealthProfileView(Context context) {
        super(context);
        init();
    }

    public PantryHealthProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setColor(Color.argb(68, 75, 85, 99));

        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dp(1));
        axisPaint.setColor(Color.argb(90, 17, 24, 39));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(76, 124, 77, 255));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(3));
        strokePaint.setColor(Color.rgb(124, 77, 255));

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(Color.rgb(5, 150, 105));

        textPaint.setTextSize(sp(12));
        textPaint.setColor(Color.rgb(31, 41, 55));
        textPaint.setFakeBoldText(true);
    }

    public void setStats(PantryRiskScorer.RiskStats stats) {
        this.stats = stats;
        if (stats == null || stats.itemCount == 0) {
            scores = new float[] {0f, 0f, 0f, 0f, 0f};
        } else {
            float health = clamp(stats.healthScoreCount == 0 ? 0 : stats.averageHealthScore);
            float riskControl = clamp(100 - stats.averageCombinedRisk);
            float userConfidence = stats.userRatedCount == 0 ? 0 : clamp(100 - stats.averageUserRisk);
            float calorieBudget = stats.calorieCount == 0 ? 0 : clamp(100 - stats.dailyCaloriesPercent);
            float coverage = clamp(((stats.healthScoreCount + stats.calorieCount + stats.userRatedCount) * 100f) / (stats.itemCount * 3f));
            scores = new float[] {health, riskControl, userConfidence, calorieBudget, coverage};
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) return;

        float centerX = getPaddingLeft() + width / 2f;
        float centerY = getPaddingTop() + height / 2f + dp(10);
        float radius = Math.min(width, height) * 0.30f;

        textPaint.setTextSize(sp(15));
        textPaint.setFakeBoldText(true);
        canvas.drawText("Pantry Health Profile", getPaddingLeft(), getPaddingTop() + dp(20), textPaint);
        textPaint.setTextSize(sp(11));
        textPaint.setFakeBoldText(false);
        canvas.drawText("A quick view of quality, risk, calories, and data coverage", getPaddingLeft(), getPaddingTop() + dp(40), textPaint);

        if (stats == null || stats.itemCount == 0) {
            textPaint.setTextSize(sp(13));
            textPaint.setFakeBoldText(true);
            canvas.drawText("Add pantry products to build this profile.", getPaddingLeft(), centerY, textPaint);
            return;
        }

        for (int level = 1; level <= 4; level++) {
            drawPolygon(canvas, centerX, centerY, radius * level / 4f, gridPaint, null);
        }

        for (int index = 0; index < LABELS.length; index++) {
            double angle = angleFor(index);
            float endX = centerX + (float) Math.cos(angle) * radius;
            float endY = centerY + (float) Math.sin(angle) * radius;
            canvas.drawLine(centerX, centerY, endX, endY, axisPaint);
            drawAxisLabel(canvas, LABELS[index], scores[index], centerX, centerY, radius, angle);
        }

        profilePath.reset();
        for (int index = 0; index < scores.length; index++) {
            double angle = angleFor(index);
            float scaledRadius = radius * scores[index] / 100f;
            float x = centerX + (float) Math.cos(angle) * scaledRadius;
            float y = centerY + (float) Math.sin(angle) * scaledRadius;
            if (index == 0) {
                profilePath.moveTo(x, y);
            } else {
                profilePath.lineTo(x, y);
            }
        }
        profilePath.close();

        fillPaint.setShader(new LinearGradient(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                Color.argb(110, 124, 77, 255),
                Color.argb(76, 5, 150, 105),
                Shader.TileMode.CLAMP));
        canvas.drawPath(profilePath, fillPaint);
        fillPaint.setShader(null);
        canvas.drawPath(profilePath, strokePaint);

        for (int index = 0; index < scores.length; index++) {
            double angle = angleFor(index);
            float scaledRadius = radius * scores[index] / 100f;
            float x = centerX + (float) Math.cos(angle) * scaledRadius;
            float y = centerY + (float) Math.sin(angle) * scaledRadius;
            canvas.drawCircle(x, y, dp(5), pointPaint);
        }
    }

    private void drawPolygon(Canvas canvas, float centerX, float centerY, float radius, Paint paint, Path outPath) {
        Path path = outPath == null ? gridPath : outPath;
        path.reset();
        for (int index = 0; index < LABELS.length; index++) {
            double angle = angleFor(index);
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;
            if (index == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawAxisLabel(Canvas canvas, String label, float score, float centerX, float centerY, float radius, double angle) {
        float labelRadius = radius + dp(34);
        float x = centerX + (float) Math.cos(angle) * labelRadius;
        float y = centerY + (float) Math.sin(angle) * labelRadius;
        String text = label + " " + Math.round(score);
        float textWidth = textPaint.measureText(text);
        if (x > centerX + dp(10)) {
            canvas.drawText(text, x, y, textPaint);
        } else if (x < centerX - dp(10)) {
            canvas.drawText(text, x - textWidth, y, textPaint);
        } else {
            canvas.drawText(text, x - textWidth / 2f, y, textPaint);
        }
    }

    private double angleFor(int index) {
        return -Math.PI / 2d + (Math.PI * 2d * index / LABELS.length);
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
