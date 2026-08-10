package com.ciblorenzo.whatsonmyfood.analysis;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.R;

/** Applies a findings display state to the shared product-detail views. */
public final class ProductFindingsViewBinder {

    private ProductFindingsViewBinder() {
    }

    public static void bind(
            @NonNull RecyclerView findingsList,
            @NonNull TextView emptyState,
            @NonNull ProductFindingsDisplay display
    ) {
        if (display.hasFindings()) {
            findingsList.setAdapter(new AnalysisResultAdapter(display.getFindings()));
            findingsList.setVisibility(View.VISIBLE);
            emptyState.setText("");
            emptyState.setVisibility(View.GONE);
            return;
        }

        findingsList.setAdapter(null);
        findingsList.setVisibility(View.GONE);
        emptyState.setText(messageFor(display.getState()));
        emptyState.setVisibility(View.VISIBLE);
    }

    private static int messageFor(ProductFindingsDisplay.State state) {
        switch (state) {
            case INGREDIENTS_REQUIRED:
                return R.string.findings_ingredients_required;
            case ANALYSIS_UNAVAILABLE:
                return R.string.findings_analysis_unavailable;
            case NO_FINDINGS:
            default:
                return R.string.findings_none_identified;
        }
    }
}
