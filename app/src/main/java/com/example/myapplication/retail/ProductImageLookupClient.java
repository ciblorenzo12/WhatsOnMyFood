package com.example.myapplication.retail;

import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class ProductImageLookupClient {
    private final OkHttpClient client = new OkHttpClient();

    String findOpenFoodFactsImage(String brand, String productName) {
        String search = ((brand != null ? brand : "") + " " + (productName != null ? productName : "")).trim();
        if (search.isEmpty()) return "";

        String url = "https://world.openfoodfacts.org/cgi/search.pl"
                + "?search_terms=" + Uri.encode(search)
                + "&search_simple=1"
                + "&action=process"
                + "&json=1"
                + "&page_size=1"
                + "&fields=image_front_url,image_url";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "YourHealthyPantry-Android/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return "";
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray products = root.has("products") ? root.getAsJsonArray("products") : null;
            if (products == null || products.size() == 0) return "";
            JsonObject product = products.get(0).getAsJsonObject();
            return firstImage(product, "image_front_url", "image_url");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String firstImage(JsonObject product, String... fields) {
        for (String field : fields) {
            JsonElement value = product.get(field);
            if (value != null && !value.isJsonNull()) {
                String imageUrl = value.getAsString();
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    return imageUrl.trim();
                }
            }
        }
        return "";
    }
}
