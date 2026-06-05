package com.example.myapplication.api;

import android.graphics.Bitmap;
import okhttp3.Call;

public class SecureAiService {

    private static final RunpodLlmClient llmClient = new RunpodLlmClient();

    public interface AiCallback {
        void onResult(String result);
        void onError(String error);
    }

    public static Call analyzeProduct(String prompt, Bitmap bitmap, AiCallback callback) {
        return llmClient.askBitwise(prompt, bitmap, new RunpodLlmClient.LlmCallback() {
            @Override
            public void onResult(String text) {
                callback.onResult(text);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
