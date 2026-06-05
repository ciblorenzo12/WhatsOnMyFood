package com.example.myapplication;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FoodDataCentralClient implements BarcodeApiClient {

    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        String apiKey = BuildConfig.FDC_API_KEY == null || BuildConfig.FDC_API_KEY.trim().isEmpty()
                ? "DEMO_KEY"
                : BuildConfig.FDC_API_KEY.trim();

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("query", barcode)
                .addQueryParameter("dataType", "Branded")
                .addQueryParameter("pageSize", "10")
                .addQueryParameter("sortBy", "fdcId")
                .addQueryParameter("sortOrder", "desc")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                if (response.code() == 404) return null;
                throw new IOException("FoodData Central lookup failed with HTTP " + response.code());
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray foods = root.has("foods") && root.get("foods").isJsonArray()
                    ? root.getAsJsonArray("foods")
                    : new JsonArray();

            JsonObject bestFood = null;
            for (JsonElement element : foods) {
                if (!element.isJsonObject()) continue;
                JsonObject food = element.getAsJsonObject();
                String gtinUpc = getString(food, "gtinUpc");
                if (barcodeMatches(barcode, gtinUpc)) {
                    bestFood = food;
                    break;
                }
            }

            return bestFood != null ? mapFood(bestFood, barcode) : null;
        }
    }

    private ProductResponse mapFood(JsonObject food, String barcode) {
        ProductResponse response = new ProductResponse();
        response.status = 1;
        response.product = new ProductResponse.ProductData();

        String description = titleCase(getString(food, "description"));
        response.product.productName = firstNonEmpty(description, getString(food, "lowercaseDescription"));
        response.product.brands = firstNonEmpty(getString(food, "brandName"), getString(food, "brandOwner"));
        response.product.quantity = getString(food, "packageWeight");
        response.product.categories = firstNonEmpty(getString(food, "brandedFoodCategory"), "USDA Branded Food");
        response.product.countries = firstNonEmpty(getString(food, "marketCountry"), "United States");
        response.product.countriesTags = new String[]{"en:united-states"};
        response.product.servingSize = formatServingSize(food);
        response.product.ingredientsText = getString(food, "ingredients");
        response.product.nutriments = mapNutrients(food.getAsJsonArray("foodNutrients"));

        return response;
    }

    private ProductResponse.NutrimentsData mapNutrients(JsonArray nutrients) {
        if (nutrients == null) return null;
        ProductResponse.NutrimentsData data = new ProductResponse.NutrimentsData();

        for (JsonElement element : nutrients) {
            if (!element.isJsonObject()) continue;
            JsonObject nutrient = element.getAsJsonObject();
            String name = getString(nutrient, "nutrientName").toLowerCase(Locale.US);
            Double value = getDouble(nutrient, "value");
            if (value == null) continue;

            if (name.contains("energy") && name.contains("kcal")) data.energy = value;
            else if (name.equals("total lipid (fat)") || name.equals("total fat")) data.fat = value;
            else if (name.contains("saturated")) data.saturatedFat = value;
            else if (name.contains("trans")) data.transFat = value;
            else if (name.contains("carbohydrate")) data.carbohydrates = value;
            else if (name.contains("sugars")) data.sugars = value;
            else if (name.contains("fiber")) data.fiber = value;
            else if (name.contains("protein")) data.proteins = value;
            else if (name.contains("sodium")) {
                data.sodium = value / 1000.0;
                data.salt = data.sodium * 2.5;
            } else if (name.contains("cholesterol")) {
                data.cholesterol = value / 1000.0;
            }
        }

        return data;
    }

    private String formatServingSize(JsonObject food) {
        Double servingSize = getDouble(food, "servingSize");
        String unit = getString(food, "servingSizeUnit");
        if (servingSize == null) return "";
        if (Math.rint(servingSize) == servingSize) {
            return String.format(Locale.US, "%.0f %s", servingSize, unit).trim();
        }
        return String.format(Locale.US, "%.2f %s", servingSize, unit).trim();
    }

    private boolean barcodeMatches(String scanned, String candidate) {
        String scannedDigits = digitsOnly(scanned);
        String candidateDigits = digitsOnly(candidate);
        if (scannedDigits.isEmpty() || candidateDigits.isEmpty()) return false;
        return scannedDigits.equals(candidateDigits)
                || scannedDigits.endsWith(candidateDigits)
                || candidateDigits.endsWith(scannedDigits);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsString().trim();
    }

    private Double getDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String titleCase(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String lower = text.trim().toLowerCase(Locale.US);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean capitalizeNext = true;
        for (char c : lower.toCharArray()) {
            if (Character.isLetter(c) && capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
                capitalizeNext = c == ' ' || c == '-' || c == '/';
            }
        }
        return builder.toString();
    }
}
