package com.example.myapplication.retail;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles legacy mock retailers (Walmart, Target, Amazon) during transition.
 */
public class LegacyMockRetailerClient implements RetailerBackendClient {

    private static final String PROVIDER = "LegacyMockProvider";

    @Override
    public List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) {
        String search = buildSearchTerm(query);
        List<RetailerAvailability> results = new ArrayList<>();
        
        results.add(new RetailerAvailability(
                "Walmart Supercenter",
                PROVIDER,
                "Search retailer",
                "Varies",
                "2.4 mi",
                "Pickup or delivery",
                "https://www.walmart.com/search?q=" + Uri.encode(search),
                "Searches this product at Walmart; inventory must be confirmed by Walmart.",
                true,
                0.0,
                2.4
        ));
        results.add(new RetailerAvailability(
                "Target Grocery",
                PROVIDER,
                "Search retailer",
                "Varies",
                "3.1 mi",
                "Pickup or delivery",
                "https://www.target.com/s?searchTerm=" + Uri.encode(search),
                "Searches this product at Target; inventory must be confirmed by Target.",
                true,
                0.0,
                3.1
        ));
        results.add(new RetailerAvailability(
                "Whole Foods Market",
                PROVIDER,
                "Search retailer",
                "Varies",
                "3.8 mi",
                "In-store or delivery",
                "https://www.wholefoodsmarket.com/search?text=" + Uri.encode(search),
                "Searches this product; inventory must be confirmed by the retailer.",
                true,
                0.0,
                3.8
        ));
        results.add(new RetailerAvailability(
                "Trader Joe's",
                PROVIDER,
                "Search retailer",
                "Varies",
                "4.4 mi",
                "In-store",
                "https://www.traderjoes.com/home/search?q=" + Uri.encode(search),
                "Useful for store-brand and specialty grocery matches.",
                true,
                0.0,
                4.4
        ));
        results.add(new RetailerAvailability(
                "Sprouts Farmers Market",
                PROVIDER,
                "Search retailer",
                "Varies",
                "5.2 mi",
                "Pickup or delivery",
                "https://shop.sprouts.com/search?search_term=" + Uri.encode(search),
                "Searches this product; inventory must be confirmed by Sprouts.",
                true,
                0.0,
                5.2
        ));
        results.add(new RetailerAvailability(
                "Costco",
                PROVIDER,
                "Search retailer",
                "Varies",
                "6.7 mi",
                "Warehouse or delivery",
                "https://www.costco.com/CatalogSearch?keyword=" + Uri.encode(search),
                "Best for club-size grocery items and multi-packs.",
                true,
                0.0,
                6.7
        ));
        results.add(new RetailerAvailability(
                "Safeway",
                PROVIDER,
                "Search retailer",
                "Varies",
                "7.4 mi",
                "Pickup or delivery",
                "https://www.safeway.com/shop/search-results.html?q=" + Uri.encode(search),
                "Regional grocery result for nearby availability checks.",
                true,
                0.0,
                7.4
        ));
        results.add(new RetailerAvailability(
                "Publix",
                PROVIDER,
                "Search retailer",
                "Varies",
                "8.1 mi",
                "Pickup or delivery",
                "https://www.publix.com/search?searchTerm=" + Uri.encode(search),
                "Regional grocery result for nearby availability checks.",
                true,
                0.0,
                8.1
        ));
        results.add(new RetailerAvailability(
                "Amazon",
                PROVIDER,
                "Online option",
                "Varies",
                "Shipping",
                "Delivery",
                "https://www.amazon.com/s?k=" + Uri.encode(search),
                "Online fallback when nearby shelves do not show a match.",
                true,
                0.0,
                99.0
        ));
        return results;
    }

    @Override
    public List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) {
        // MockRetailerBackendClient handles alternatives globally for now
        return new ArrayList<>();
    }

    private String buildSearchTerm(RetailerProductQuery query) {
        if (query.productName != null && !query.productName.isEmpty()) {
            return query.productName;
        }
        if (query.brand != null && !query.brand.trim().isEmpty()) {
            return query.brand.trim();
        }
        return query.barcode != null ? query.barcode.trim() : "";
    }
}
