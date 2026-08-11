package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.List;

/** Binds source, freshness, and fallback indicators to the shared product-detail panel. */
public final class SourceStatusViewBinder {

    private SourceStatusViewBinder() {
    }

    public static void bind(
            Context context,
            View container,
            TextView indicatorView,
            TextView messageView,
            TextView interpretationView,
            List<ProductRepository.SourceStatus> statuses
    ) {
        if (context == null || container == null || indicatorView == null
                || messageView == null || interpretationView == null) {
            return;
        }

        String message = SourceStatusMessageFormatter.format(context, statuses);
        boolean hasStatus = !message.isEmpty();
        container.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        indicatorView.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        messageView.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        interpretationView.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        if (!hasStatus) return;

        SourceStatusPresentation.Model presentation = SourceStatusPresentation.resolve(statuses);
        indicatorView.setText(formatIndicators(context, presentation.indicators));
        messageView.setText(message);
        interpretationView.setText(interpretationMessage(context, presentation.interpretationNote));
    }

    private static String formatIndicators(
            Context context,
            List<SourceStatusPresentation.Indicator> indicators
    ) {
        StringBuilder text = new StringBuilder();
        for (SourceStatusPresentation.Indicator indicator : indicators) {
            if (text.length() > 0) text.append("  •  ");
            text.append(context.getString(indicatorResource(indicator)));
        }
        return text.toString();
    }

    private static int indicatorResource(SourceStatusPresentation.Indicator indicator) {
        switch (indicator) {
            case PRODUCT_DATABASE:
                return R.string.source_indicator_product_database;
            case RECENT_SAVED_RESULT:
                return R.string.source_indicator_recent_saved_result;
            case SUPPORTING_SOURCE:
                return R.string.source_indicator_supporting_source;
            case RECOVERED_INGREDIENTS:
                return R.string.source_indicator_recovered_ingredients;
            case REFRESH_RECOMMENDED:
                return R.string.source_indicator_refresh_recommended;
            case OFFLINE_COPY:
                return R.string.source_indicator_offline_copy;
            case AI_UNAVAILABLE:
                return R.string.source_indicator_ai_unavailable;
            default:
                throw new IllegalArgumentException("Unknown source indicator: " + indicator);
        }
    }

    private static String interpretationMessage(
            Context context,
            SourceStatusPresentation.InterpretationNote note
    ) {
        switch (note) {
            case OFFLINE_FRESHNESS_UNCONFIRMED:
                return context.getString(R.string.source_interpretation_offline);
            case SAVED_INFORMATION_MAY_BE_OLDER:
                return context.getString(R.string.source_interpretation_stale);
            case RECOVERED_INGREDIENTS_REVIEW:
                return context.getString(R.string.source_interpretation_recovered);
            case RULE_FINDINGS_REMAIN_AVAILABLE:
                return context.getString(R.string.source_interpretation_ai_unavailable);
            case SUPPORTING_SOURCE_DEPENDENT:
                return context.getString(R.string.source_interpretation_supporting_source);
            case SOURCE_RECORD_DEPENDENT:
            default:
                return context.getString(R.string.source_interpretation_database);
        }
    }
}
