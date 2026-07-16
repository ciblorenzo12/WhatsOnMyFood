package com.ciblorenzo.whatsonmyfood;

import android.content.Context;

import java.util.LinkedHashSet;
import java.util.List;

/** Formats source statuses in a stable order and removes duplicates before display. */
public final class SourceStatusMessageFormatter {

    private SourceStatusMessageFormatter() {
    }

    public static String format(Context context, List<ProductRepository.SourceStatus> statuses) {
        if (context == null || statuses == null || statuses.isEmpty()) {
            return "";
        }

        LinkedHashSet<ProductRepository.SourceStatus> uniqueStatuses = new LinkedHashSet<>(statuses);
        StringBuilder message = new StringBuilder();
        for (ProductRepository.SourceStatus status : uniqueStatuses) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(context.getString(messageResource(status)));
        }
        return message.toString();
    }

    private static int messageResource(ProductRepository.SourceStatus status) {
        switch (status) {
            case FRESH_CACHED_RESULT:
                return R.string.source_status_fresh_cached_result;
            case UPDATED_FROM_PRODUCT_DATABASE:
                return R.string.source_status_updated_from_product_database;
            case FALLBACK_PRODUCT_SOURCE:
                return R.string.source_status_fallback_product_source;
            case SAVED_OFFLINE_RESULT:
                return R.string.source_status_saved_offline_result;
            case INFORMATION_MAY_BE_OUTDATED:
                return R.string.source_status_information_may_be_outdated;
            case INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE:
                return R.string.source_status_ingredients_recovered;
            default:
                throw new IllegalArgumentException("Unknown product source status: " + status);
        }
    }
}
