package com.ciblorenzo.whatsonmyfood.api;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLException;

/** Shared bounded retry rules for protected AI and RAG requests. */
public final class ResilientRequestPolicy {
    public static final int MAX_TRANSIENT_RETRIES = 1;
    public static final long RETRY_DELAY_MS = 750L;
    static final long FIRST_COLD_START_RETRY_DELAY_MS = 1_500L;
    static final long SECOND_COLD_START_RETRY_DELAY_MS = 5_000L;

    private ResilientRequestPolicy() {
    }

    public static boolean shouldRetryStatus(int statusCode, int attempt) {
        return shouldRetryStatus(statusCode, attempt, MAX_TRANSIENT_RETRIES);
    }

    public static boolean shouldRetryStatus(int statusCode, int attempt, int maxRetries) {
        return attempt < maxRetries && isTransientStatus(statusCode);
    }

    public static boolean shouldRetryStartupBody(String body, int attempt) {
        return shouldRetryStartupBody(body, attempt, MAX_TRANSIENT_RETRIES);
    }

    public static boolean shouldRetryStartupBody(String body, int attempt, int maxRetries) {
        return attempt < maxRetries && looksLikeStartupHtml(body);
    }

    public static boolean shouldRetryResponse(int statusCode, String body, int attempt) {
        return shouldRetryResponse(statusCode, body, attempt, MAX_TRANSIENT_RETRIES);
    }

    public static boolean shouldRetryResponse(int statusCode, String body, int attempt, int maxRetries) {
        if (shouldRetryStatus(statusCode, attempt, maxRetries)) return true;
        boolean successfulOrStartupStatus = (statusCode >= 200 && statusCode < 300)
                || isTransientStatus(statusCode);
        return successfulOrStartupStatus && shouldRetryStartupBody(body, attempt, maxRetries);
    }

    public static boolean shouldRetryFailure(IOException error, int attempt) {
        return shouldRetryFailure(error, attempt, MAX_TRANSIENT_RETRIES);
    }

    public static boolean shouldRetryFailure(IOException error, int attempt, int maxRetries) {
        if (attempt >= maxRetries || error == null) return false;
        if (error instanceof ProtocolException || error instanceof SSLException) return false;
        String message = error.getMessage();
        if (message != null && message.equalsIgnoreCase("Canceled")) return false;
        return error instanceof SocketTimeoutException
                || error instanceof ConnectException
                || error instanceof SocketException
                || error instanceof EOFException;
    }

    public static boolean isTransientStatus(int statusCode) {
        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    public static boolean looksLikeStartupHtml(String body) {
        if (body == null) return false;
        String normalized = body.trim().toLowerCase();
        return normalized.startsWith("<!doctype html")
                || normalized.startsWith("<html")
                || normalized.contains("<title>waiting for service to respond");
    }

    public static void waitBeforeRetry() throws IOException {
        waitFor(RETRY_DELAY_MS);
    }

    public static long coldStartRetryDelayMs(int attempt) {
        return attempt <= 0
                ? FIRST_COLD_START_RETRY_DELAY_MS
                : SECOND_COLD_START_RETRY_DELAY_MS;
    }

    public static void waitBeforeColdStartRetry(int attempt) throws IOException {
        waitFor(coldStartRetryDelayMs(attempt));
    }

    private static void waitFor(long delayMs) throws IOException {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Retry interrupted", error);
        }
    }
}
