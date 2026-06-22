package com.ciblorenzo.whatsonmyfood;

import java.io.IOException;

public interface BarcodeApiClient {
    ProductResponse getProduct(String barcode) throws IOException;
}
