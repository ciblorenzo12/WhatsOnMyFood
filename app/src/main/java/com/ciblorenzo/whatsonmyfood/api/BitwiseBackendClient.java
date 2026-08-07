package com.ciblorenzo.whatsonmyfood.api;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.ciblorenzo.whatsonmyfood.analysis.IngredientTextParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BitwiseBackendClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // One retry fits within the total call window and avoids long retry loops.
    static final int MAX_TRANSIENT_RETRIES = ResilientRequestPolicy.MAX_TRANSIENT_RETRIES;
    static final int ANALYSIS_CONNECT_TIMEOUT_SECONDS = 10;
    static final int ANALYSIS_READ_TIMEOUT_SECONDS = 20;
    static final int ANALYSIS_WRITE_TIMEOUT_SECONDS = 15;
    static final int ANALYSIS_CALL_TIMEOUT_SECONDS = 45;
    private static final Pattern SOURCE_STATUS_PATTERN = Pattern.compile(
            "(?im)^\\s*(?:source_status|source status)\\s*:\\s*(.+?)\\s*$"
    );
    private static final Pattern DETECTED_LABEL_PATTERN = Pattern.compile(
            "(?is)detected_ingredient_label\\s*:\\s*(.*?)(?=\\n\\s*(?:contains_allergens|may_contain_allergens|product_ocr_text|ocr_text)\\s*:|$)"
    );
    private static final Pattern INGREDIENTS_LINE_PATTERN = Pattern.compile(
            "(?im)^\\s*ingredients\\s*:\\s*(.+?)\\s*$"
    );

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LlmCallback {
        void onResult(String text);
        void onError(String message);
    }

    public BitwiseBackendClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(ANALYSIS_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ANALYSIS_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ANALYSIS_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(ANALYSIS_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(new ControlledRetryInterceptor())
                .build();
    }

    public Call askBitwise(String prompt, Bitmap bitmap, LlmCallback callback) {
        return askBitwise(prompt, "", Collections.emptyList(), bitmap, callback);
    }

    public Call askBitwise(
            String prompt,
            String productContext,
            List<String> rules,
            Bitmap bitmap,
            LlmCallback callback
    ) {
        String correlationId = PrivacySafeRequestDiagnostics.newCorrelationId();
        long startedNanos = PrivacySafeRequestDiagnostics.startNanos();
        String configuredBaseUrl = configuredBaseUrl(
                BuildConfig.BITWISE_LLM_BASE_URL,
                BuildConfig.RETAILER_BACKEND_BASE_URL
        );
        if (configuredBaseUrl.isEmpty()) {
            PrivacySafeRequestDiagnostics.log(
                    correlationId,
                    PrivacySafeRequestDiagnostics.AI_ROUTE,
                    "failure",
                    0,
                    startedNanos,
                    "configuration"
            );
            postError(callback, "Bitwise is not configured. Add the protected backend URL and rebuild the app.");
            return null;
        }

        JsonObject bodyJson = buildRequestBody(prompt, productContext, rules);
        if (bitmap != null) {
            JsonObject image = new JsonObject();
            image.addProperty("mimeType", "image/jpeg");
            image.addProperty("data", encodeImage(bitmap));
            bodyJson.add("image", image);
        }

        Request request = new Request.Builder()
                .url(configuredBaseUrl + "v1/bitwise/analyze")
                .header("X-APP-TOKEN", BuildConfig.BITWISE_APP_TOKEN)
                .header(PrivacySafeRequestDiagnostics.CORRELATION_HEADER, correlationId)
                .post(RequestBody.create(bodyJson.toString(), JSON))
                .build();

        return enqueueRequest(request, callback, correlationId, startedNanos);
    }

    static JsonObject buildRequestBody(String prompt, String productContext, List<String> rules) {
        JsonObject body = new JsonObject();
        body.addProperty("requestVersion", 1);
        body.addProperty("prompt", prompt == null ? "" : prompt.trim());

        JsonObject structuredProduct = new JsonObject();
        String rawContext = productContext == null ? "" : productContext.trim();
        structuredProduct.addProperty("raw", rawContext);
        JsonArray normalizedIngredients = normalizedIngredients(rawContext);
        structuredProduct.add("normalizedIngredients", normalizedIngredients);
        String sourceStatus = sourceStatus(rawContext, normalizedIngredients.size());
        structuredProduct.addProperty("sourceStatus", sourceStatus);
        structuredProduct.addProperty("uncertainty", uncertainty(sourceStatus, normalizedIngredients.size()));
        body.add("productContext", structuredProduct);

        JsonArray structuredRules = new JsonArray();
        if (rules != null) {
            for (String rule : rules) {
                if (rule != null && !rule.trim().isEmpty()) structuredRules.add(rule.trim());
            }
        }
        body.add("rules", structuredRules);
        return body;
    }

    static JsonArray normalizedIngredients(String productContext) {
        JsonArray result = new JsonArray();
        if (productContext == null || productContext.trim().isEmpty()) return result;

        String ingredientText = firstMatch(DETECTED_LABEL_PATTERN, productContext);
        if (ingredientText.isEmpty()) ingredientText = firstMatch(INGREDIENTS_LINE_PATTERN, productContext);
        for (String ingredient : IngredientTextParser.parseIngredientCandidates(ingredientText)) {
            if (ingredient != null && !ingredient.trim().isEmpty()) result.add(ingredient.trim());
        }
        return result;
    }

    static String sourceStatus(String productContext, int ingredientCount) {
        String explicit = firstMatch(SOURCE_STATUS_PATTERN, productContext);
        if (!explicit.isEmpty()) return explicit;
        if (!firstMatch(DETECTED_LABEL_PATTERN, productContext).isEmpty()) return "scanned_ingredient_label";
        return ingredientCount > 0 ? "product_record_unspecified" : "unknown";
    }

    static String uncertainty(String sourceStatus, int ingredientCount) {
        String normalized = sourceStatus == null ? "" : sourceStatus.toLowerCase(java.util.Locale.US);
        if (ingredientCount == 0 || normalized.contains("unknown") || normalized.contains("outdated")) {
            return "Ingredient evidence is missing or uncertain. Preserve deterministic findings, avoid a confident verdict, and explain the limitation.";
        }
        if (normalized.contains("recover") || normalized.contains("fallback") || normalized.contains("supporting")) {
            return "Ingredients came from a recovery or fallback source. Attribute that source and use cautious wording for claims not directly visible on the label.";
        }
        return "Keep every claim limited to the supplied product data, normalized ingredients, deterministic findings, and verified sources.";
    }

    private static String firstMatch(Pattern pattern, String value) {
        if (value == null) return "";
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    static String configuredBaseUrl(String bitwiseUrl, String retailerUrl) {
        String configured = bitwiseUrl == null ? "" : bitwiseUrl.trim();
        if (configured.isEmpty()) configured = retailerUrl == null ? "" : retailerUrl.trim();
        if (configured.isEmpty()) return "";
        return configured.endsWith("/") ? configured : configured + "/";
    }

    private Call enqueueRequest(
            Request request,
            LlmCallback callback,
            String correlationId,
            long startedNanos
    ) {
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                PrivacySafeRequestDiagnostics.log(
                        correlationId,
                        PrivacySafeRequestDiagnostics.AI_ROUTE,
                        "failure",
                        0,
                        startedNanos,
                        PrivacySafeRequestDiagnostics.classifyFailure(error)
                );
                postError(callback, friendlyFailureMessage(error));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String category = PrivacySafeRequestDiagnostics.classifyStatus(response.code());
                    PrivacySafeRequestDiagnostics.log(
                            correlationId,
                            PrivacySafeRequestDiagnostics.AI_ROUTE,
                            response.code() == 429 ? "rate_limited" : "failure",
                            response.code(),
                            startedNanos,
                            category
                    );
                    postError(callback, friendlyErrorMessage(
                            response.code(),
                            responseBody,
                            response.header("Retry-After")
                    ));
                    return;
                }

                if (looksLikeHtml(responseBody)) {
                    PrivacySafeRequestDiagnostics.log(
                            correlationId,
                            PrivacySafeRequestDiagnostics.AI_ROUTE,
                            "failure",
                            response.code(),
                            startedNanos,
                            "invalid_response"
                    );
                    postError(callback, "Bitwise is starting up. Please try again in a moment.");
                    return;
                }

                try {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    if (result != null && result.has("content")) {
                        boolean fallback = result.has("provider")
                                && "local-fallback".equals(result.get("provider").getAsString());
                        PrivacySafeRequestDiagnostics.log(
                                correlationId,
                                PrivacySafeRequestDiagnostics.AI_ROUTE,
                                fallback ? "fallback_success" : "success",
                                response.code(),
                                startedNanos,
                                fallback ? "provider_unavailable" : "none"
                        );
                        postResult(callback, result.get("content").getAsString());
                    } else {
                        PrivacySafeRequestDiagnostics.log(
                                correlationId,
                                PrivacySafeRequestDiagnostics.AI_ROUTE,
                                "failure",
                                response.code(),
                                startedNanos,
                                "invalid_response"
                        );
                        postError(callback, "Bitwise returned an empty response.");
                    }
                } catch (Exception error) {
                    PrivacySafeRequestDiagnostics.log(
                            correlationId,
                            PrivacySafeRequestDiagnostics.AI_ROUTE,
                            "failure",
                            response.code(),
                            startedNanos,
                            "invalid_response"
                    );
                    postError(callback, "Bitwise received an invalid server response. Please try again.");
                }
            }
        });
        return call;
    }

    static boolean looksLikeHtml(String body) {
        return ResilientRequestPolicy.looksLikeStartupHtml(body);
    }

    static String friendlyErrorMessage(int statusCode, String responseBody) {
        return friendlyErrorMessage(statusCode, responseBody, "");
    }

    static String friendlyErrorMessage(int statusCode, String responseBody, String retryAfter) {
        if (statusCode == 401 || statusCode == 403) {
            return "Bitwise authentication failed. Please update the app and try again.";
        }
        if (statusCode == 404) {
            return "Bitwise is not available at the configured server address.";
        }
        if (statusCode == 429) {
            String wait = retryAfter == null ? "" : retryAfter.trim();
            return wait.matches("\\d+")
                    ? "Bitwise has reached its request limit. Please try again in " + wait + " seconds."
                    : "Bitwise has reached its request limit. Please try again shortly.";
        }
        if (ResilientRequestPolicy.isTransientStatus(statusCode) || looksLikeHtml(responseBody)) {
            return "Bitwise is starting up. Please try again in a moment.";
        }
        return "Bitwise is temporarily unavailable. Please try again.";
    }

    static String friendlyFailureMessage(IOException error) {
        if (error instanceof InterruptedIOException) {
            return "Bitwise took too long to respond. Please try again.";
        }
        return "Bitwise could not reach the analysis service. Check your connection and try again.";
    }

    private static final class ControlledRetryInterceptor implements Interceptor {
        private static final long STARTUP_PEEK_BYTES = 16 * 1024L;

        @Override
        public Response intercept(Chain chain) throws IOException {
            IOException lastFailure = null;
            for (int attempt = 0; attempt <= MAX_TRANSIENT_RETRIES; attempt++) {
                try {
                    Response response = chain.proceed(chain.request());
                    String preview = response.peekBody(STARTUP_PEEK_BYTES).string();
                    boolean retry = ResilientRequestPolicy.shouldRetryResponse(
                            response.code(),
                            preview,
                            attempt
                    );
                    if (!retry) return response;

                    response.close();
                    ResilientRequestPolicy.waitBeforeRetry();
                } catch (IOException error) {
                    lastFailure = error;
                    if (!ResilientRequestPolicy.shouldRetryFailure(error, attempt)) throw error;
                    ResilientRequestPolicy.waitBeforeRetry();
                }
            }
            throw lastFailure != null ? lastFailure : new IOException("Protected analysis retry failed");
        }
    }

    private void postResult(LlmCallback callback, String text) {
        mainHandler.post(() -> callback.onResult(text));
    }

    private void postError(LlmCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private String encodeImage(Bitmap bitmap) {
        int maxWidth = 960;
        int maxHeight = 960;
        float ratio = Math.min(1f, Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight()));
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP);
    }
}
