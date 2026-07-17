package com.ciblorenzo.whatsonmyfood;

/** Repository boundary used by the barcode flow. */
public interface ProductLookupGateway {
    void getProductByBarcode(
            String barcode,
            ProductRepository.RepositoryCallback<ProductRepository.ProductResult> callback
    );
}
