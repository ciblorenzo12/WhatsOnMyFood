package com.example.myapplication.retail;

public class MarketplaceItem {
    public enum ItemType { ORIGINAL, ALTERNATIVE }

    public final String productName;
    public final String brand;
    public final String retailerName;
    public final String price;
    public final double priceValue;
    public final String distance;
    public final double distanceValue;
    public final int healthScore;
    public final String productUrl;
    public final String imageUrl;
    public final ItemType type;

    public MarketplaceItem(String productName, String brand, String retailerName, String price, double priceValue,
                           String distance, double distanceValue, int healthScore, String productUrl,
                           String imageUrl, ItemType type) {
        this.productName = productName;
        this.brand = brand;
        this.retailerName = retailerName;
        this.price = price;
        this.priceValue = priceValue;
        this.distance = distance;
        this.distanceValue = distanceValue;
        this.healthScore = healthScore;
        this.productUrl = productUrl;
        this.imageUrl = imageUrl;
        this.type = type;
    }
}
