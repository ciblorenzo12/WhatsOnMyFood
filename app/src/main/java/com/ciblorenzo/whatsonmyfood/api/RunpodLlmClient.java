package com.ciblorenzo.whatsonmyfood.api;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RunpodLlmClient {
    private static final String TAG = "RunpodLlmClient";
    private static final String APP_TOKEN = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG";

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LlmCallback {
        void onResult(String text);
        void onError(String message);
    }

    public RunpodLlmClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS) // Shorter connect timeout
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public Call askBitwise(String prompt, Bitmap bitmap, LlmCallback callback) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", "gpt-4o-mini");

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        if (bitmap == null) {
            userMessage.addProperty("content", prompt);
        } else {
            JsonArray contentArray = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", prompt);
            contentArray.add(textPart);

            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", "data:image/jpeg;base64," + encodeImage(bitmap));
            imageUrl.addProperty("detail", "high");
            imagePart.add("image_url", imageUrl);
            contentArray.add(imagePart);

            userMessage.add("content", contentArray);
        }
        messages.add(userMessage);

        jsonBody.add("messages", messages);
        jsonBody.addProperty("temperature", 0.1);
        jsonBody.addProperty("top_p", 0.9);
        jsonBody.addProperty("max_tokens", 850);
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        jsonBody.add("response_format", responseFormat);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl() + "v1/chat/completions")
                .header("X-APP-TOKEN", APP_TOKEN)
                .post(body)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "Connection Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    postError(callback, "Server Error " + response.code() + ": " + responseBody);
                    return;
                }

                try {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        String content = choices.get(0).getAsJsonObject()
                                .get("message").getAsJsonObject()
                                .get("content").getAsString();
                        postResult(callback, content);
                    } else {
                        postError(callback, "AI returned an empty list of choices.");
                    }
                } catch (Exception e) {
                    postError(callback, "Parsing Error: " + e.getMessage());
                }
            }
        });
        return call;
    }

    private void postResult(LlmCallback callback, String text) {
        mainHandler.post(() -> callback.onResult(text));
    }

    private void postError(LlmCallback callback, String msg) {
        Log.e(TAG, msg);
        mainHandler.post(() -> callback.onError(msg));
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
        // 1. Scale down image to reduce payload size and speed up upload/processing
        int maxWidth = 960;
        int maxHeight = 960;
        float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
        int width = Math.round(ratio * bitmap.getWidth());
        int height = Math.round(ratio * bitmap.getHeight());

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
        byte[] bytes = outputStream.toByteArray();
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }
}
