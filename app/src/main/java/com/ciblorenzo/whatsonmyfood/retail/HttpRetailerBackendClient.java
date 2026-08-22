package com.ciblorenzo.whatsonmyfood.retail;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpRetailerBackendClient implements RetailerBackendClient {
    static final int CALL_TIMEOUT_SECONDS = 8;
    private final OkHttpClient client;
    private final String baseUrl;

    public HttpRetailerBackendClient(String baseUrl) {
        this(baseUrl, new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build());
    }

    HttpRetailerBackendClient(String baseUrl, OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    public static boolean isConfigured() {
        return BuildConfig.RETAILER_BACKEND_BASE_URL != null
                && !BuildConfig.RETAILER_BACKEND_BASE_URL.trim().isEmpty();
    }

    @Override
    public List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception {
        return fetchAvailabilityResult(query).results;
    }

    @Override
    public RetailerEndpointResult<RetailerAvailability> fetchAvailabilityResult(RetailerProductQuery query) throws Exception {
        JsonObject root = fetchResponse(query, "availability");
        JsonArray results = results(root);
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
        return endpointResult(root, availability);
    }

    @Override
    public List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception {
        return fetchAlternativesResult(query).results;
    }

    @Override
    public RetailerEndpointResult<RetailerAlternative> fetchAlternativesResult(RetailerProductQuery query) throws Exception {
        JsonObject root = fetchResponse(query, "alternatives");
        JsonArray results = results(root);
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
                    integerOrUnavailable(item, "healthScore"),
                    number(item, "priceValue"),
                    number(item, "distanceValue"),
                    text(item, "providerName")
            ));
        }
        return endpointResult(root, alternatives);
    }

    HttpUrl buildUrl(RetailerProductQuery query, String endpoint) {
        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
            throw new IllegalStateException("Invalid retailer backend URL");
        }

        return base.newBuilder()
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
    }

    private JsonObject fetchResponse(RetailerProductQuery query, String endpoint) throws Exception {
        HttpUrl url = buildUrl(query, endpoint);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("Retailer backend failed with HTTP " + response.code());
            }
            JsonElement parsed = JsonParser.parseString(response.body().string());
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("Retailer backend returned an invalid response");
            }
            return parsed.getAsJsonObject();
        }
    }

    private JsonArray results(JsonObject root) {
        return root.has("results") && root.get("results").isJsonArray()
                ? root.getAsJsonArray("results")
                : new JsonArray();
    }

    private <T> RetailerEndpointResult<T> endpointResult(JsonObject root, List<T> values) {
        String resultMode = text(root, "resultMode");
        String providerMode = text(root, "providerMode");
        return new RetailerEndpointResult<>(
                values,
                RetailerEndpointResult.parseSourceMode(resultMode, values != null && !values.isEmpty()),
                providerMode);
    }

    private String text(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull()) return "";
        JsonElement value = item.get(key);
        if (!value.isJsonPrimitive()) return "";
        try {
            return value.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean bool(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull() || !item.get(key).isJsonPrimitive()) return false;
        try {
            return item.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private double number(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull()) return 0.0;
        try {
            return item.get(key).getAsDouble();
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private int integerOrUnavailable(JsonObject item, String key) {
        if (!item.has(key) || item.get(key).isJsonNull() || !item.get(key).isJsonPrimitive()) return -1;
        try {
            return item.get(key).getAsInt();
        } catch (Exception ignored) {
            return -1;
        }
    }
}
