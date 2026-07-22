package com.ciblorenzo.whatsonmyfood;

import android.app.Application;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository implements ProductLookupGateway {

    private final ProductDao productDao;
    private final List<BarcodeApiClient> apiClients;
    private final RagIngredientLookupClient ragIngredientLookupClient;
    private final ExecutorService executorService;
    private final Application application;

    private static final String IDLING_RESOURCE = "Network_Calls";
    public static final CountingIdlingResource idlingResource = new CountingIdlingResource(IDLING_RESOURCE);

    public enum DataStatus { FRESH, STALE, OFFLINE }
    public enum SourceStatus {
        FRESH_CACHED_RESULT,
        UPDATED_FROM_PRODUCT_DATABASE,
        FALLBACK_PRODUCT_SOURCE,
        SAVED_OFFLINE_RESULT,
        INFORMATION_MAY_BE_OUTDATED,
        INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE
    }

    public static class ProductResult {
        public final ProductWithDetails productWithDetails;
        public final DataStatus status;
        public final String apiSourceName;
        public final List<SourceStatus> sourceStatuses;

        public ProductResult(ProductWithDetails productWithDetails, DataStatus status, String apiSourceName) {
            this(productWithDetails, status, apiSourceName, Collections.emptyList());
        }

        public ProductResult(
                ProductWithDetails productWithDetails,
                DataStatus status,
                String apiSourceName,
                List<SourceStatus> sourceStatuses
        ) {
            this.productWithDetails = productWithDetails;
            this.status = status;
            this.apiSourceName = apiSourceName;
            this.sourceStatuses = Collections.unmodifiableList(new ArrayList<>(
                    sourceStatuses == null ? Collections.emptyList() : sourceStatuses
            ));
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
        String languageCode = LanguageManager.getLanguageCode(application);
        this.apiClients = Arrays.asList(
                new OpenFoodFactsApiClient(application.getCacheDir(), languageCode),
                new FoodDataCentralClient(),
                new NutritionixClient(),
                new WalmartBackendProductClient(),
                new BarcodeLookupClient(),
                new UpcItemDbClient()
        );
        this.ragIngredientLookupClient = new RagIngredientLookupClient();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void getProductByBarcode(String barcode, RepositoryCallback<ProductResult> callback) {
        idlingResource.increment();
        executorService.execute(() -> {
            try {
                ProductWithDetails cachedProduct = productDao.getProductWithDetails(barcode);
                CacheMeta cacheMeta = productDao.getCacheMeta(barcode);

                long currentTime = System.currentTimeMillis();
                boolean isCacheStale = cacheMeta == null || (currentTime - cacheMeta.lastUpdated) > (24 * 60 * 60 * 1000);

                if (cachedProduct != null && hasMeaningfulProductName(cachedProduct) && hasParsedIngredients(cachedProduct)) {
                    callback.onComplete(new ProductResult(
                            cachedProduct,
                            isCacheStale ? DataStatus.STALE : DataStatus.FRESH,
                            hasSavedAiInsight(cachedProduct) ? "Cache (Saved AI insight)" : "Cache",
                            SourceStatusResolver.forCachedResult(isCacheStale)
                    ));
                    idlingResource.decrement();
                    return;
                }

                if (NetworkUtils.isOnline(application)) {
                    fetchFromApiChain(barcode, callback, cachedProduct, isCacheStale);
                } else {
                    if (cachedProduct != null) {
                        callback.onComplete(new ProductResult(
                                cachedProduct,
                                DataStatus.OFFLINE,
                                "Cache",
                                SourceStatusResolver.forSavedOfflineResult()
                        ));
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

    public void refreshProductByBarcode(String barcode, RepositoryCallback<ProductResult> callback) {
        idlingResource.increment();
        executorService.execute(() -> {
            try {
                ProductWithDetails cachedProduct = productDao.getProductWithDetails(barcode);
                if (NetworkUtils.isOnline(application)) {
                    fetchFromApiChain(barcode, callback, cachedProduct, true, false);
                } else {
                    callback.onError(new IOException("You are offline. Please check your connection."));
                    idlingResource.decrement();
                }
            } catch (Exception e) {
                callback.onError(e);
                idlingResource.decrement();
            }
        });
    }

    private void fetchFromApiChain(String barcode, RepositoryCallback<ProductResult> callback, ProductWithDetails cachedProduct, boolean isCacheStale) {
        fetchFromApiChain(barcode, callback, cachedProduct, isCacheStale, true);
    }

    private void fetchFromApiChain(String barcode, RepositoryCallback<ProductResult> callback, ProductWithDetails cachedProduct, boolean isCacheStale, boolean preserveAiInsight) {
        ProductResponse bestResponse = null;
        String sourceName = "";
        String supplementalLabels = "";
        String openFoodFactsIngredients = "";
        String supplementalIngredients = "";
        String languageCode = LanguageManager.getLanguageCode(application);
        int bestScore = Integer.MIN_VALUE;
        int networkErrors = 0;

        for (BarcodeApiClient client : apiClients) {
            try {
                ProductResponse currentResponse = client.getProduct(barcode);
                if (isValidResponse(currentResponse)) {
                    String currentLabels = localizedLabels(currentResponse.product, languageCode);
                    supplementalLabels = mergeLabelText(supplementalLabels, currentLabels);
                    String currentIngredients = localizedIngredients(currentResponse.product, languageCode);
                    if (!isBlank(currentIngredients)) {
                        if (client instanceof OpenFoodFactsApiClient) {
                            openFoodFactsIngredients = currentIngredients;
                        } else if (isBlank(supplementalIngredients)) {
                            supplementalIngredients = currentIngredients;
                        }
                    }

                    int candidateScore = scoreResponse(currentResponse);
                    if (candidateScore > bestScore) {
                        bestResponse = currentResponse;
                        sourceName = client.getClass().getSimpleName();
                        bestScore = candidateScore;
                    }

                    if (candidateScore >= 145 && client instanceof OpenFoodFactsApiClient) {
                        break;
                    }
                }
            } catch (IOException e) {
                networkErrors++;
                e.printStackTrace();
            }
        }

        if (bestResponse != null) {
            ProductWithDetails fetchedProduct = responseToProductWithDetails(bestResponse, barcode);
            if (fetchedProduct.product != null) {
                fetchedProduct.product.labels = mergeLabelText(fetchedProduct.product.labels, supplementalLabels);
            }
            boolean ingredientsRecovered = fillMissingIngredients(
                    fetchedProduct,
                    barcode,
                    firstNonEmpty(openFoodFactsIngredients, supplementalIngredients)
            );
            preserveSavedFields(fetchedProduct, cachedProduct, preserveAiInsight);
            productDao.insertProductWithDetails(fetchedProduct);
            productDao.insertCacheMeta(new CacheMeta(barcode, System.currentTimeMillis()));
            boolean usedFallbackSource = !OpenFoodFactsApiClient.class.getSimpleName().equals(sourceName);
            callback.onComplete(new ProductResult(
                    fetchedProduct,
                    DataStatus.FRESH,
                    sourceName,
                    SourceStatusResolver.forUpdatedDatabaseResult(usedFallbackSource, ingredientsRecovered)
            ));
        } else {
            if (cachedProduct != null) {
                callback.onComplete(new ProductResult(
                        cachedProduct,
                        DataStatus.STALE,
                        "Cache",
                        SourceStatusResolver.forUnavailableRefreshResult()
                ));
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
        String languageCode = LanguageManager.getLanguageCode(application);
        Product product = new Product(
                barcode,
                localizedProductName(productData, languageCode),
                productData.brands,
                productData.quantity,
                firstNonEmpty(productData.imageUrl, productData.imageFrontUrl),
                localizedLabels(productData, languageCode),
                localizedValue(languageCode, productData.packaging, productData.packagingEn, productData.packagingEs, productData.packagingFr),
                localizedValue(languageCode, productData.categories, productData.categoriesEn, productData.categoriesEs, productData.categoriesFr),
                productData.servingSize,
                productData.nutriscoreGrade,
                productData.novaGroup,
                productData.ecoscoreGrade
        );

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

        String ingredientsSource = localizedIngredients(productData, languageCode);
        IngredientTextParser.ParsedLabel parsedLabel = IngredientTextParser.parseLabel(ingredientsSource);

        if (ingredientsSource != null && !ingredientsSource.isEmpty()) {
            int rank = 0;
            for (String ingredientText : parsedLabel.ingredients) {
                String formattedText = formatIngredientText(ingredientText, sugarKeywords, hasAddedSugars);
                if (!formattedText.isEmpty()) {
                    ingredients.add(new Ingredient(barcode, formattedText, rank++));
                }
            }
        } else if (productData.ingredients != null) {
            for (ProductResponse.IngredientsData ingredientData : productData.ingredients) {
                if (ingredientData != null && ingredientData.text != null) {
                    String formattedText = formatIngredientText(ingredientData.text, sugarKeywords, hasAddedSugars);
                    if (!formattedText.isEmpty()) {
                        ingredients.add(new Ingredient(barcode, formattedText, ingredientData.rank));
                    }
                }
            }
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.nutriments = nutriments;
        productWithDetails.ingredients = ingredients;
        productWithDetails.containsAllergens = mergeAllergens(
                parsedLabel.containsAllergens,
                parseApiAllergens(productData.allergens, productData.allergensTags)
        );
        productWithDetails.mayContainAllergens = new ArrayList<>(parsedLabel.mayContainAllergens);

        return productWithDetails;
    }

    private List<String> parseApiAllergens(String allergens, String[] allergenTags) {
        StringBuilder source = new StringBuilder();
        if (!isBlank(allergens)) source.append(allergens.trim());
        if (allergenTags != null) {
            for (String tag : allergenTags) {
                if (isBlank(tag)) continue;
                if (source.length() > 0) source.append(", ");
                source.append(tag.trim());
            }
        }
        if (source.length() == 0) return new ArrayList<>();
        return new ArrayList<>(IngredientTextParser.parseLabel("Contains: " + source).containsAllergens);
    }

    private List<String> mergeAllergens(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (first != null) {
            for (String value : first) {
                if (!isBlank(value) && seen.add(value.toLowerCase(Locale.US))) merged.add(value);
            }
        }
        if (second != null) {
            for (String value : second) {
                if (!isBlank(value) && seen.add(value.toLowerCase(Locale.US))) merged.add(value);
            }
        }
        return merged;
    }

    private boolean isValidResponse(ProductResponse response) {
        if (response == null || response.status != 1 || response.product == null) {
            return false;
        }
        String languageCode = LanguageManager.getLanguageCode(application);
        return isMeaningfulProductName(localizedProductName(response.product, languageCode));
    }

    private int scoreResponse(ProductResponse response) {
        ProductResponse.ProductData product = response.product;
        int score = 0;
        if (isLikelyUnitedStatesProduct(product)) score += 100;
        if (hasIngredients(product)) score += 25;
        if (hasNutrients(product)) score += 15;
        if (!isBlank(firstNonEmpty(product.imageUrl, product.imageFrontUrl))) score += 5;
        if (!isBlank(product.brands)) score += 3;
        if (!isBlank(product.quantity)) score += 2;
        return score;
    }

    private boolean hasIngredients(ProductResponse.ProductData product) {
        return !isBlank(product.ingredientsText)
                || !isBlank(product.ingredientsTextEn)
                || !isBlank(product.ingredientsTextEs)
                || !isBlank(product.ingredientsTextFr)
                || !isBlank(product.ingredientsTextWithAllergensEn)
                || (product.ingredients != null && product.ingredients.length > 0);
    }

    private boolean hasNutrients(ProductResponse.ProductData product) {
        ProductResponse.NutrimentsData d = product.nutriments;
        return d != null && (d.energy != null
                || d.energyKj != null
                || d.fat != null
                || d.saturatedFat != null
                || d.carbohydrates != null
                || d.sugars != null
                || d.fiber != null
                || d.proteins != null
                || d.sodium != null
                || d.salt != null);
    }

    private boolean isLikelyUnitedStatesProduct(ProductResponse.ProductData product) {
        if (product == null) return false;
        String countries = product.countries == null ? "" : product.countries.toLowerCase(Locale.US);
        if (countries.contains("united states") || countries.contains("usa") || countries.contains("u.s.")) {
            return true;
        }
        if (product.countriesTags != null) {
            for (String tag : product.countriesTags) {
                String normalized = tag == null ? "" : tag.toLowerCase(Locale.US);
                if (normalized.contains("united-states") || normalized.equals("en:us")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String localizedProductName(ProductResponse.ProductData product, String languageCode) {
        if (product == null) return "";
        if ("es".equals(languageCode)) {
            return firstNonEmpty(product.productNameEs, product.genericNameEs, product.productNameEn, product.genericNameEn, product.productName, product.genericName);
        }
        if ("fr".equals(languageCode)) {
            return firstNonEmpty(product.productNameFr, product.genericNameFr, product.productNameEn, product.genericNameEn, product.productName, product.genericName);
        }
        return firstNonEmpty(product.productNameEn, product.genericNameEn, product.productName, product.genericName);
    }

    private boolean hasMeaningfulProductName(ProductWithDetails productWithDetails) {
        return productWithDetails != null
                && productWithDetails.product != null
                && isMeaningfulProductName(productWithDetails.product.productName);
    }

    private boolean isMeaningfulProductName(String value) {
        if (isBlank(value)) return false;
        String normalized = value.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        return !normalized.equals("scanned product")
                && !normalized.equals("scanned")
                && !normalized.equals("product")
                && !normalized.equals("name")
                && !normalized.equals("unknown")
                && !normalized.equals("unknown product")
                && !normalized.equals("n a");
    }

    private String localizedValue(String languageCode, String defaultValue, String englishValue, String spanishValue, String frenchValue) {
        if ("es".equals(languageCode)) {
            return firstNonEmpty(spanishValue, englishValue, defaultValue);
        }
        if ("fr".equals(languageCode)) {
            return firstNonEmpty(frenchValue, englishValue, defaultValue);
        }
        return firstNonEmpty(englishValue, defaultValue);
    }

    private String localizedLabels(ProductResponse.ProductData product, String languageCode) {
        if (product == null) return "";
        String labelText = localizedValue(languageCode, product.labels, product.labelsEn, product.labelsEs, product.labelsFr);
        return combineLabels(labelText, product.labelsTags);
    }

    private String combineLabels(String labelText, String[] labelTags) {
        List<String> labels = new ArrayList<>();
        if (!isBlank(labelText)) {
            labels.add(labelText.trim());
        }
        if (labelTags != null) {
            for (String tag : labelTags) {
                if (!isBlank(tag)) {
                    labels.add(tag.trim());
                }
            }
        }
        return String.join(", ", labels);
    }

    private boolean fillMissingIngredients(ProductWithDetails productWithDetails, String barcode, String retrievedIngredients) {
        if (productWithDetails == null || productWithDetails.product == null || hasParsedIngredients(productWithDetails)) {
            return false;
        }

        if (!isBlank(retrievedIngredients)) {
            productWithDetails.ingredients = parseIngredients(barcode, retrievedIngredients, productWithDetails.nutriments);
            if (hasParsedIngredients(productWithDetails)) {
                return true;
            }
        }

        try {
            ProductResponse ragResponse = ragIngredientLookupClient.getIngredients(
                    barcode,
                    productWithDetails.product.productName,
                    productWithDetails.product.brands
            );
            if (ragResponse == null || ragResponse.status != 1 || ragResponse.product == null) {
                return false;
            }

            String ingredientsText = localizedIngredients(ragResponse.product, LanguageManager.getLanguageCode(application));
            if (isBlank(ingredientsText)) {
                return false;
            }

            productWithDetails.ingredients = parseIngredients(barcode, ingredientsText, productWithDetails.nutriments);
            return hasParsedIngredients(productWithDetails);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasParsedIngredients(ProductWithDetails productWithDetails) {
        if (productWithDetails == null || productWithDetails.ingredients == null || productWithDetails.ingredients.isEmpty()) {
            return false;
        }
        int ingredientCount = 0;
        String onlyIngredient = "";
        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient != null && !isBlank(ingredient.text)) {
                ingredientCount++;
                onlyIngredient = ingredient.text.trim();
            }
        }
        if (ingredientCount == 1 && isLikelyWarningOnlyIngredient(onlyIngredient)) {
            return false;
        }
        return ingredientCount > 0;
    }

    private boolean isLikelyWarningOnlyIngredient(String ingredientText) {
        if (isBlank(ingredientText)) return true;
        String normalized = ingredientText.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
        return normalized.equals("phenylalanine")
                || normalized.equals("no calories")
                || normalized.equals("no sugar")
                || normalized.equals("zero calories")
                || normalized.equals("zero sugar");
    }

    private List<Ingredient> parseIngredients(String barcode, String ingredientsSource, Nutriments nutriments) {
        List<Ingredient> ingredients = new ArrayList<>();
        boolean hasAddedSugars = nutriments != null && nutriments.addedSugars != null && nutriments.addedSugars > 0;
        List<String> sugarKeywords = Arrays.asList("sugar", "syrup", "juice", "sweetener", "fructose", "dextrose", "cane");

        int rank = 0;
        for (String ingredientText : IngredientTextParser.parseIngredientCandidates(ingredientsSource)) {
            String formattedText = formatIngredientText(ingredientText, sugarKeywords, hasAddedSugars);
            if (!formattedText.isEmpty()) {
                ingredients.add(new Ingredient(barcode, formattedText, rank++));
            }
        }
        return ingredients;
    }

    private String mergeLabelText(String existingLabels, String newLabels) {
        if (isBlank(existingLabels)) return newLabels == null ? "" : newLabels.trim();
        if (isBlank(newLabels)) return existingLabels.trim();
        String existing = existingLabels.trim();
        String candidate = newLabels.trim();
        if (existing.toLowerCase(Locale.US).contains(candidate.toLowerCase(Locale.US))) {
            return existing;
        }
        return existing + ", " + candidate;
    }

    private String localizedIngredients(ProductResponse.ProductData product, String languageCode) {
        if (product == null) return "";
        if ("es".equals(languageCode)) {
            return richestIngredientsText(
                    product.ingredientsTextEs,
                    product.ingredientsTextEn,
                    product.ingredientsText,
                    structuredIngredientsText(product.ingredients),
                    product.ingredientsTextWithAllergensEs,
                    product.ingredientsTextWithAllergensEn,
                    product.ingredientsTextWithAllergens
            );
        }
        if ("fr".equals(languageCode)) {
            return richestIngredientsText(
                    product.ingredientsTextFr,
                    product.ingredientsTextEn,
                    product.ingredientsText,
                    structuredIngredientsText(product.ingredients),
                    product.ingredientsTextWithAllergensFr,
                    product.ingredientsTextWithAllergensEn,
                    product.ingredientsTextWithAllergens
            );
        }
        return richestIngredientsText(
                product.ingredientsTextEn,
                product.ingredientsText,
                structuredIngredientsText(product.ingredients),
                product.ingredientsTextWithAllergensEn,
                product.ingredientsTextWithAllergens,
                product.ingredientsTextEs,
                product.ingredientsTextFr,
                product.ingredientsTextWithAllergensEs,
                product.ingredientsTextWithAllergensFr
        );
    }

    private String richestIngredientsText(String... values) {
        String best = "";
        int bestCount = 0;
        if (values == null) return best;
        for (String value : values) {
            if (isBlank(value)) continue;
            int count = IngredientTextParser.parseIngredientCandidates(value).size();
            if (count > bestCount) {
                best = value.trim();
                bestCount = count;
            }
        }
        return !best.isEmpty() ? best : firstNonEmpty(values);
    }

    private String structuredIngredientsText(ProductResponse.IngredientsData[] ingredients) {
        if (ingredients == null || ingredients.length == 0) return "";
        List<ProductResponse.IngredientsData> sortedIngredients = new ArrayList<>(Arrays.asList(ingredients));
        sortedIngredients.sort((left, right) -> Integer.compare(left != null ? left.rank : Integer.MAX_VALUE, right != null ? right.rank : Integer.MAX_VALUE));
        List<String> values = new ArrayList<>();
        for (ProductResponse.IngredientsData ingredient : sortedIngredients) {
            if (ingredient != null && !isBlank(ingredient.text)) {
                values.add(ingredient.text.trim());
            }
        }
        return String.join(", ", values);
    }

    private String formatIngredientText(String ingredientText, List<String> sugarKeywords, boolean hasAddedSugars) {
        String cleaned = IngredientTextParser.cleanIngredientText(ingredientText).replace("_", "").trim();
        if (cleaned.isEmpty()) return "";
        String formattedText = cleaned.substring(0, 1).toUpperCase(Locale.US) + cleaned.substring(1).toLowerCase(Locale.US);
        boolean isSugar = sugarKeywords.stream().anyMatch(formattedText.toLowerCase(Locale.US)::contains);
        if (isSugar) {
            formattedText += hasAddedSugars ? " (Added Sugar)" : " (Sugar)";
        }
        return formattedText;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasSavedAiInsight(ProductWithDetails productWithDetails) {
        return productWithDetails != null
                && productWithDetails.product != null
                && productWithDetails.product.aiInsight != null
                && !productWithDetails.product.aiInsight.trim().isEmpty();
    }

    private void preserveSavedFields(ProductWithDetails fetchedProduct, ProductWithDetails cachedProduct, boolean preserveAiInsight) {
        if (fetchedProduct == null || fetchedProduct.product == null || cachedProduct == null || cachedProduct.product == null) {
            return;
        }

        if (preserveAiInsight) {
            fetchedProduct.product.aiInsight = cachedProduct.product.aiInsight;
            fetchedProduct.product.healthScore = cachedProduct.product.healthScore;
        }
        fetchedProduct.product.isFavorite = cachedProduct.product.isFavorite;
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
