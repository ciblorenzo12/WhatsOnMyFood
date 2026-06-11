package com.example.myapplication;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BarcodeLookupClient implements BarcodeApiClient {

    private static final String BASE_URL = "https://api.barcodelookup.com/v3/products";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        if (BuildConfig.BARCODE_LOOKUP_API_KEY == null || BuildConfig.BARCODE_LOOKUP_API_KEY.trim().isEmpty()) {
            return null;
        }

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("barcode", barcode)
                .addQueryParameter("formatted", "y")
                .addQueryParameter("key", BuildConfig.BARCODE_LOOKUP_API_KEY.trim())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Barcode Lookup failed with HTTP " + response.code());
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray products = root.has("products") && root.get("products").isJsonArray()
                    ? root.getAsJsonArray("products")
                    : new JsonArray();
            if (products.size() == 0 || !products.get(0).isJsonObject()) return null;
            return mapProduct(products.get(0).getAsJsonObject(), barcode);
        }
    }

    private ProductResponse mapProduct(JsonObject product, String barcode) {
        ProductResponse response = new ProductResponse();
        response.status = 1;
        response.product = new ProductResponse.ProductData();
        response.product.productName = firstNonEmpty(getString(product, "product_name"), getString(product, "title"));
        response.product.brands = getString(product, "brand");
        response.product.quantity = firstNonEmpty(getString(product, "size"), getString(product, "weight"));
        response.product.categories = firstNonEmpty(getString(product, "category"), getString(product, "description"));
        response.product.imageUrl = firstImage(product);
        if (isLikelyNorthAmericanBarcode(barcode)) {
            response.product.countries = "United States";
            response.product.countriesTags = new String[]{"en:united-states"};
        }
        return response;
    }

    private String firstImage(JsonObject product) {
        if (!product.has("images") || !product.get("images").isJsonArray()) return "";
        JsonArray images = product.getAsJsonArray("images");
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
