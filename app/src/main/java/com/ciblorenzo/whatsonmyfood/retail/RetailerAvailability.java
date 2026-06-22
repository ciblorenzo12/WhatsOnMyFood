package com.ciblorenzo.whatsonmyfood.retail;

public class RetailerAvailability {
    public final String retailerName;
    public final String providerName;
    public final String availabilityStatus;
    public final String price;
    public final String distance;
    public final String fulfillment;
    public final String productUrl;
    public final String note;
    public final boolean available;
    public final double priceValue;
    public final double distanceValue;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final String mapUrl;

    public RetailerAvailability(String retailerName, String providerName, String availabilityStatus,
                                String price, String distance, String fulfillment, String productUrl,
                                String note, boolean available) {
        this(retailerName, providerName, availabilityStatus, price, distance, fulfillment, productUrl, note, available, 0.0, 0.0);
    }

    public RetailerAvailability(String retailerName, String providerName, String availabilityStatus,
                                String price, String distance, String fulfillment, String productUrl,
                                String note, boolean available, double priceValue, double distanceValue) {
        this(retailerName, providerName, availabilityStatus, price, distance, fulfillment, productUrl,
                note, available, priceValue, distanceValue, "", 0.0, 0.0, "");
    }

    public RetailerAvailability(String retailerName, String providerName, String availabilityStatus,
                                String price, String distance, String fulfillment, String productUrl,
                                String note, boolean available, double priceValue, double distanceValue,
                                String address, double latitude, double longitude, String mapUrl) {
        this.retailerName = retailerName;
        this.providerName = providerName;
        this.availabilityStatus = availabilityStatus;
        this.price = price;
        this.distance = distance;
        this.fulfillment = fulfillment;
        this.productUrl = productUrl;
        this.note = note;
        this.available = available;
        this.priceValue = priceValue;
        this.distanceValue = distanceValue;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapUrl = mapUrl;
    }
}
