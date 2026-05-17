package com.example.myapplication;

import android.app.Application;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final ProductDao productDao;
    private final List<BarcodeApiClient> apiClients;
    private final ExecutorService executorService;
    private final Application application;

    private static final String IDLING_RESOURCE = "Network_Calls";
    public static final CountingIdlingResource idlingResource = new CountingIdlingResource(IDLING_RESOURCE);

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
        idlingResource.increment();
        executorService.execute(() -> {
            try {
                ProductWithDetails cachedProduct = productDao.getProductWithDetails(barcode);
                CacheMeta cacheMeta = productDao.getCacheMeta(barcode);

                long currentTime = System.currentTimeMillis();
                boolean isCacheStale = cacheMeta == null || (currentTime - cacheMeta.lastUpdated) > (24 * 60 * 60 * 1000);

                if (NetworkUtils.isOnline(application)) {
                    if (cachedProduct == null || isCacheStale) {
                        fetchFromApiChain(barcode, callback, cachedProduct, isCacheStale);
                    } else {
                        callback.onComplete(new ProductResult(cachedProduct, DataStatus.FRESH, "Cache"));
                        idlingResource.decrement();
                    }
                } else {
                    if (cachedProduct != null) {
                        callback.onComplete(new ProductResult( cachedProduct, DataStatus.OFFLINE, "Cache"));
                    } else {
                        callback.onError(new IOException("You are offline. Please check your connection."));
                    }
                    idlingResource.decrement();
                }
            } catch (Exception e) {
                callback.onError(e);
                idlingResource.decrement();
            }
        });
    }

    private void fetchFromApiChain(String barcode, RepositoryCallback<ProductResult> callback, ProductWithDetails cachedProduct, boolean isCacheStale) {
        ProductResponse finalResponse = null;
        String sourceName = "";
        int networkErrors = 0;

        for (BarcodeApiClient client : apiClients) {
            try {
                ProductResponse currentResponse = client.getProduct(barcode);
                if (currentResponse != null && currentResponse.status == 1 && currentResponse.product != null) {
                    boolean hasIngredients = (currentResponse.product.ingredientsText != null && !currentResponse.product.ingredientsText.isEmpty()) ||
                                           (currentResponse.product.ingredients != null && currentResponse.product.ingredients.length > 0);
                    if (hasIngredients) {
                        finalResponse = currentResponse;
                        sourceName = client.getClass().getSimpleName();
                        break; 
                    }
                }
            } catch (IOException e) {
                networkErrors++;
                e.printStackTrace();
            }
        }

        if (finalResponse != null) {
            ProductWithDetails fetchedProduct = responseToProductWithDetails(finalResponse, barcode);
            productDao.insertProductWithDetails(fetchedProduct);
            productDao.insertCacheMeta(new CacheMeta(barcode, System.currentTimeMillis()));
            callback.onComplete(new ProductResult(fetchedProduct, DataStatus.FRESH, sourceName));
        } else {
            if (cachedProduct != null) {
                callback.onComplete(new ProductResult(cachedProduct, isCacheStale ? DataStatus.STALE : DataStatus.FRESH, "Cache (Stale)"));
            } else {
                 if (networkErrors == apiClients.size()) {
                    callback.onError(new IOException("Network error. Please check your connection and try again."));
                } else {
                    callback.onError(new Exception("Product not found in any database."));
                }
            }
        }
        idlingResource.decrement();
    }

    private ProductWithDetails responseToProductWithDetails(ProductResponse response, String barcode) {
        ProductResponse.ProductData productData = response.product;
        Product product = new Product(barcode, productData.productName, productData.brands, productData.quantity, productData.imageUrl, productData.labels, productData.packaging, productData.categories, productData.servingSize, productData.nutriscoreGrade, productData.novaGroup, productData.ecoscoreGrade);

        Nutriments nutriments = null;
        if (productData.nutriments != null) {
            ProductResponse.NutrimentsData d = productData.nutriments;
            nutriments = new Nutriments(barcode,
                d.energy, d.energyKj, d.fat, d.saturatedFat, d.monounsaturatedFat, d.polyunsaturatedFat, d.transFat, d.cholesterol,
                d.carbohydrates, d.sugars, d.addedSugars, d.sucrose, d.glucose, d.fructose, d.lactose, d.maltose, d.maltodextrins, d.starch, d.polyols,
                d.fiber, d.proteins, d.salt, d.sodium, d.alcohol, d.vitaminA, d.vitaminD, d.vitaminE, d.vitaminK, d.vitaminC, d.vitaminB1, d.vitaminB2, d.vitaminPP,
                d.vitaminB6, d.vitaminB9, d.vitaminB12, d.biotin, d.pantothenicAcid, d.silica, d.bicarbonate, d.potassium, d.chloride, d.calcium, d.phosphorus,
                d.iron, d.magnesium, d.zinc, d.copper, d.manganese, d.fluoride, d.selenium, d.chromium, d.molybdenum, d.iodine, d.caffeine, d.taurine,
                d.omega3Fat, d.omega6Fat, d.omega9Fat, d.oleicAcid, d.linoleicAcid, d.gammaLinolenicAcid, d.dihomoGammaLinolenicAcid, d.arachidonicAcid,
                d.alphaLinolenicAcid, d.eicosapentaenoicAcid, d.docosahexaenoicAcid, d.carbonFootprint);
        }

        List<Ingredient> ingredients = new ArrayList<>();
        boolean hasAddedSugars = nutriments != null && nutriments.addedSugars != null && nutriments.addedSugars > 0;
        List<String> sugarKeywords = Arrays.asList("sugar", "syrup", "juice", "sweetener", "fructose", "dextrose", "cane");

        String ingredientsSource = productData.ingredientsText;
        if (ingredientsSource == null || ingredientsSource.isEmpty()) {
            ingredientsSource = productData.ingredientsTextEn;
        }

        if (ingredientsSource != null && !ingredientsSource.isEmpty()) {
            // Remove language tags like [en:sugar] and cleanup extra whitespace
            String cleanedText = ingredientsSource.replaceAll("\\[[a-zA-Z:-]+\\]", "").trim();
            
            List<String> ingredientStrings = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            int parenDepth = 0;
            for (char c : cleanedText.toCharArray()) {
                if (c == '(') parenDepth++;
                if (c == ')') parenDepth--;

                if (c == ',' && parenDepth == 0) {
                    ingredientStrings.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                ingredientStrings.add(current.toString());
            }

            int rank = 0;
            for (String ingredientText : ingredientStrings) {
                String trimmedText = ingredientText.trim();
                if (trimmedText.startsWith(",")) {
                    trimmedText = trimmedText.substring(1).trim();
                }
                trimmedText = trimmedText.replaceAll("_", "");

                if (!trimmedText.isEmpty()) {
                    String formattedText = trimmedText.substring(0, 1).toUpperCase() + trimmedText.substring(1).toLowerCase();
                    boolean isSugar = sugarKeywords.stream().anyMatch(formattedText.toLowerCase()::contains);
                    if (isSugar) {
                        if (hasAddedSugars) {
                            formattedText += " (Added Sugar)";
                        } else {
                            formattedText += " (Sugar)";
                        }
                    }
                    ingredients.add(new Ingredient(barcode, formattedText, rank++));
                }
            }
        } else if (productData.ingredients != null) {
            for (ProductResponse.IngredientsData ingredientData : productData.ingredients) {
                if (ingredientData != null && ingredientData.text != null) {
                    String cleanedText = ingredientData.text.replaceAll("\\[[a-zA-Z:-]+\\]", "").trim();
                    String formattedText = cleanedText.substring(0, 1).toUpperCase() + cleanedText.substring(1).toLowerCase();
                    boolean isSugar = sugarKeywords.stream().anyMatch(formattedText.toLowerCase()::contains);
                    if (isSugar) {
                        if (hasAddedSugars) {
                            formattedText += " (Added Sugar)";
                        } else {
                            formattedText += " (Sugar)";
                        }
                    }
                    ingredients.add(new Ingredient(barcode, formattedText, ingredientData.rank));
                }
            }
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.nutriments = nutriments;
        productWithDetails.ingredients = ingredients;

        return productWithDetails;
    }

    public void updateProductAiInsight(String barcode, String aiInsight) {
        executorService.execute(() -> productDao.updateAiInsight(barcode, aiInsight));
    }

    public void updateProductHealthScore(String barcode, int healthScore) {
        executorService.execute(() -> productDao.updateHealthScore(barcode, healthScore));
    }

    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
