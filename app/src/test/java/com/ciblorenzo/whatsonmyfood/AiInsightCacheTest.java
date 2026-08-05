package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiInsightCacheTest {

    @Test
    public void acceptedInsightRoundTripsWithItsSources() {
        String stored = AiInsightCache.encode(
                "A complete shopper explanation.",
                "[{\"name\":\"FDA\",\"url\":\"https://www.fda.gov/food\"}]"
        );
        AiInsightCache.Decoded decoded = AiInsightCache.decode(stored);

        assertTrue(stored.startsWith(AiInsightCache.PREFIX));
        assertTrue(decoded.usable);
        assertTrue(decoded.sourcesJson.contains("fda.gov"));
    }

    @Test
    public void emptyOrMalformedInsightIsNotSavedAsUsable() {
        assertTrue(AiInsightCache.encode("", "[]").isEmpty());
        assertFalse(AiInsightCache.decode("BITWISE_AI_CACHE_V11:not-json").usable);
        assertFalse(AiInsightCache.decode("plain legacy text").usable);
    }
}
