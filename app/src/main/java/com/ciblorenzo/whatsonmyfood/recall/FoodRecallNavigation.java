package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** Keeps scan and pantry entry points on the same recall flow. */
public final class FoodRecallNavigation {
    static final String EXTRA_PRODUCT_JSON = "food_recall_product_json";
    static final String EXTRA_ENTRY_POINT = "food_recall_entry_point";

    private static final Gson GSON = new Gson();

    public enum EntryPoint {
        SCAN_RESULT,
        SAVED_PRODUCT
    }

    private FoodRecallNavigation() {}

    public static boolean isAvailable(@Nullable ProductWithDetails productDetails) {
        Product product = productDetails == null ? null : productDetails.product;
        return product != null && product.isValid();
    }

    public static void bindEntry(
            Context context,
            View root,
            @Nullable ProductWithDetails productDetails,
            EntryPoint entryPoint
    ) {
        View layout = root.findViewById(R.id.food_recall_entry_layout);
        Button action = root.findViewById(R.id.food_recall_entry_button);
        TextView unavailable = root.findViewById(R.id.food_recall_entry_unavailable);
        if (layout == null || action == null || unavailable == null) return;

        boolean available = isAvailable(productDetails);
        layout.setVisibility(View.VISIBLE);
        action.setEnabled(available);
        action.setOnClickListener(available
                ? view -> context.startActivity(createIntent(context, productDetails, entryPoint))
                : null);
        unavailable.setVisibility(available ? View.GONE : View.VISIBLE);
        unavailable.setText(available ? "" : context.getString(R.string.food_recall_unavailable_product));

        if (available) {
            String name = productDetails.product.productName;
            action.setContentDescription(context.getString(R.string.food_recall_action_for_product, name));
        } else {
            action.setContentDescription(context.getString(R.string.food_recall_unavailable_product));
        }
        GlassMotion.attachPress(action);
    }

    public static Intent createIntent(
            Context context,
            ProductWithDetails productDetails,
            EntryPoint entryPoint
    ) {
        if (!isAvailable(productDetails)) {
            throw new IllegalArgumentException("A valid product is required for a recall check.");
        }
        EntryPoint safeEntryPoint = entryPoint == null ? EntryPoint.SCAN_RESULT : entryPoint;
        return new Intent(context, FoodRecallActivity.class)
                .putExtra(EXTRA_PRODUCT_JSON, GSON.toJson(productDetails))
                .putExtra(EXTRA_ENTRY_POINT, safeEntryPoint.name());
    }

    @Nullable
    public static ProductWithDetails readProduct(Intent intent) {
        if (intent == null) return null;
        String json = intent.getStringExtra(EXTRA_PRODUCT_JSON);
        if (json == null || json.trim().isEmpty()) return null;
        try {
            ProductWithDetails productDetails = GSON.fromJson(json, ProductWithDetails.class);
            return isAvailable(productDetails) ? productDetails : null;
        } catch (JsonParseException | IllegalStateException error) {
            return null;
        }
    }

    public static EntryPoint readEntryPoint(Intent intent) {
        if (intent == null) return EntryPoint.SCAN_RESULT;
        String value = intent.getStringExtra(EXTRA_ENTRY_POINT);
        try {
            return value == null ? EntryPoint.SCAN_RESULT : EntryPoint.valueOf(value);
        } catch (IllegalArgumentException error) {
            return EntryPoint.SCAN_RESULT;
        }
    }
}
