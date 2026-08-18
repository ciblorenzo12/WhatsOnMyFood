package com.ciblorenzo.whatsonmyfood.retail;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

public final class MarketplaceStateResolver {
    public enum UiState { LOADING, LIVE, MOCK, EMPTY, TIMEOUT, ERROR }

    private MarketplaceStateResolver() {}

    public static UiState fromResult(RetailerMarketplaceResult result) {
        if (result == null || result.isEmpty()) return UiState.EMPTY;
        if (result.sourceMode == RetailerEndpointResult.SourceMode.MOCK) return UiState.MOCK;
        return UiState.LIVE;
    }

    public static UiState fromError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) {
                return UiState.TIMEOUT;
            }
            current = current.getCause();
        }
        return UiState.ERROR;
    }
}
