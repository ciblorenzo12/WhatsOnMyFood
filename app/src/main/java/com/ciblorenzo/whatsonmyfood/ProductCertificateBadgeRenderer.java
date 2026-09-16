package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public final class ProductCertificateBadgeRenderer {
    private ProductCertificateBadgeRenderer() {}

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
        for (ProductCertificate certificate : certificates) {
            badgesContainer.addView(createBadgeView(context, certificate));
        }
    }

    private static View createBadgeView(Context context, ProductCertificate certificate) {
        int logoResId = officialLogoFor(certificate.logoKey);
        View view;
        LinearLayout.LayoutParams params;
        if (logoResId != 0) {
            ImageView badge = new ImageView(context);
            badge.setImageResource(logoResId);
            badge.setAdjustViewBounds(true);
            badge.setScaleType(ImageView.ScaleType.FIT_CENTER);
            badge.setMaxWidth(dp(context, 156));
            // The OU source is white artwork; preserve it on a dark backing.
            // Other marks use white backing for contrast in either app theme.
            badge.setBackgroundColor("ou_kosher".equals(certificate.logoKey)
                    ? Color.BLACK : Color.WHITE);
            badge.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(context, 88));
            view = badge;
        } else {
            // Unidentified or unsupported certifications stay descriptive; never invent a seal.
            TextView label = new TextView(context);
            label.setText(certificate.sourceLabel);
            label.setTextColor(context.getColor(R.color.text_primary));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setMaxWidth(dp(context, 180));
            label.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
            int dietaryIcon = dietaryIconFor(certificate);
            if (dietaryIcon != 0) {
                label.setText("gluten_free".equals(certificate.key) ? R.string.dietary_gluten_free
                        : "vegan".equals(certificate.key) ? R.string.dietary_vegan
                        : R.string.dietary_vegetarian);
                label.setGravity(Gravity.CENTER);
                label.setTextColor(context.getColor(R.color.dietary_badge_foreground));
                label.setMinWidth(dp(context, 100));
                label.setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4));
                android.graphics.drawable.Drawable icon = context.getDrawable(dietaryIcon);
                icon.setBounds(0, 0, dp(context, 32), dp(context, 32));
                label.setCompoundDrawables(null, icon, null, null);
                label.setCompoundDrawablePadding(dp(context, 4));
            }
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            view = label;
            if (dietaryIcon != 0) {
                LinearLayout badge = new LinearLayout(context);
                badge.setOrientation(LinearLayout.VERTICAL);
                badge.setGravity(Gravity.CENTER);
                badge.setBackgroundResource(R.drawable.dietary_badge_background);
                badge.addView(label);
                TextView status = new TextView(context);
                status.setText(R.string.dietary_not_verified);
                status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                status.setGravity(Gravity.CENTER);
                status.setTextColor(context.getColor(R.color.dietary_unverified_foreground));
                status.setBackgroundResource(R.drawable.dietary_unverified_banner);
                status.setPadding(dp(context, 8), dp(context, 3), dp(context, 8), dp(context, 3));
                badge.addView(status, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                view = badge;
            }
        }
        params.gravity = Gravity.CENTER_VERTICAL;
        params.setMarginEnd(dp(context, 12));
        view.setLayoutParams(params);
        view.setContentDescription(dietaryIconFor(certificate) != 0 && logoResId == 0
                ? certificate.sourceLabel + ", " + context.getString(R.string.dietary_not_verified)
                : certificate.sourceLabel);
        return view;
    }

    private static int dietaryIconFor(ProductCertificate certificate) {
        if (certificate.specific) return 0;
        switch (certificate.key) {
            case "gluten_free": return R.drawable.ic_dietary_gluten_free;
            case "vegan": return R.drawable.ic_dietary_vegan;
            case "vegetarian": return R.drawable.ic_dietary_vegetarian;
            default: return 0;
        }
    }

    private static int officialLogoFor(String logoKey) {
        if (logoKey == null) return 0;
        switch (logoKey) {
            case "gfco": return R.drawable.cert_gfco;
            case "usda_organic": return R.drawable.cert_usda_organic;
            case "non_gmo_project_verified": return R.drawable.cert_non_gmo_project_verified;
            case "ou_kosher": return R.drawable.cert_ou_kosher;
            case "vegan_action": return R.drawable.cert_vegan_action;
            case "fair_trade": return R.drawable.cert_fair_trade;
            case "rainforest": return R.drawable.cert_rainforest;
            case "b_corp": return R.drawable.cert_b_corp;
            case "regenerative": return R.drawable.cert_regenerative;
            case "animal_welfare": return R.drawable.cert_animal_welfare;
            case "green_dot": return R.drawable.cert_green_dot;
            case "triman": return R.drawable.cert_triman;
            default: return 0;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }
}
