package com.example.myapplication;

import java.io.IOException;

public interface BarcodeApiClient {
    ProductResponse getProduct(String barcode) throws IOException;
}
