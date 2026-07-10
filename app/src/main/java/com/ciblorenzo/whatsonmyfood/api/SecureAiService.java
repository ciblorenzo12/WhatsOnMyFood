package com.ciblorenzo.whatsonmyfood.api;

import android.graphics.Bitmap;
import okhttp3.Call;

public class SecureAiService {

    private static final BitwiseBackendClient llmClient = new BitwiseBackendClient();

    public interface AiCallback {
        void onResult(String result);
        void onError(String error);
    }

    public static Call analyzeProduct(String prompt, Bitmap bitmap, AiCallback callback) {
        return llmClient.askBitwise(prompt, bitmap, new BitwiseBackendClient.LlmCallback() {
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
