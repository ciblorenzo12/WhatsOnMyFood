package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.Intent;

/** Builds the single navigation contract used by every pantry product row. */
public final class PantryNavigation {

    private PantryNavigation() {
    }

    public static Intent productDetailsIntent(Context context, Product product) {
        if (context == null || product == null || product.barcode == null
                || product.barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("A pantry product barcode is required");
        }
        return new Intent(context, ProductDetailsActivity.class)
                .putExtra(ProductDetailsActivity.EXTRA_BARCODE, product.barcode);
    }
}
