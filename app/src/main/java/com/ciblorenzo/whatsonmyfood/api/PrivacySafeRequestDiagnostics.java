package com.ciblorenzo.whatsonmyfood.api;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Locale;
import java.util.UUID;

public final class PrivacySafeRequestDiagnostics {
    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String AI_ROUTE = "/v1/bitwise/analyze";
    public static final String RAG_ROUTE = "/api/retail/products/:barcode/ingredients/rag";
    private static final String TAG = "PrivacySafeRequest";

    private PrivacySafeRequestDiagnostics() {
    }

    public static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public static long startNanos() {
        return System.nanoTime();
    }

    public static long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    public static String classifyStatus(int status) {
        if (status == 429) return "rate_limit";
        if (status == 401 || status == 403) return "authentication";
        if (status == 404) return "not_found";
        if (status == 400 || status == 413 || status == 422) return "invalid_request";
        if (status == 502 || status == 503 || status == 504) return "provider_unavailable";
        if (status >= 500) return "server_error";
        return "none";
    }

    public static String classifyFailure(IOException error) {
        if (error instanceof InterruptedIOException) return "timeout";
        String message = error == null ? "" : String.valueOf(error.getMessage()).toLowerCase(Locale.US);
        if (message.contains("rate limit")) return "rate_limit";
        if (message.contains("invalid json") || message.contains("startup page") || message.contains("empty response")) {
            return "invalid_response";
        }
        return "provider_unavailable";
    }

    public static String format(
            String correlationId,
            String route,
            String outcome,
            int status,
            long latencyMs,
            String errorCategory
    ) {
        return String.format(
                Locale.US,
                "request_diagnostic correlation_id=%s route=%s outcome=%s status=%d latency_ms=%d error_category=%s",
                safeCorrelationId(correlationId),
                safeRoute(route),
                safeOutcome(outcome),
                Math.max(status, 0),
                Math.max(latencyMs, 0L),
                safeCategory(errorCategory)
        );
    }

    public static void log(
            String correlationId,
            String route,
            String outcome,
            int status,
            long startedNanos,
            String errorCategory
    ) {
        String line = format(
                correlationId,
                route,
                outcome,
                status,
                elapsedMillis(startedNanos),
                errorCategory
        );
        if ("success".equals(outcome) || "empty_result".equals(outcome)) {
            Log.i(TAG, line);
        } else {
            Log.w(TAG, line);
        }
    }

    private static String safeCorrelationId(String value) {
        String candidate = value == null ? "" : value.trim();
        return candidate.matches("[A-Za-z0-9_-]{8,64}") ? candidate : "invalid-correlation-id";
    }

    private static String safeRoute(String route) {
        if (AI_ROUTE.equals(route)) return AI_ROUTE;
        if (RAG_ROUTE.equals(route)) return RAG_ROUTE;
        return "protected_route";
    }

    private static String safeOutcome(String outcome) {
        if ("success".equals(outcome)
                || "empty_result".equals(outcome)
                || "fallback_success".equals(outcome)
                || "rate_limited".equals(outcome)) {
            return outcome;
        }
        return "failure";
    }

    private static String safeCategory(String category) {
        if ("none".equals(category)
                || "authentication".equals(category)
                || "configuration".equals(category)
                || "invalid_request".equals(category)
                || "invalid_response".equals(category)
                || "not_found".equals(category)
                || "provider_unavailable".equals(category)
                || "rate_limit".equals(category)
                || "server_error".equals(category)
                || "timeout".equals(category)) {
            return category;
        }
        return "server_error";
    }
}
