package com.example.myapplication;

import android.app.Application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final ProductDao productDao;
    private final List<BarcodeApiClient> apiClients;
    private final ExecutorService executorService;
    private final Application application;

    public enum DataStatus { FRESH, STALE, OFFLINE }

    public static class ProductResult {
        public final ProductWithDetails productWithDetails;
        public final DataStatus status;
        public final String apiSourceName;

        public ProductResult(ProductWithDetails productWithDetails, DataStatus status, String apiSourceName) {
            this.productWithDetails = productWithDetails;
            this.status = status;
            this.apiSourceName = apiSourceName;
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
        this.apiClients = Arrays.asList(
                new FoodDataCentralClient(),
                new NutritionixClient(),
                new OpenFoodFactsApiClient(application.getCacheDir())
        );
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
                    fetchFromApiChain(barcode, callback, cachedProduct, isCacheStale);
                } else {
                    callback.onComplete(new ProductResult(cachedProduct, DataStatus.FRESH, "Cache"));
                }
            } else {
                if (cachedProduct != null) {
                    callback.onComplete(new ProductResult(cachedProduct, DataStatus.OFFLINE, "Cache"));
                } else {
                    callback.onError(new IOException("Offline and no cached data available."));
                }
            }
        });
    }

    private void fetchFromApiChain(String barcode, RepositoryCallback<ProductResult> callback, ProductWithDetails cachedProduct, boolean isCacheStale) {
        ProductResponse response = null;
        String sourceName = "";
        for (BarcodeApiClient client : apiClients) {
            try {
                response = client.getProduct(barcode);
                if (response != null && response.status == 1 && response.product != null && 
                    (response.product.ingredientsText != null && !response.product.ingredientsText.isEmpty() || 
                     response.product.ingredients != null && response.product.ingredients.length > 0)) {
                    sourceName = client.getClass().getSimpleName();
                    break; 
                }
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }

        if (response != null && response.status == 1 && response.product != null) {
            ProductWithDetails fetchedProduct = responseToProductWithDetails(response, barcode);
            productDao.insertProductWithDetails(fetchedProduct);
            productDao.insertCacheMeta(new CacheMeta(barcode, System.currentTimeMillis()));
            callback.onComplete(new ProductResult(fetchedProduct, DataStatus.FRESH, sourceName));
        } else {
            if (cachedProduct != null) {
                callback.onComplete(new ProductResult(cachedProduct, isCacheStale ? DataStatus.STALE : DataStatus.FRESH, "Cache"));
            } else {
                callback.onError(new Exception("Product not found in any API."));
            }
        }
    }

    private ProductWithDetails responseToProductWithDetails(ProductResponse response, String barcode) {
        ProductResponse.ProductData productData = response.product;
        Product product = new Product(barcode, productData.productName, productData.brands, productData.quantity, productData.imageUrl, productData.labels, productData.packaging, productData.categories, productData.servingSize, productData.nutriscoreGrade, productData.novaGroup, productData.ecoscoreGrade);

        Nutriments nutriments = null;
        if (productData.nutriments != null) {
            ProductResponse.NutrimentsData nutrimentsData = productData.nutriments;
            nutriments = new Nutriments(barcode, nutrimentsData.energy, nutrimentsData.fat, nutrimentsData.saturatedFat, nutrimentsData.carbohydrates, nutrimentsData.sugars, nutrimentsData.proteins, nutrimentsData.salt, nutrimentsData.fiber);
        }

        List<Ingredient> ingredients = new ArrayList<>();
        if (productData.ingredientsText != null && !productData.ingredientsText.isEmpty()) {
            String cleanedText = productData.ingredientsText.replaceAll("\\[[a-zA-Z-]+\\]", "").trim();
            String[] ingredientsArray = cleanedText.split(",");
            int rank = 0;
            for (String ingredientText : ingredientsArray) {
                String trimmedText = ingredientText.trim().replaceAll("_", "");
                if (!trimmedText.isEmpty()) {
                    String formattedText = trimmedText.substring(0, 1).toUpperCase() + trimmedText.substring(1).toLowerCase();
                    ingredients.add(new Ingredient(barcode, formattedText, rank++));
                }
            }
        } else if (productData.ingredients != null) {
            for (ProductResponse.IngredientsData ingredientData : productData.ingredients) {
                if (ingredientData.text != null && !ingredientData.text.isEmpty()) {
                    String formattedText = ingredientData.text.substring(0, 1).toUpperCase() + ingredientData.text.substring(1).toLowerCase();
                    ingredients.add(new Ingredient(barcode, formattedText, ingredientData.rank));
                }
            }
        }

        if (ingredients.size() == 1 && productData.labels != null && productData.labels.toLowerCase().contains("organic")) {
            Ingredient singleIngredient = ingredients.get(0);
            if (singleIngredient.text != null && !singleIngredient.text.toLowerCase().contains("organic")) {
                singleIngredient.text = "Organic " + singleIngredient.text;
            }
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.nutriments = nutriments;
        productWithDetails.ingredients = ingredients;

        return productWithDetails;
    }

    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
