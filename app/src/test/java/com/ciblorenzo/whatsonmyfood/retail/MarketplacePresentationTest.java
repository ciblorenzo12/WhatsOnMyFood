package com.ciblorenzo.whatsonmyfood.retail;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MarketplacePresentationTest {

    @Test
    public void incompleteAlternativeRecordUsesSafeFallbacks() {
        RetailerAlternative alternative = new RetailerAlternative(
                "Simple snack", null, null, null, null, null, null,
                80, 0, 0, "MockRetailerProvider");

        assertEquals("Multiple retailers", MarketplacePresentation.retailerName(alternative));
        assertEquals("Brand not provided", MarketplacePresentation.safeText(alternative.brand, "Brand not provided"));
        assertEquals("DEVELOPMENT SAMPLE", MarketplacePresentation.sourceLabel(alternative.providerName, false));
    }

    @Test
    public void retailerHintAndLiveProviderArePresentedClearly() {
        RetailerAlternative hinted = new RetailerAlternative(
                "Simple snack", "Brand", "Reason", "Signal",
                "Available at Target, Walmart, and Kroger", "https://example.com", "",
                85, 3.49, 1.2, "MockRetailerProvider");
        RetailerAlternative live = new RetailerAlternative(
                "Simple snack", "Brand", "Reason", "Signal", "", "", "",
                85, 3.49, 1.2, "WalmartAffiliatesProvider");

        assertEquals("Target", MarketplacePresentation.retailerName(hinted));
        assertEquals("Walmart", MarketplacePresentation.retailerName(live));
        assertEquals("LIVE PROVIDER", MarketplacePresentation.sourceLabel(live.providerName, false));
    }
}
