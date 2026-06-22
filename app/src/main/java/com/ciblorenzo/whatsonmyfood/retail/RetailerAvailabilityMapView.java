package com.ciblorenzo.whatsonmyfood.retail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RetailerAvailabilityMapView extends View {
    private final List<RetailerAvailability> items = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public RetailerAvailabilityMapView(Context context) {
        super(context);
        init();
    }

    public RetailerAvailabilityMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RetailerAvailabilityMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void submitList(@Nullable List<RetailerAvailability> newItems) {
        items.clear();
        if (newItems != null) {
            for (RetailerAvailability item : newItems) {
                if (item != null && item.distanceValue < 95.0) {
                    items.add(item);
                }
                if (items.size() >= 6) break;
            }
        }
        invalidate();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        drawBaseMap(canvas, width, height);
        drawHomeMarker(canvas, width, height);

        if (items.isEmpty()) {
            drawEmptyState(canvas, width, height);
            return;
        }

        float maxDistance = 1f;
        for (RetailerAvailability item : items) {
            if (item.distanceValue > maxDistance && item.distanceValue < 95.0) {
                maxDistance = (float) item.distanceValue;
            }
        }

        for (int i = 0; i < items.size(); i++) {
            drawStoreMarker(canvas, items.get(i), i, maxDistance, width, height);
        }
    }

    private void drawBaseMap(Canvas canvas, float width, float height) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 248, 255));
        rect.set(0, 0, width, height);
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(8));
        paint.setColor(Color.rgb(224, 232, 252));
        canvas.drawLine(dp(20), height * 0.32f, width - dp(20), height * 0.25f, paint);
        canvas.drawLine(dp(28), height * 0.74f, width - dp(18), height * 0.62f, paint);
        canvas.drawLine(width * 0.24f, dp(16), width * 0.34f, height - dp(18), paint);
        canvas.drawLine(width * 0.68f, dp(20), width * 0.58f, height - dp(22), paint);

        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.rgb(203, 214, 242));
        canvas.drawLine(dp(20), height * 0.32f, width - dp(20), height * 0.25f, paint);
        canvas.drawLine(dp(28), height * 0.74f, width - dp(18), height * 0.62f, paint);
        canvas.drawLine(width * 0.24f, dp(16), width * 0.34f, height - dp(18), paint);
        canvas.drawLine(width * 0.68f, dp(20), width * 0.58f, height - dp(22), paint);
    }

    private void drawHomeMarker(Canvas canvas, float width, float height) {
        float x = width * 0.5f;
        float y = height * 0.5f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(18), paint);
        paint.setColor(Color.rgb(124, 77, 255));
        canvas.drawCircle(x, y, dp(11), paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(4), paint);
    }

    private void drawStoreMarker(Canvas canvas, RetailerAvailability item, int index, float maxDistance, float width, float height) {
        float angle = (float) Math.toRadians(-92 + index * 58);
        float normalized = item.distanceValue > 0 ? (float) item.distanceValue / maxDistance : 0.45f;
        float radius = dp(38) + normalized * Math.min(width, height) * 0.24f;
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float x = clamp(centerX + (float) Math.cos(angle) * radius, dp(38), width - dp(38));
        float y = clamp(centerY + (float) Math.sin(angle) * radius, dp(40), height - dp(42));

        int brandColor = RetailerBrandAssets.resolve(item.retailerName).brandColor;
        int statusColor = item.available ? Color.rgb(5, 122, 69) : Color.rgb(107, 114, 128);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(54, Color.red(brandColor), Color.green(brandColor), Color.blue(brandColor)));
        canvas.drawCircle(x, y, dp(22), paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(16), paint);
        paint.setColor(brandColor);
        canvas.drawCircle(x, y, dp(10), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(statusColor);
        canvas.drawCircle(x, y, dp(17), paint);

        drawLabel(canvas, item, x, y + dp(33), brandColor);
    }

    private void drawLabel(Canvas canvas, RetailerAvailability item, float x, float y, int brandColor) {
        String name = shortName(item.retailerName);
        String price = item.price == null || item.price.trim().isEmpty() ? "Price varies" : item.price.trim();
        String distance = item.distance == null || item.distance.trim().isEmpty()
                ? formatDistance(item.distanceValue)
                : item.distance.trim();

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(11));
        paint.setFakeBoldText(true);
        float nameWidth = paint.measureText(name);
        paint.setTextSize(dp(10));
        paint.setFakeBoldText(false);
        float metaWidth = paint.measureText(price + " | " + distance);
        float labelWidth = Math.min(getWidth() - dp(20), Math.max(nameWidth, metaWidth) + dp(18));

        rect.set(x - labelWidth / 2f, y - dp(16), x + labelWidth / 2f, y + dp(16));
        if (rect.left < dp(6)) rect.offset(dp(6) - rect.left, 0);
        if (rect.right > getWidth() - dp(6)) rect.offset((getWidth() - dp(6)) - rect.right, 0);

        paint.setColor(Color.WHITE);
        paint.setShadowLayer(dp(8), 0, dp(2), Color.argb(38, 17, 24, 39));
        canvas.drawRoundRect(rect, dp(9), dp(9), paint);
        paint.clearShadowLayer();

        paint.setTextSize(dp(11));
        paint.setFakeBoldText(true);
        paint.setColor(brandColor);
        canvas.drawText(name, rect.centerX(), y - dp(3), paint);

        paint.setTextSize(dp(10));
        paint.setFakeBoldText(false);
        paint.setColor(Color.rgb(66, 71, 78));
        canvas.drawText(price + " | " + distance, rect.centerX(), y + dp(10), paint);
    }

    private void drawEmptyState(Canvas canvas, float width, float height) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(dp(14));
        paint.setColor(Color.rgb(66, 71, 78));
        canvas.drawText("Checking nearby stores", width / 2f, height * 0.78f, paint);
        paint.setFakeBoldText(false);
    }

    private String shortName(String retailerName) {
        if (retailerName == null || retailerName.trim().isEmpty()) return "Store";
        String name = retailerName.trim();
        String lower = name.toLowerCase(Locale.US);
        if (lower.contains("walmart")) return "Walmart";
        if (lower.contains("target")) return "Target";
        if (lower.contains("whole foods")) return "Whole Foods";
        if (lower.contains("trader joe")) return "Trader Joe's";
        if (lower.contains("sprouts")) return "Sprouts";
        if (lower.contains("costco")) return "Costco";
        if (lower.contains("safeway")) return "Safeway";
        if (lower.contains("publix")) return "Publix";
        if (lower.contains("kroger")) return "Kroger";
        if (lower.contains("instacart")) return "Instacart";
        return name.length() > 14 ? name.substring(0, 13).trim() + "..." : name;
    }

    private String formatDistance(double distanceValue) {
        if (distanceValue <= 0.0 || distanceValue >= 95.0) return "Nearby";
        return String.format(Locale.US, "%.1f mi", distanceValue);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
