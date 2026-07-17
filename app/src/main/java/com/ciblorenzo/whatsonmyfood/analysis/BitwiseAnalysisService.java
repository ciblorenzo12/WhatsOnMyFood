package com.ciblorenzo.whatsonmyfood.analysis;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;

public class BitwiseAnalysisService {

    static final long ANALYSIS_TIMEOUT_MS = 25_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Call activeCall;
    private Runnable timeoutRunnable;
    private boolean completed;

    public interface AnalysisCallback {
        void onResult(String jsonResult);
        void onError(Throwable error);
    }

    public void analyzeWithRules(String productData, List<String> rules, AnalysisCallback callback) {
        analyzeWithRules(productData, rules, null, callback);
    }

    public synchronized void analyzeWithRules(String productData, List<String> rules, Bitmap bitmap, AnalysisCallback callback) {
        cancelActiveCall();
        completed = false;
        timeoutRunnable = () -> finishWithTimeout(callback);
        mainHandler.postDelayed(timeoutRunnable, ANALYSIS_TIMEOUT_MS);

        activeCall = BitwiseAiCore.startAnalysis(null, productData, bitmap, new BitwiseAiCore.AiCallback() {
            @Override
            public void onResult(String jsonResult) {
                if (markCompleted()) callback.onResult(jsonResult);
            }

            @Override
            public void onError(Throwable error) {
                if (markCompleted()) callback.onError(error);
            }
        });
    }

    public synchronized void cancelActiveCall() {
        completed = true;
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        if (activeCall != null && !activeCall.isCanceled()) activeCall.cancel();
        activeCall = null;
    }

    private synchronized boolean markCompleted() {
        if (completed) return false;
        completed = true;
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        activeCall = null;
        return true;
    }

    private void finishWithTimeout(AnalysisCallback callback) {
        Call callToCancel;
        synchronized (this) {
            if (completed) return;
            completed = true;
            timeoutRunnable = null;
            callToCancel = activeCall;
            activeCall = null;
        }
        if (callToCancel != null) callToCancel.cancel();
        callback.onError(new TimeoutException("Bitwise took too long. Tap to try again."));
    }
}
