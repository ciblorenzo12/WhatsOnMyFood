package com.ciblorenzo.whatsonmyfood;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NutritionixClient implements BarcodeApiClient {

    private static final String BASE_URL = "https://trackapi.nutritionix.com/v2/search/item";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        if (isBlank(BuildConfig.NUTRITIONIX_APP_ID) || isBlank(BuildConfig.NUTRITIONIX_APP_KEY)) {
            return null;
        }

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("upc", barcode)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("x-app-id", BuildConfig.NUTRITIONIX_APP_ID.trim())
                .addHeader("x-app-key", BuildConfig.NUTRITIONIX_APP_KEY.trim())
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Nutritionix lookup failed with HTTP " + response.code());
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray foods = root.has("foods") && root.get("foods").isJsonArray()
                    ? root.getAsJsonArray("foods")
                    : new JsonArray();
            if (foods.size() == 0 || !foods.get(0).isJsonObject()) return null;
            return mapFood(foods.get(0).getAsJsonObject());
        }
    }

    private ProductResponse mapFood(JsonObject food) {
        ProductResponse response = new ProductResponse();
        response.status = 1;
        response.product = new ProductResponse.ProductData();
        response.product.productName = firstNonEmpty(getString(food, "nix_item_name"), getString(food, "food_name"));
        response.product.brands = getString(food, "brand_name");
        response.product.ingredientsText = getString(food, "nf_ingredient_statement");
        response.product.countries = "United States";
        response.product.countriesTags = new String[]{"en:united-states"};
        response.product.servingSize = formatServing(food);
        response.product.imageUrl = extractPhoto(food);
        response.product.categories = "Nutritionix UPC Food";
        response.product.nutriments = mapNutrients(food);
        return response;
    }

    private ProductResponse.NutrimentsData mapNutrients(JsonObject food) {
        ProductResponse.NutrimentsData data = new ProductResponse.NutrimentsData();
        data.energy = getDouble(food, "nf_calories");
        data.fat = getDouble(food, "nf_total_fat");
        data.saturatedFat = getDouble(food, "nf_saturated_fat");
        data.cholesterol = mgToGrams(getDouble(food, "nf_cholesterol"));
        data.sodium = mgToGrams(getDouble(food, "nf_sodium"));
        if (data.sodium != null) data.salt = data.sodium * 2.5;
        data.carbohydrates = getDouble(food, "nf_total_carbohydrate");
        data.fiber = getDouble(food, "nf_dietary_fiber");
        data.sugars = getDouble(food, "nf_sugars");
        data.proteins = getDouble(food, "nf_protein");
        return data;
    }

    private String formatServing(JsonObject food) {
        Double qty = getDouble(food, "serving_qty");
        String unit = getString(food, "serving_unit");
        if (qty == null) return unit;
        if (Math.rint(qty) == qty) {
            return String.format(Locale.US, "%.0f %s", qty, unit).trim();
        }
        return String.format(Locale.US, "%.2f %s", qty, unit).trim();
    }

    private String extractPhoto(JsonObject food) {
        if (!food.has("photo") || !food.get("photo").isJsonObject()) return "";
        return getString(food.getAsJsonObject("photo"), "thumb");
    }

    private Double mgToGrams(Double value) {
        return value == null ? null : value / 1000.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
}
