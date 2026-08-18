package com.ciblorenzo.whatsonmyfood.retail;

import java.util.Locale;

public final class MarketplacePresentation {
    private MarketplacePresentation() {}

    public static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static String retailerName(RetailerAlternative alternative) {
        if (alternative == null) return "Retailer availability varies";
        String provider = safeText(alternative.providerName, "");
        if (!provider.isEmpty() && !isMockProvider(provider)) {
            return readableProviderName(provider);
        }
        String hint = safeText(alternative.retailerHint, "");
        if (hint.toLowerCase(Locale.US).startsWith("available at ")) {
            String stores = hint.substring("available at ".length()).trim();
            int comma = stores.indexOf(',');
            String firstStore = comma >= 0 ? stores.substring(0, comma) : stores;
            if (!firstStore.trim().isEmpty() && !firstStore.toLowerCase(Locale.US).contains("many")) {
                return firstStore.trim();
            }
        }
        return "Multiple retailers";
    }

    public static String sourceLabel(String providerName, boolean defaultMock) {
        if (isMockProvider(providerName) || defaultMock) return "DEVELOPMENT SAMPLE";
        return "LIVE PROVIDER";
    }

    public static boolean isMockProvider(String providerName) {
        return providerName != null && providerName.toLowerCase(Locale.US).contains("mock");
    }

    private static String readableProviderName(String providerName) {
        String name = providerName.replace("Provider", "").replace("Affiliates", "").replace("SpApi", "");
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2").trim();
        return name.isEmpty() ? "Retailer" : name;
    }
}
