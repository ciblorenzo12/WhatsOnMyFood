package com.example.myapplication.retail;

public class RetailerAlternative {
    public final String productName;
    public final String brand;
    public final String reason;
    public final String healthSignal;
    public final String retailerHint;
    public final String productUrl;
    public final String imageUrl;
    public final int healthScore;
    public final double priceValue;
    public final double distanceValue;

    public RetailerAlternative(String productName, String brand, String reason, String healthSignal,
                               String retailerHint, String productUrl, String imageUrl) {
        this(productName, brand, reason, healthSignal, retailerHint, productUrl, imageUrl, 100, 0.0, 0.0);
    }

    public RetailerAlternative(String productName, String brand, String reason, String healthSignal,
                               String retailerHint, String productUrl, String imageUrl,
                               int healthScore, double priceValue, double distanceValue) {
        this.productName = productName;
        this.brand = brand;
        this.reason = reason;
        this.healthSignal = healthSignal;
        this.retailerHint = retailerHint;
        this.productUrl = productUrl;
        this.imageUrl = imageUrl;
        this.healthScore = healthScore;
        this.priceValue = priceValue;
        this.distanceValue = distanceValue;
    }
}

