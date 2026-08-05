package com.ciblorenzo.whatsonmyfood;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class BitwiseBillingVerificationClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public interface Callback {
        void onResult(VerificationResult result);

        void onError(String message);
    }

    public void verify(String purchaseToken, String accountIdHash, Callback callback) {
        String configuredBaseUrl = baseUrl();
        if (configuredBaseUrl.isEmpty()) {
            postError(callback, "Bitwise is not configured. Add the protected backend URL and rebuild the app.");
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("purchaseToken", purchaseToken);
        body.addProperty("accountIdHash", accountIdHash);
        Request request = new Request.Builder()
                .url(configuredBaseUrl + "v1/billing/google-play/verify")
                .header("X-APP-TOKEN", BuildConfig.BITWISE_APP_TOKEN)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                postError(callback, "Bitwise could not verify this purchase. Check your connection and try again.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String message = response.code() == 503
                            ? "Google Play verification is not configured on the Bitwise server yet."
                            : "Google Play could not verify this purchase. Please try again.";
                    postError(callback, message);
                    return;
                }
                try {
                    VerificationResult result = gson.fromJson(responseBody, VerificationResult.class);
                    if (result == null || result.state == null) {
                        postError(callback, "Bitwise received an invalid purchase verification response.");
                        return;
                    }
                    mainHandler.post(() -> callback.onResult(result));
                } catch (Exception error) {
                    postError(callback, "Bitwise received an invalid purchase verification response.");
                }
            }
        });
    }

    private void postError(Callback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private String baseUrl() {
        String configured = BuildConfig.BITWISE_LLM_BASE_URL;
        if (configured == null || configured.trim().isEmpty()) configured = BuildConfig.RETAILER_BACKEND_BASE_URL;
        if (configured == null || configured.trim().isEmpty()) return "";
        configured = configured.trim();
        return configured.endsWith("/") ? configured : configured + "/";
    }

    public static final class VerificationResult {
        public boolean verified;
        public boolean entitlementActive;
        public boolean pending;
        public boolean acknowledgementPending;
        public String state;
        public String expiryTime;
    }
}
