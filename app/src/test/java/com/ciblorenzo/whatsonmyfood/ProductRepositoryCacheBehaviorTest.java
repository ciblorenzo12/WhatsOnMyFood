package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductRepositoryCacheBehaviorTest {

    @Test
    public void onlineFreshCache_isReturnedAsFreshSavedData() {
        CacheLookupPolicy.Decision result = CacheLookupPolicy.forCachedResult(true, false);

        assertEquals(ProductRepository.DataStatus.FRESH, result.dataStatus);
        assertEquals(
                Collections.singletonList(ProductRepository.SourceStatus.FRESH_CACHED_RESULT),
                result.sourceStatuses
        );
    }

    @Test
    public void onlineStaleCache_isReturnedWithAnOutdatedWarning() {
        CacheLookupPolicy.Decision result = CacheLookupPolicy.forCachedResult(true, true);

        assertEquals(ProductRepository.DataStatus.STALE, result.dataStatus);
        assertEquals(
                Collections.singletonList(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED),
                result.sourceStatuses
        );
    }

    @Test
    public void offlineCache_isNeverPresentedAsCurrent() {
        CacheLookupPolicy.Decision freshCache = CacheLookupPolicy.forCachedResult(false, false);
        CacheLookupPolicy.Decision staleCache = CacheLookupPolicy.forCachedResult(false, true);

        for (CacheLookupPolicy.Decision result : Arrays.asList(freshCache, staleCache)) {
            assertEquals(ProductRepository.DataStatus.OFFLINE, result.dataStatus);
            assertEquals(
                    Arrays.asList(
                            ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT,
                            ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
                    ),
                    result.sourceStatuses
            );
        }
    }

    @Test
    public void offlineWithoutCache_hasAnUnderstandableUnavailableMessage() {
        assertEquals(
                "You are offline. No saved result is available for this product.",
                CacheLookupPolicy.offlineCacheMissMessage()
        );
        assertTrue(CacheLookupPolicy.offlineCacheMissMessage().contains("No saved result"));
    }
}
