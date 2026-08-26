package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CacheLookupPolicy {

    private static final String OFFLINE_CACHE_MISS_MESSAGE =
            "You are offline. No saved result is available for this product.";

    static final class Decision {
        final ProductRepository.DataStatus dataStatus;
        final List<ProductRepository.SourceStatus> sourceStatuses;

        Decision(
                ProductRepository.DataStatus dataStatus,
                List<ProductRepository.SourceStatus> sourceStatuses
        ) {
            this.dataStatus = dataStatus;
            this.sourceStatuses = Collections.unmodifiableList(new ArrayList<>(sourceStatuses));
        }
    }

    private CacheLookupPolicy() {
    }

    static Decision forCachedResult(boolean isOnline, boolean isCacheStale) {
        if (!isOnline) {
            return new Decision(
                    ProductRepository.DataStatus.OFFLINE,
                    SourceStatusResolver.forSavedOfflineResult()
            );
        }
        return new Decision(
                isCacheStale
                        ? ProductRepository.DataStatus.STALE
                        : ProductRepository.DataStatus.FRESH,
                SourceStatusResolver.forCachedResult(isCacheStale)
        );
    }

    static String offlineCacheMissMessage() {
        return OFFLINE_CACHE_MISS_MESSAGE;
    }
}
