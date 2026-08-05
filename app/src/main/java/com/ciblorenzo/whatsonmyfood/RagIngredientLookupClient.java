package com.ciblorenzo.whatsonmyfood;

import com.ciblorenzo.whatsonmyfood.api.ResilientRequestPolicy;
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
        String baseUrl = BuildConfig.RETAILER_BACKEND_BASE_URL;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        HttpUrl base = HttpUrl.parse(baseUrl.trim());
        if (base == null) {
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
                .build();

        IOException lastFailure = null;
        for (int attempt = 0; attempt <= ResilientRequestPolicy.MAX_TRANSIENT_RETRIES; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 404) return null;

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
                    return gson.fromJson(responseBody, ProductResponse.class);
                } catch (JsonParseException error) {
                    throw new IOException("RAG ingredient lookup returned invalid JSON", error);
                }
            } catch (IOException error) {
                lastFailure = error;
                if (!ResilientRequestPolicy.shouldRetryFailure(error, attempt)) throw error;
                ResilientRequestPolicy.waitBeforeRetry();
            }
        }
        throw lastFailure != null ? lastFailure : new IOException("RAG ingredient lookup failed");
    }
}
