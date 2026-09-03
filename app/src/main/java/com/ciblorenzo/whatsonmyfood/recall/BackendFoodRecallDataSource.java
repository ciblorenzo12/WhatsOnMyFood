package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Protected client for FDA recall records retrieved by the application backend. */
public final class BackendFoodRecallDataSource implements FoodRecallDataSource {
    static final String ENDPOINT_PATH = "v1/food-recalls";
    private static final int MAX_ATTEMPTS = 2;
    private final OkHttpClient client;
    private final String backendBaseUrl;
    private final String appToken;

    public BackendFoodRecallDataSource() {
        this(
                new OkHttpClient.Builder()
                        .connectTimeout(4, TimeUnit.SECONDS)
                        .readTimeout(8, TimeUnit.SECONDS)
                        .callTimeout(10, TimeUnit.SECONDS)
                        .build(),
                BuildConfig.RETAILER_BACKEND_BASE_URL,
                BuildConfig.BITWISE_APP_TOKEN
        );
    }

    BackendFoodRecallDataSource(OkHttpClient client, String backendBaseUrl, String appToken) {
        this.client = client;
        this.backendBaseUrl = backendBaseUrl == null ? "" : backendBaseUrl.trim();
        this.appToken = appToken == null ? "" : appToken.trim();
    }

    @Override
    public FoodRecallDataset search(Product product) throws IOException {
        Request request;
        try {
            request = buildRequest(product);
        } catch (IllegalArgumentException | IllegalStateException error) {
            throw new FoodRecallServiceException(error.getMessage(), true);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 404) {
                    return new FoodRecallDataset(Collections.emptyList(), "");
                }
                if (isTransient(response.code()) && attempt + 1 < MAX_ATTEMPTS) {
                    ResilientRequestPolicy.waitBeforeRetry();
                    continue;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    throw new FoodRecallServiceException(
                            "Recall backend returned HTTP " + response.code(),
                            isTransient(response.code())
                    );
                }
                return parseResponse(response.body().string());
            }
        }
        throw new FoodRecallServiceException("Recall service is temporarily unavailable", true);
    }

    Request buildRequest(Product product) {
        return new Request.Builder()
                .url(buildUrl(product))
                .header("Accept", "application/json")
                .header("User-Agent", "WhatsOnMyFood-Android")
                .header("X-APP-TOKEN", appToken)
                .get()
                .build();
    }

    HttpUrl buildUrl(Product product) {
        if (product == null) throw new IllegalArgumentException("Product is required");
        if (backendBaseUrl.isEmpty()) {
            throw new IllegalStateException("Recall backend is not configured");
        }
        HttpUrl base = HttpUrl.parse(backendBaseUrl.endsWith("/")
                ? backendBaseUrl
                : backendBaseUrl + "/");
        if (base == null) throw new IllegalStateException("Recall backend URL is invalid");

        HttpUrl.Builder builder = base.newBuilder()
                .addPathSegments(ENDPOINT_PATH);
        addQueryParameter(builder, "barcode", product.barcode);
        addQueryParameter(builder, "productName", product.productName);
        addQueryParameter(builder, "brand", product.brands);
        return builder.build();
    }

    private static void addQueryParameter(HttpUrl.Builder builder, String name, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.addQueryParameter(name, value.trim());
        }
    }

    static FoodRecallDataset parseResponse(String body) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) throw new IOException("Recall backend returned an invalid response");
            JsonObject root = parsed.getAsJsonObject();
            String lastUpdated = "";
            if (root.has("meta") && root.get("meta").isJsonObject()) {
                lastUpdated = text(root.getAsJsonObject("meta"), "last_updated");
            }
            List<FoodRecallRecord> records = new ArrayList<>();
            JsonArray results = root.has("results") && root.get("results").isJsonArray()
                    ? root.getAsJsonArray("results")
                    : new JsonArray();
            for (JsonElement element : results) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                records.add(new FoodRecallRecord(
                        text(item, "recall_number"),
                        text(item, "product_description"),
                        text(item, "recalling_firm"),
                        text(item, "classification"),
                        text(item, "reason_for_recall"),
                        text(item, "code_info"),
                        text(item, "report_date"),
                        text(item, "status")
                ));
            }
            return new FoodRecallDataset(records, lastUpdated);
        } catch (RuntimeException error) {
            throw new IOException("Recall backend returned malformed JSON", error);
        }
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean isTransient(int statusCode) {
        return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }
}
