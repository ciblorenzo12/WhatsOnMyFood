package com.ciblorenzo.whatsonmyfood.retail;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpRetailerBackendClient implements RetailerBackendClient {
    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;

    public HttpRetailerBackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static boolean isConfigured() {
        return BuildConfig.RETAILER_BACKEND_BASE_URL != null
                && !BuildConfig.RETAILER_BACKEND_BASE_URL.trim().isEmpty();
    }

    @Override
    public List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception {
        JsonArray results = fetchResults(query, "availability");
        List<RetailerAvailability> availability = new ArrayList<>();
        for (JsonElement element : results) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            availability.add(new RetailerAvailability(
                    text(item, "retailerName"),
                    text(item, "providerName"),
                    text(item, "availabilityStatus"),
                    text(item, "price"),
                    text(item, "distance"),
                    text(item, "fulfillment"),
                    text(item, "productUrl"),
                    text(item, "note"),
                    bool(item, "available"),
                    number(item, "priceValue"),
                    number(item, "distanceValue")
            ));
        }
        return availability;
    }

    @Override
    public List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception {
        JsonArray results = fetchResults(query, "alternatives");
        List<RetailerAlternative> alternatives = new ArrayList<>();
        for (JsonElement element : results) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            alternatives.add(new RetailerAlternative(
                    text(item, "productName"),
                    text(item, "brand"),
                    text(item, "reason"),
                    text(item, "healthSignal"),
                    text(item, "retailerHint"),
                    text(item, "productUrl"),
                    text(item, "imageUrl"),
                    (int) number(item, "healthScore"),
                    number(item, "priceValue"),
                    number(item, "distanceValue")
            ));
        }
        return alternatives;
    }

    private JsonArray fetchResults(RetailerProductQuery query, String endpoint) throws Exception {
        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
            throw new IllegalStateException("Invalid retailer backend URL");
        }

        HttpUrl url = base.newBuilder()
                .addPathSegment("api")
                .addPathSegment("retail")
                .addPathSegment("products")
                .addPathSegment(query.barcode)
                .addPathSegment(endpoint)
                .addQueryParameter("productName", query.productName)
                .addQueryParameter("brand", query.brand)
                .addQueryParameter("category", query.category)
                .addQueryParameter("zip", query.zipCode)
                .addQueryParameter("lat", String.valueOf(query.latitude))
                .addQueryParameter("lng", String.valueOf(query.longitude))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("Retailer backend failed with HTTP " + response.code());
            }
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return root.has("results") && root.get("results").isJsonArray()
                    ? root.getAsJsonArray("results")
                    : new JsonArray();
        }
    }

    private String text(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull()) return "";
        return item.get(key).getAsString();
    }

    private boolean bool(JsonObject item, String key) {
        return item.has(key) && !item.get(key).isJsonNull() && item.get(key).getAsBoolean();
    }

    private double number(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull()) return 0.0;
        try {
            return item.get(key).getAsDouble();
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
