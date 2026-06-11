package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public final class ProductCertificateBadgeRenderer {

    private ProductCertificateBadgeRenderer() {
    }

    public static void bind(Context context, View labelsLabel, TextView labelsTextView,
                            HorizontalScrollView badgesScrollView, LinearLayout badgesContainer,
                            String labels) {
        String displayLabels = ProductCertificateParser.formatLabelsForDisplay(labels);
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(labels);
        boolean hasLabels = displayLabels != null && !displayLabels.trim().isEmpty();
        boolean hasCertificates = !certificates.isEmpty();

        if (labelsLabel != null) {
            labelsLabel.setVisibility(hasLabels || hasCertificates ? View.VISIBLE : View.GONE);
        }

        if (labelsTextView != null) {
            labelsTextView.setText(hasLabels ? displayLabels : "");
            labelsTextView.setVisibility(hasLabels ? View.VISIBLE : View.GONE);
        }

        if (badgesContainer == null || badgesScrollView == null) return;
        badgesContainer.removeAllViews();
        badgesScrollView.setVisibility(hasCertificates ? View.VISIBLE : View.GONE);
        if (!hasCertificates) return;

        for (ProductCertificate certificate : certificates) {
            badgesContainer.addView(createBadgeView(context, certificate));
        }
    }

    private static View createBadgeView(Context context, ProductCertificate certificate) {
        BadgeStyle style = styleFor(certificate.styleKey);
        int logoResId = officialLogoFor(certificate);
        if (logoResId != 0) {
            ImageView badge = new ImageView(context);
            badge.setImageResource(logoResId);
            badge.setAdjustViewBounds(true);
            badge.setScaleType(ImageView.ScaleType.FIT_CENTER);
            badge.setContentDescription(certificate.displayName);
            badge.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
            badge.setBackgroundColor(Color.TRANSPARENT);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(context, style.widthDp),
                    dp(context, style.heightDp)
            );
            params.setMarginEnd(dp(context, 10));
            badge.setLayoutParams(params);
            return badge;
        }

        CertificateBadgeView badge = new CertificateBadgeView(context, certificate, style);
        badge.setContentDescription(certificate.displayName);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dp(context, style.widthDp),
                dp(context, style.heightDp)
        );
        params.setMarginEnd(dp(context, 10));
        badge.setLayoutParams(params);
        return badge;
    }

    private static int officialLogoFor(ProductCertificate certificate) {
        if (certificate == null || !certificate.specific) return 0;
        String key = certificate.key == null ? "" : certificate.key;
        String displayName = certificate.displayName == null
                ? ""
                : certificate.displayName.toLowerCase(Locale.US);

        if ("organic".equals(key) && displayName.contains("usda")) {
            return R.drawable.cert_usda_organic;
        }
        if ("non_gmo".equals(key) && displayName.contains("project")) {
            return R.drawable.cert_non_gmo_project_verified;
        }
        if ("gluten_free".equals(key)) {
            return R.drawable.cert_gluten_free;
        }
        if ("fair_trade".equals(key)) {
            return R.drawable.cert_fair_trade;
        }
        if ("rainforest".equals(key)) {
            return R.drawable.cert_rainforest;
        }
        if ("vegan".equals(key)) {
            return R.drawable.cert_vegan;
        }
        if ("halal".equals(key)) {
            return R.drawable.cert_halal;
        }
        if ("b_corp".equals(key)) {
            return R.drawable.cert_b_corp;
        }
        if ("regenerative".equals(key) && displayName.contains("organic")) {
            return R.drawable.cert_regenerative;
        }
        if ("animal_welfare".equals(key) && displayName.contains("humane")) {
            return R.drawable.cert_animal_welfare;
        }
        if ("whole_grain".equals(key)) {
            return R.drawable.cert_whole_grain;
        }
        if ("green_dot".equals(key)) {
            return R.drawable.cert_green_dot;
        }
        if ("triman".equals(key)) {
            return R.drawable.cert_triman;
        }
        return 0;
    }

    private static BadgeStyle styleFor(String styleKey) {
        if ("organic".equals(styleKey)) {
            return new BadgeStyle("#087A3A", "#7A3F0A", "#FFFFFF", true, 92, 92, 11, 2);
        }
        if ("non_gmo".equals(styleKey)) {
            return new BadgeStyle("#FFFFFF", "#5BA047", "#1F4E9A", false, 156, 86, 11, 1);
        }
        if ("kosher".equals(styleKey)) {
            return new BadgeStyle("#FFFFFF", "#111111", "#111111", true, 92, 92, 11, 2);
        }
        if ("halal".equals(styleKey)) {
            return new BadgeStyle("#FFFFFF", "#0B7A43", "#0B7A43", true, 82, 82, 13, 2);
        }
        if ("gluten_free".equals(styleKey)) {
            return new BadgeStyle("#FFF9ED", "#B45309", "#92400E", false, 126, 76, 10, 2);
        }
        if ("fair_trade".equals(styleKey)) {
            return new BadgeStyle("#F0FDFA", "#0F766E", "#0F766E", false, 126, 76, 10, 2);
        }
        if ("rainforest".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#15803D", "#166534", false, 130, 76, 10, 2);
        }
        if ("vegan".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#15803D", "#166534", false, 106, 76, 11, 2);
        }
        if ("plant_based".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#16A34A", "#166534", false, 118, 76, 10, 2);
        }
        if ("vegetarian".equals(styleKey)) {
            return new BadgeStyle("#F7FEE7", "#65A30D", "#3F6212", false, 126, 76, 10, 2);
        }
        if ("b_corp".equals(styleKey)) {
            return new BadgeStyle("#EFF6FF", "#1D4ED8", "#1E3A8A", false, 116, 76, 11, 2);
        }
        if ("regenerative".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#15803D", "#14532D", false, 132, 76, 10, 2);
        }
        if ("animal_welfare".equals(styleKey)) {
            return new BadgeStyle("#FFF7ED", "#EA580C", "#9A3412", false, 126, 76, 10, 2);
        }
        if ("marine".equals(styleKey)) {
            return new BadgeStyle("#EFF6FF", "#0284C7", "#075985", false, 116, 76, 11, 2);
        }
        if ("whole_grain".equals(styleKey)) {
            return new BadgeStyle("#FFFBEB", "#B45309", "#78350F", false, 124, 76, 10, 2);
        }
        if ("keto".equals(styleKey)) {
            return new BadgeStyle("#F5F3FF", "#7C3AED", "#4C1D95", false, 112, 76, 11, 2);
        }
        if ("paleo".equals(styleKey)) {
            return new BadgeStyle("#FFF7ED", "#C2410C", "#7C2D12", false, 116, 76, 11, 2);
        }
        if ("heart_check".equals(styleKey)) {
            return new BadgeStyle("#FEF2F2", "#DC2626", "#991B1B", false, 112, 76, 11, 2);
        }
        if ("sport".equals(styleKey)) {
            return new BadgeStyle("#F8FAFC", "#334155", "#0F172A", false, 118, 76, 10, 2);
        }
        if ("green_dot".equals(styleKey)) {
            return new BadgeStyle("#E7F6B7", "#166534", "#166534", true, 92, 92, 11, 2);
        }
        if ("triman".equals(styleKey)) {
            return new BadgeStyle("#FFFFFF", "#111111", "#111111", true, 92, 92, 11, 2);
        }
        if ("recycling".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#15803D", "#14532D", false, 124, 76, 10, 2);
        }
        if ("forest".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#166534", "#14532D", false, 112, 76, 12, 2);
        }
        if ("compostable".equals(styleKey)) {
            return new BadgeStyle("#ECFDF5", "#047857", "#064E3B", false, 126, 76, 10, 2);
        }
        if ("plastic_free".equals(styleKey)) {
            return new BadgeStyle("#EFF6FF", "#2563EB", "#1E3A8A", false, 118, 76, 10, 2);
        }
        if ("carbon".equals(styleKey)) {
            return new BadgeStyle("#F8FAFC", "#475569", "#0F172A", false, 124, 76, 10, 2);
        }
        if ("ecolabel".equals(styleKey)) {
            return new BadgeStyle("#F0FDF4", "#16A34A", "#14532D", false, 116, 76, 11, 2);
        }
        return new BadgeStyle("#F8FAFC", "#64748B", "#334155", false, 134, 76, 10, 1);
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    private static class BadgeStyle {
        final int backgroundColor;
        final int strokeColor;
        final int textColor;
        final boolean oval;
        final int widthDp;
        final int heightDp;
        final int textSizeSp;
        final int strokeWidthDp;

        BadgeStyle(String backgroundColor, String strokeColor, String textColor, boolean oval,
                   int widthDp, int heightDp, int textSizeSp, int strokeWidthDp) {
            this.backgroundColor = Color.parseColor(backgroundColor);
            this.strokeColor = Color.parseColor(strokeColor);
            this.textColor = Color.parseColor(textColor);
            this.oval = oval;
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.textSizeSp = textSizeSp;
            this.strokeWidthDp = strokeWidthDp;
        }
    }

    private static class CertificateBadgeView extends View {
        private final ProductCertificate certificate;
        private final BadgeStyle style;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        CertificateBadgeView(Context context, ProductCertificate certificate, BadgeStyle style) {
            super(context);
            this.certificate = certificate;
            this.style = style;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = dp(getContext(), style.strokeWidthDp);
            rect.set(stroke, stroke, getWidth() - stroke, getHeight() - stroke);

            if ("organic".equals(certificate.styleKey)) {
                drawOrganic(canvas, stroke);
            } else if ("kosher".equals(certificate.styleKey)) {
                drawKosher(canvas, stroke);
            } else if ("non_gmo".equals(certificate.styleKey)) {
                drawNonGmo(canvas, stroke);
            } else {
                drawGeneric(canvas, stroke);
            }
        }

        private void drawOrganic(Canvas canvas, float stroke) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - stroke;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#7A3F0A"));
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius - dp(getContext(), 5), paint);

            float innerRadius = radius - dp(getContext(), 9);
            Path sealClip = new Path();
            sealClip.addCircle(cx, cy, innerRadius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(sealClip);

            paint.setColor(Color.WHITE);
            canvas.drawRect(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + dp(getContext(), 6), paint);
            paint.setColor(Color.parseColor("#087A3A"));
            canvas.drawRect(cx - innerRadius, cy + dp(getContext(), 6), cx + innerRadius, cy + innerRadius, paint);

            paint.setColor(Color.parseColor("#0A7A3D"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 2));
            canvas.drawArc(new RectF(cx - innerRadius, cy - innerRadius + dp(getContext(), 2),
                    cx + innerRadius, cy + innerRadius), 192, 156, false, paint);

            canvas.restore();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#087A3A"));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(sp(17));
            canvas.drawText("USDA", cx, cy - dp(getContext(), 8), paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(sp(15));
            canvas.drawText("ORGANIC", cx, cy + dp(getContext(), 21), paint);
            paint.setFakeBoldText(false);
        }

        private void drawKosher(Canvas canvas, float stroke) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - stroke - dp(getContext(), 2);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(sp(8));
            Path topArc = new Path();
            topArc.addArc(new RectF(cx - radius + dp(getContext(), 8), cy - radius + dp(getContext(), 8),
                    cx + radius - dp(getContext(), 8), cy + radius - dp(getContext(), 8)), 208, 124);
            canvas.drawTextOnPath("ORTHODOX UNION", topArc, 0, 0, paint);

            Path bottomArc = new Path();
            bottomArc.addArc(new RectF(cx - radius + dp(getContext(), 8), cy - radius + dp(getContext(), 8),
                    cx + radius - dp(getContext(), 8), cy + radius - dp(getContext(), 8)), 28, 124);
            canvas.drawTextOnPath("KOSHER CERTIFIED", bottomArc, 0, 0, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 3));
            canvas.drawCircle(cx, cy, dp(getContext(), 20), paint);
            canvas.drawCircle(cx, cy, dp(getContext(), 11), paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(sp(31));
            canvas.drawText("U", cx, cy + dp(getContext(), 12), paint);
            paint.setFakeBoldText(false);
        }

        private void drawNonGmo(Canvas canvas, float stroke) {
            float w = getWidth();
            float h = getHeight();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, w, h, paint);

            float butterflyRight = dp(getContext(), 49);
            float blueLeft = dp(getContext(), 55);
            float blueRight = w;
            float blueBottom = dp(getContext(), 52);
            float verifiedTop = dp(getContext(), 54);
            float verifiedBottom = dp(getContext(), 71);

            paint.setColor(Color.parseColor("#1B62AA"));
            canvas.drawRect(blueLeft, 0, blueRight, blueBottom, paint);
            paint.setColor(Color.parseColor("#559A3C"));
            Path check = new Path();
            check.moveTo(dp(getContext(), 40), dp(getContext(), 28));
            check.lineTo(dp(getContext(), 58), dp(getContext(), 47));
            check.lineTo(dp(getContext(), 86), dp(getContext(), 1));
            check.lineTo(dp(getContext(), 93), dp(getContext(), 1));
            check.lineTo(dp(getContext(), 59), dp(getContext(), 58));
            check.lineTo(dp(getContext(), 34), dp(getContext(), 35));
            check.close();
            canvas.drawPath(check, paint);

            drawButterfly(canvas, dp(getContext(), 25), dp(getContext(), 24));

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor("#5BA047"));
            canvas.drawRect(blueLeft, verifiedTop, blueRight - stroke, verifiedBottom, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setColor(Color.WHITE);
            paint.setTextSize(sp(18));
            canvas.drawText("NON", (blueLeft + blueRight) / 2f, dp(getContext(), 18), paint);
            canvas.drawText("GMO", (blueLeft + blueRight) / 2f, dp(getContext(), 36), paint);
            paint.setTextSize(sp(11));
            paint.setFakeBoldText(false);
            canvas.drawText("Project", (blueLeft + blueRight) / 2f, dp(getContext(), 49), paint);

            paint.setColor(Color.parseColor("#5BA047"));
            paint.setFakeBoldText(true);
            paint.setTextSize(sp(15));
            canvas.drawText("VERIFIED", (blueLeft + blueRight) / 2f, dp(getContext(), 68), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(sp(11));
            canvas.drawText("nongmoproject.org", (blueLeft + blueRight) / 2f, dp(getContext(), 83), paint);
        }

        private void drawButterfly(Canvas canvas, float cx, float cy) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawOval(new RectF(cx - dp(getContext(), 2), cy - dp(getContext(), 13),
                    cx + dp(getContext(), 2), cy + dp(getContext(), 15)), paint);

            paint.setColor(Color.parseColor("#F59E0B"));
            Path leftWing = new Path();
            leftWing.moveTo(cx - dp(getContext(), 3), cy - dp(getContext(), 2));
            leftWing.cubicTo(cx - dp(getContext(), 26), cy - dp(getContext(), 26),
                    cx - dp(getContext(), 24), cy + dp(getContext(), 1),
                    cx - dp(getContext(), 8), cy + dp(getContext(), 11));
            leftWing.cubicTo(cx - dp(getContext(), 8), cy + dp(getContext(), 3),
                    cx - dp(getContext(), 7), cy, cx - dp(getContext(), 3), cy - dp(getContext(), 2));
            canvas.drawPath(leftWing, paint);

            paint.setColor(Color.parseColor("#F97316"));
            Path rightWing = new Path();
            rightWing.moveTo(cx + dp(getContext(), 4), cy - dp(getContext(), 2));
            rightWing.cubicTo(cx + dp(getContext(), 24), cy - dp(getContext(), 23),
                    cx + dp(getContext(), 25), cy + dp(getContext(), 3),
                    cx + dp(getContext(), 8), cy + dp(getContext(), 13));
            rightWing.cubicTo(cx + dp(getContext(), 7), cy + dp(getContext(), 4),
                    cx + dp(getContext(), 7), cy, cx + dp(getContext(), 4), cy - dp(getContext(), 2));
            canvas.drawPath(rightWing, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 1));
            paint.setColor(Color.BLACK);
            canvas.drawPath(leftWing, paint);
            canvas.drawPath(rightWing, paint);
        }

        private void drawGeneric(Canvas canvas, float stroke) {
            float radius = style.oval ? Math.min(getWidth(), getHeight()) / 2f - stroke : dp(getContext(), 10);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(style.backgroundColor);
            if (style.oval) {
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
            } else {
                canvas.drawRoundRect(rect, radius, radius, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(style.strokeColor);
            if (style.oval) {
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
            } else {
                canvas.drawRoundRect(rect, radius, radius, paint);
            }

            String[] lines = certificate.badgeText.split("\\n");
            float[] sizes = new float[lines.length];
            for (int i = 0; i < sizes.length; i++) {
                sizes[i] = style.textSizeSp;
            }
            drawCenteredLines(canvas, lines, style.textColor, sizes, getHeight() / 2f - sp((lines.length - 1) * 5), true);
        }

        private void drawCenteredLines(Canvas canvas, String[] lines, int color, float[] sizesSp, float firstBaseline, boolean bold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(bold);
            float y = firstBaseline;
            for (int i = 0; i < lines.length; i++) {
                paint.setTextSize(sp(sizesSp[Math.min(i, sizesSp.length - 1)]));
                Paint.FontMetrics metrics = paint.getFontMetrics();
                float baseline = y - (metrics.ascent + metrics.descent) / 2f;
                canvas.drawText(lines[i], getWidth() / 2f, baseline, paint);
                y += sp(15);
            }
            paint.setFakeBoldText(false);
        }

        private float sp(float value) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    value,
                    getResources().getDisplayMetrics()
            );
        }
    }
}
