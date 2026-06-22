package com.ciblorenzo.whatsonmyfood;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpcItemDbClient implements BarcodeApiClient {

    private static final String FREE_URL = "https://api.upcitemdb.com/prod/trial/lookup";
    private static final String PAID_URL = "https://api.upcitemdb.com/prod/v1/lookup";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        boolean hasKey = BuildConfig.UPCITEMDB_USER_KEY != null && !BuildConfig.UPCITEMDB_USER_KEY.trim().isEmpty();
        HttpUrl url = HttpUrl.parse(hasKey ? PAID_URL : FREE_URL).newBuilder()
                .addQueryParameter("upc", barcode)
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json");
        if (hasKey) {
            requestBuilder.addHeader("user_key", BuildConfig.UPCITEMDB_USER_KEY.trim());
            requestBuilder.addHeader("key_type", "3scale");
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("UPCitemdb lookup failed with HTTP " + response.code());
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray items = root.has("items") && root.get("items").isJsonArray()
                    ? root.getAsJsonArray("items")
                    : new JsonArray();
            if (items.size() == 0 || !items.get(0).isJsonObject()) return null;
            return mapItem(items.get(0).getAsJsonObject(), barcode);
        }
    }

    private ProductResponse mapItem(JsonObject item, String barcode) {
        ProductResponse response = new ProductResponse();
        response.status = 1;
        response.product = new ProductResponse.ProductData();
        response.product.productName = getString(item, "title");
        response.product.brands = getString(item, "brand");
        response.product.quantity = firstNonEmpty(getString(item, "size"), getString(item, "weight"));
        response.product.categories = getString(item, "category");
        response.product.imageUrl = firstImage(item);
        if (isLikelyNorthAmericanBarcode(barcode)) {
            response.product.countries = "United States";
            response.product.countriesTags = new String[]{"en:united-states"};
        }
        return response;
    }

    private String firstImage(JsonObject item) {
        if (!item.has("images") || !item.get("images").isJsonArray()) return "";
        JsonArray images = item.getAsJsonArray("images");
        for (JsonElement image : images) {
            if (image != null && !image.isJsonNull()) {
                String value = image.getAsString();
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        }
        return "";
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsString().trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private boolean isLikelyNorthAmericanBarcode(String barcode) {
        String digits = barcode == null ? "" : barcode.replaceAll("[^0-9]", "");
        return digits.length() == 12 || (digits.length() == 13 && digits.startsWith("0"));
    }
}
