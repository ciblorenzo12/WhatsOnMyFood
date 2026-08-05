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

    private ResilientRequestPolicy() {
    }

    public static boolean shouldRetryStatus(int statusCode, int attempt) {
        return attempt < MAX_TRANSIENT_RETRIES && isTransientStatus(statusCode);
    }

    public static boolean shouldRetryStartupBody(String body, int attempt) {
        return attempt < MAX_TRANSIENT_RETRIES && looksLikeStartupHtml(body);
    }

    public static boolean shouldRetryResponse(int statusCode, String body, int attempt) {
        if (shouldRetryStatus(statusCode, attempt)) return true;
        boolean successfulOrStartupStatus = (statusCode >= 200 && statusCode < 300)
                || isTransientStatus(statusCode);
        return successfulOrStartupStatus && shouldRetryStartupBody(body, attempt);
    }

    public static boolean shouldRetryFailure(IOException error, int attempt) {
        if (attempt >= MAX_TRANSIENT_RETRIES || error == null) return false;
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
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Retry interrupted", error);
        }
    }
}
