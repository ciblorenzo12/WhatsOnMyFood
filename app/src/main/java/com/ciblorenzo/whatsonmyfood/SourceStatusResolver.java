package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Maps product lookup outcomes to the status messages shown to the shopper. */
public final class SourceStatusResolver {

    private SourceStatusResolver() {
    }

    /** Preserve the same provenance in both product-detail AI requests. */
    public static String forAiContext(Iterable<ProductRepository.SourceStatus> statuses) {
        StringBuilder value = new StringBuilder();
        if (statuses != null) {
            for (ProductRepository.SourceStatus status : statuses) {
                if (status == null) continue;
                if (value.length() > 0) value.append(", ");
                value.append(status.name().toLowerCase(Locale.US));
            }
        }
        return value.length() == 0 ? "unknown" : value.toString();
    }

    public static List<ProductRepository.SourceStatus> forCachedResult(boolean isStale) {
        List<ProductRepository.SourceStatus> statuses = new ArrayList<>();
        statuses.add(isStale
                ? ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
                : ProductRepository.SourceStatus.FRESH_CACHED_RESULT);
        return statuses;
    }

    public static List<ProductRepository.SourceStatus> forSavedOfflineResult() {
        List<ProductRepository.SourceStatus> statuses = new ArrayList<>();
        statuses.add(ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT);
        statuses.add(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED);
        return statuses;
    }

    public static List<ProductRepository.SourceStatus> forUpdatedDatabaseResult(
            boolean usedFallbackSource,
            boolean ingredientsRecovered
    ) {
        List<ProductRepository.SourceStatus> statuses = new ArrayList<>();
        statuses.add(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE);
        if (usedFallbackSource) {
            statuses.add(ProductRepository.SourceStatus.FALLBACK_PRODUCT_SOURCE);
        }
        if (ingredientsRecovered) {
            statuses.add(ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE);
        }
        return statuses;
    }

    public static List<ProductRepository.SourceStatus> forUnavailableRefreshResult() {
        List<ProductRepository.SourceStatus> statuses = new ArrayList<>();
        statuses.add(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED);
        return statuses;
    }
}
