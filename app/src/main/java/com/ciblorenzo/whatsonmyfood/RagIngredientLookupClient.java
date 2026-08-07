package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicy;
import com.ciblorenzo.whatsonmyfood.api.PrivacySafeRequestDiagnostics;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RagIngredientLookupClient {
    static final int CONNECT_TIMEOUT_SECONDS = 5;
    static final int READ_TIMEOUT_SECONDS = 8;
    static final int CALL_TIMEOUT_SECONDS = 10;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
    private final Gson gson = new Gson();

    public ProductResponse getIngredients(String barcode, String productName, String brand) throws IOException {
        String correlationId = PrivacySafeRequestDiagnostics.newCorrelationId();
        long startedNanos = PrivacySafeRequestDiagnostics.startNanos();
        String baseUrl = BuildConfig.RETAILER_BACKEND_BASE_URL;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            PrivacySafeRequestDiagnostics.log(
                    correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE, "failure", 0,
                    startedNanos, "configuration"
            );
            return null;
        }

        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
            PrivacySafeRequestDiagnostics.log(
                    correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE, "failure", 0,
                    startedNanos, "configuration"
            );
            return null;
        }

        HttpUrl.Builder urlBuilder = base.newBuilder()
                .addPathSegment("api")
                .addPathSegment("retail")
                .addPathSegment("products")
                .addPathSegment(barcode)
                .addPathSegment("ingredients")
                .addPathSegment("rag");

        if (productName != null && !productName.trim().isEmpty()) {
            urlBuilder.addQueryParameter("productName", productName.trim());
        }
        if (brand != null && !brand.trim().isEmpty()) {
            urlBuilder.addQueryParameter("brand", brand.trim());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Accept", "application/json")
                .addHeader("X-APP-TOKEN", BuildConfig.BITWISE_APP_TOKEN)
                .addHeader(PrivacySafeRequestDiagnostics.CORRELATION_HEADER, correlationId)
                .build();

        IOException lastFailure = null;
        int lastStatus = 0;
        for (int attempt = 0; attempt <= ResilientRequestPolicy.MAX_TRANSIENT_RETRIES; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                lastStatus = response.code();
                if (response.code() == 404) {
                    PrivacySafeRequestDiagnostics.log(
                            correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE, "empty_result",
                            response.code(), startedNanos, "not_found"
                    );
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                boolean retry = ResilientRequestPolicy.shouldRetryResponse(
                        response.code(),
                        responseBody,
                        attempt
                );
                if (retry) {
                    ResilientRequestPolicy.waitBeforeRetry();
                    continue;
                }
                if (response.code() == 429) {
                    throw new IOException("RAG ingredient lookup is rate limited");
                }
                if (!response.isSuccessful()) {
                    throw new IOException("RAG ingredient lookup is temporarily unavailable");
                }
                if (ResilientRequestPolicy.looksLikeStartupHtml(responseBody)) {
                    throw new IOException("RAG ingredient lookup returned a startup page");
                }
                if (responseBody.trim().isEmpty()) {
                    throw new IOException("RAG ingredient lookup returned an empty response");
                }
                try {
                    ProductResponse result = gson.fromJson(responseBody, ProductResponse.class);
                    PrivacySafeRequestDiagnostics.log(
                            correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE,
                            result != null && result.status == 1 ? "success" : "empty_result",
                            response.code(), startedNanos, "none"
                    );
                    return result;
                } catch (JsonParseException error) {
                    throw new IOException("RAG ingredient lookup returned invalid JSON", error);
                }
            } catch (IOException error) {
                lastFailure = error;
                if (!ResilientRequestPolicy.shouldRetryFailure(error, attempt)) {
                    String category = lastStatus > 0
                            ? PrivacySafeRequestDiagnostics.classifyStatus(lastStatus)
                            : PrivacySafeRequestDiagnostics.classifyFailure(error);
                    if ("none".equals(category)) {
                        category = PrivacySafeRequestDiagnostics.classifyFailure(error);
                    }
                    PrivacySafeRequestDiagnostics.log(
                            correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE,
                            lastStatus == 429 ? "rate_limited" : "failure",
                            lastStatus, startedNanos, category
                    );
                    throw error;
                }
                ResilientRequestPolicy.waitBeforeRetry();
            }
        }
        IOException finalFailure = lastFailure != null
                ? lastFailure
                : new IOException("RAG ingredient lookup failed");
        PrivacySafeRequestDiagnostics.log(
                correlationId, PrivacySafeRequestDiagnostics.RAG_ROUTE, "failure",
                lastStatus, startedNanos, PrivacySafeRequestDiagnostics.classifyFailure(finalFailure)
        );
        throw finalFailure;
    }
}
