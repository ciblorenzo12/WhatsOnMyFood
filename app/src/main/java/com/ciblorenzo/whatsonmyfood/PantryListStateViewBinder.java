package com.ciblorenzo.whatsonmyfood;

import android.view.View;

/** Keeps populated and empty pantry states mutually exclusive. */
public final class PantryListStateViewBinder {

    private PantryListStateViewBinder() {
    }

    public static void bind(View productList, View emptyState, int itemCount) {
        if (productList == null || emptyState == null) return;
        boolean isEmpty = itemCount <= 0;
        productList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
