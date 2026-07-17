package com.ciblorenzo.whatsonmyfood;

/** Keeps the scanner-to-repository handoff explicit and testable. */
public final class ProductLookupDispatcher {

    private ProductLookupDispatcher() {
    }

    public static void dispatch(
            String barcode,
            ProductLookupGateway repository,
            ProductRepository.RepositoryCallback<ProductRepository.ProductResult> callback
    ) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (callback == null) throw new IllegalArgumentException("callback is required");

        String validatedBarcode = BarcodeScanGate.normalizeAndValidate(barcode);
        if (validatedBarcode == null) {
            callback.onError(new IllegalArgumentException("Invalid product GTIN barcode"));
            return;
        }
        repository.getProductByBarcode(validatedBarcode, callback);
    }
}
