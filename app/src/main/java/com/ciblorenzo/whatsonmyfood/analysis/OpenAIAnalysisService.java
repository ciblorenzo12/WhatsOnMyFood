package com.ciblorenzo.whatsonmyfood.analysis;

import android.graphics.Bitmap;
import okhttp3.Call;
import java.util.List;

public class OpenAIAnalysisService {

    public interface AnalysisCallback {
        void onResult(String jsonResult);
        void onError(Throwable t);
    }

    private Call activeCall;

    public void analyzeWithRules(String productData, List<String> rules, AnalysisCallback callback) {
        analyzeWithRules(productData, rules, null, callback);
    }

    public void analyzeWithRules(String productData, List<String> rules, Bitmap bitmap, AnalysisCallback callback) {
        BitwiseAiCore.startAnalysis(null, productData, bitmap, new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String jsonResult) {
                callback.onResult(jsonResult);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void cancelActiveCall() {
    }
}
