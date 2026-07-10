package com.ciblorenzo.whatsonmyfood.api;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BitwiseBackendClient {
    private static final String TAG = "BitwiseBackendClient";
    private static final String APP_TOKEN = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_TRANSIENT_RETRIES = 4;

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LlmCallback {
        void onResult(String text);
        void onError(String message);
    }

    public BitwiseBackendClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public Call askBitwise(String prompt, Bitmap bitmap, LlmCallback callback) {
        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("prompt", prompt);
        if (bitmap != null) {
            JsonObject image = new JsonObject();
            image.addProperty("mimeType", "image/jpeg");
            image.addProperty("data", encodeImage(bitmap));
            bodyJson.add("image", image);
        }

        Request request = new Request.Builder()
                .url(baseUrl() + "v1/bitwise/analyze")
                .header("X-APP-TOKEN", APP_TOKEN)
                .post(RequestBody.create(bodyJson.toString(), JSON))
                .build();

        return enqueueRequest(request, callback, 0);
    }

    private Call enqueueRequest(Request request, LlmCallback callback, int attempt) {
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                postError(callback, "Bitwise could not reach the analysis service. Check your connection and try again.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    if (isTransientStatus(response.code()) && attempt < MAX_TRANSIENT_RETRIES) {
                        long delayMs = 3000L * (attempt + 1);
                        Log.w(TAG, "Bitwise service returned " + response.code() + "; retrying in " + delayMs + " ms");
                        mainHandler.postDelayed(() -> enqueueRequest(request, callback, attempt + 1), delayMs);
                        return;
                    }
                    postError(callback, friendlyErrorMessage(response.code(), responseBody));
                    return;
                }

                if (looksLikeHtml(responseBody)) {
                    if (attempt < MAX_TRANSIENT_RETRIES) {
                        long delayMs = 3000L * (attempt + 1);
                        mainHandler.postDelayed(() -> enqueueRequest(request, callback, attempt + 1), delayMs);
                    } else {
                        postError(callback, "Bitwise is starting up. Please try again in a moment.");
                    }
                    return;
                }

                try {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    if (result != null && result.has("content")) {
                        postResult(callback, result.get("content").getAsString());
                    } else {
                        postError(callback, "Bitwise returned an empty response.");
                    }
                } catch (Exception error) {
                    postError(callback, "Bitwise received an invalid server response. Please try again.");
                }
            }
        });
        return call;
    }

    static boolean looksLikeHtml(String body) {
        if (body == null) return false;
        String normalized = body.trim().toLowerCase();
        return normalized.startsWith("<!doctype html")
                || normalized.startsWith("<html")
                || normalized.contains("<title>waiting for service to respond");
    }

    static String friendlyErrorMessage(int statusCode, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            return "Bitwise authentication failed. Please update the app and try again.";
        }
        if (statusCode == 404) {
            return "Bitwise is not available at the configured server address.";
        }
        if (statusCode == 429) {
            return "Bitwise is busy right now. Please try again shortly.";
        }
        if (isTransientStatus(statusCode) || looksLikeHtml(responseBody)) {
            return "Bitwise is starting up. Please try again in a moment.";
        }
        return "Bitwise is temporarily unavailable. Please try again.";
    }

    private static boolean isTransientStatus(int statusCode) {
        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void postResult(LlmCallback callback, String text) {
        mainHandler.post(() -> callback.onResult(text));
    }

    private void postError(LlmCallback callback, String message) {
        Log.e(TAG, message);
        mainHandler.post(() -> callback.onError(message));
    }

    private String baseUrl() {
        String configured = BuildConfig.BITWISE_LLM_BASE_URL;
        if (configured == null || configured.trim().isEmpty()) {
            configured = "https://x7amycb9govesb-8787.proxy.runpod.net/";
        }
        configured = configured.trim();
        return configured.endsWith("/") ? configured : configured + "/";
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
