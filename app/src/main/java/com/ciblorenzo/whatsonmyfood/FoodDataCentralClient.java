package com.ciblorenzo.whatsonmyfood;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** USDA is a fallback; its provider credential belongs only on our backend. */
public class FoodDataCentralClient implements BarcodeApiClient {
    private static final long MAX_RESPONSE_BYTES = 512 * 1024;
    private static final Gson GSON = new Gson();
    private final OkHttpClient client;
    private final String baseUrl;
    private final String appToken;

    public FoodDataCentralClient() {
        this(defaultHttpClient(), BuildConfig.RETAILER_BACKEND_BASE_URL, BuildConfig.BITWISE_APP_TOKEN);
    }

    FoodDataCentralClient(OkHttpClient client, String baseUrl, String appToken) {
        this.client = client;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.appToken = appToken == null ? "" : appToken.trim();
    }

    static OkHttpClient defaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .callTimeout(18, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    @Override
    public ProductResponse getProduct(String barcode) throws IOException {
        if (baseUrl.isEmpty()) return null;
        Request request = buildRequest(barcode);
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                // Do not expose upstream error bodies, credentials, or request URLs.
                throw new IOException("USDA fallback unavailable (HTTP " + response.code() + ").");
            }
            ResponseBody body = response.body();
            if (body.contentLength() > MAX_RESPONSE_BYTES || body.source().request(MAX_RESPONSE_BYTES + 1)) {
                throw new IOException("USDA fallback response is too large.");
            }
            return parseResponse(body.source().readUtf8(), barcode);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new IOException("USDA fallback returned an invalid response.");
        }
    }

    Request buildRequest(String barcode) throws IOException {
        String normalized = validBarcode(barcode);
        HttpUrl base = HttpUrl.parse(baseUrl);
        if (base == null || !base.username().isEmpty() || !base.password().isEmpty()
                || base.query() != null || base.fragment() != null || appToken.isEmpty()) {
            throw new IOException("USDA fallback backend is not configured.");
        }
        boolean localDebug = BuildConfig.DEBUG && (base.host().equals("localhost")
                || base.host().equals("127.0.0.1") || base.host().equals("10.0.2.2"));
        if (!base.isHttps() && !localDebug) {
            throw new IOException("USDA fallback requires a secure backend connection.");
        }
        HttpUrl url = base.newBuilder()
                .addPathSegments("v1/food-data/usda")
                .addQueryParameter("barcode", normalized)
                .build();
        try {
            return new Request.Builder().url(url)
                    .header("Accept", "application/json")
                    .header("X-APP-TOKEN", appToken)
                    .get().build();
        } catch (IllegalArgumentException e) {
            throw new IOException("USDA fallback backend is not configured.");
        }
    }

    static ProductResponse parseResponse(String json, String barcode) throws IOException {
        String requested = canonicalBarcode(validBarcode(barcode));
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonElement status = root.get("status");
            if (status == null || !status.isJsonPrimitive() || !status.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException();
            }
            if (status.getAsDouble() == 0) return null;
            if (status.getAsDouble() != 1) throw new IllegalArgumentException();
            ProductResponse result = GSON.fromJson(root, ProductResponse.class);
            if (result.product == null || result.product.productName == null
                    || result.product.productName.trim().isEmpty() || result.provenance == null
                    || result.provenance.fdcId <= 0
                    || !requested.equals(canonicalBarcode(validBarcode(result.provenance.gtinUpc)))) {
                throw new IllegalArgumentException();
            }
            JsonElement nutrients = root.getAsJsonObject("product").get("nutriments");
            if (nutrients != null && !nutrients.isJsonNull()) {
                for (Map.Entry<String, JsonElement> entry : nutrients.getAsJsonObject().entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonNull()) continue;
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                            || !Double.isFinite(value.getAsDouble()) || value.getAsDouble() < 0) {
                        throw new IllegalArgumentException();
                    }
                }
            }
            // Never pass per-volume or unknown-basis values into per-100g scoring.
            if (!"per100g".equals(result.provenance.nutrientBasis)) result.product.nutriments = null;
            result.source = "USDA FoodData Central";
            return result;
        } catch (RuntimeException | IOException e) {
            throw new IOException("USDA fallback returned incomplete or invalid product data.");
        }
    }

    private static String validBarcode(String barcode) throws IOException {
        String normalized = BarcodeScanGate.normalizeAndValidate(barcode);
        if (normalized == null || !normalized.matches("[0-9]+")) {
            throw new IOException("A valid product barcode is required for USDA lookup.");
        }
        return normalized;
    }

    private static String canonicalBarcode(String barcode) {
        return ("00000000000000" + barcode).substring(barcode.length());
    }
}
