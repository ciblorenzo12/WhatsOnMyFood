package com.example.myapplication.retail;

public class RetailerProductQuery {
    public final String barcode;
    public final String productName;
    public final String brand;
    public final String category;
    public final String zipCode;
    public final double latitude;
    public final double longitude;

    public RetailerProductQuery(String barcode, String productName, String brand, String category, String zipCode, double latitude, double longitude) {
        this.barcode = barcode;
        this.productName = productName;
        this.brand = brand;
        this.category = category;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public RetailerProductQuery(String barcode, String productName, String brand, String category, String zipCode) {
        this(barcode, productName, brand, category, zipCode, 0, 0);
    }
}

