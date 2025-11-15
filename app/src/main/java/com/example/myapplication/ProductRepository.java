package com.example.myapplication;

import android.app.Application;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final ProductDao productDao;
    private final OpenFoodFactsApiClient apiClient;
    private final ExecutorService executorService;
    private final Application application;

    public enum DataStatus { FRESH, STALE, OFFLINE }

    public static class ProductResult {
        public final ProductWithDetails productWithDetails;
        public final DataStatus status;

        public ProductResult(ProductWithDetails productWithDetails, DataStatus status) {
            this.productWithDetails = productWithDetails;
            this.status = status;
        }
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }

    public ProductRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        this.productDao = db.productDao();
        this.apiClient = new OpenFoodFactsApiClient(application.getCacheDir());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void getProductByBarcode(String barcode, RepositoryCallback<ProductResult> callback) {
        executorService.execute(() -> {
            ProductWithDetails cachedProduct = productDao.getProductWithDetails(barcode);
            CacheMeta cacheMeta = productDao.getCacheMeta(barcode);

            long currentTime = System.currentTimeMillis();
            boolean isCacheStale = cacheMeta == null || (currentTime - cacheMeta.lastUpdated) > (24 * 60 * 60 * 1000); // 24 hours

            if (NetworkUtils.isOnline(application)) {
                if (cachedProduct == null || isCacheStale) {
                    fetchFromApi(barcode, callback, cachedProduct, isCacheStale);
                } else {
                    callback.onComplete(new ProductResult(cachedProduct, DataStatus.FRESH));
                }
            } else {
                if (cachedProduct != null) {
                    callback.onComplete(new ProductResult(cachedProduct, DataStatus.OFFLINE));
                } else {
                    callback.onError(new IOException("Offline and no cached data available."));
                }
            }
        });
    }

    private void fetchFromApi(String barcode, RepositoryCallback<ProductResult> callback, ProductWithDetails cachedProduct, boolean isCacheStale) {
        try {
            ProductResponse response = apiClient.getProduct(barcode);
            if (response != null && response.status == 1 && response.product != null) {
                ProductWithDetails fetchedProduct = responseToProductWithDetails(response, barcode);
                productDao.insertProductWithDetails(fetchedProduct);
                productDao.insertCacheMeta(new CacheMeta(barcode, System.currentTimeMillis()));
                callback.onComplete(new ProductResult(fetchedProduct, DataStatus.FRESH));
            } else {
                if (cachedProduct != null) {
                    callback.onComplete(new ProductResult(cachedProduct, isCacheStale ? DataStatus.STALE : DataStatus.FRESH));
                } else {
                    callback.onError(new Exception("Product not found."));
                }
            }
        } catch (IOException e) {
            if (cachedProduct != null) {
                callback.onComplete(new ProductResult(cachedProduct, isCacheStale ? DataStatus.STALE : DataStatus.FRESH));
            } else {
                callback.onError(e);
            }
        }
    }

    private ProductWithDetails responseToProductWithDetails(ProductResponse response, String barcode) {
        // This logic can be extracted to a mapper class for cleanliness
        ProductResponse.ProductData productData = response.product;
        Product product = new Product(barcode, productData.productName, productData.brands, productData.quantity, productData.imageUrl, productData.labels, productData.packaging, productData.categories, productData.servingSize, productData.nutriscoreGrade, productData.novaGroup, productData.ecoscoreGrade);

        Nutriments nutriments = null;
        if (productData.nutriments != null) {
            ProductResponse.NutrimentsData nutrimentsData = productData.nutriments;
            nutriments = new Nutriments(barcode, nutrimentsData.energy, nutrimentsData.fat, nutrimentsData.saturatedFat, nutrimentsData.carbohydrates, nutrimentsData.sugars, nutrimentsData.proteins, nutrimentsData.salt, nutrimentsData.fiber);
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.nutriments = nutriments;

        return productWithDetails;
    }

    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
