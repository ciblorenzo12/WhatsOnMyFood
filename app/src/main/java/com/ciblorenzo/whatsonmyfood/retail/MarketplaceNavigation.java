package com.ciblorenzo.whatsonmyfood.retail;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** Keeps the product-detail to marketplace handoff in one testable place. */
public final class MarketplaceNavigation {

    private static final Gson GSON = new Gson();

    private MarketplaceNavigation() {}

    public static boolean isAvailable(@Nullable ProductWithDetails productDetails) {
        Product product = productDetails == null ? null : productDetails.product;
        return product != null && product.isValid();
    }

    public static void bindAction(
            Context context,
            Button button,
            @Nullable TextView unavailableMessage,
            @Nullable ProductWithDetails productDetails
    ) {
        boolean available = isAvailable(productDetails);
        button.setEnabled(available);
        button.setOnClickListener(available
                ? view -> context.startActivity(createIntent(context, productDetails))
                : null);

        if (available) {
            button.setContentDescription(context.getString(
                    R.string.compare_alternatives_for_product,
                    productDetails.product.productName
            ));
        } else {
            button.setContentDescription(context.getString(R.string.comparison_unavailable));
        }

        if (unavailableMessage != null) {
            unavailableMessage.setText(available ? "" : context.getString(R.string.comparison_unavailable));
            unavailableMessage.setVisibility(available ? View.GONE : View.VISIBLE);
        }
    }

    public static Intent createIntent(Context context, ProductWithDetails productDetails) {
        if (!isAvailable(productDetails)) {
            throw new IllegalArgumentException("A valid product is required to compare alternatives.");
        }
        return new Intent(context, MarketplaceActivity.class)
                .putExtra(MarketplaceActivity.EXTRA_PRODUCT_JSON, GSON.toJson(productDetails));
    }

    @Nullable
    public static ProductWithDetails readProduct(Intent intent) {
        if (intent == null) return null;
        String productJson = intent.getStringExtra(MarketplaceActivity.EXTRA_PRODUCT_JSON);
        if (productJson == null || productJson.trim().isEmpty()) return null;
        try {
            ProductWithDetails productDetails = GSON.fromJson(productJson, ProductWithDetails.class);
            return isAvailable(productDetails) ? productDetails : null;
        } catch (JsonParseException | IllegalStateException error) {
            return null;
        }
    }
}
