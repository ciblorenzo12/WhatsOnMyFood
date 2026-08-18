package com.ciblorenzo.whatsonmyfood.retail;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MarketplaceStateResolverTest {

    @Test
    public void resultSourceMapsToLiveMockAndEmptyStates() {
        RetailerAvailability item = new RetailerAvailability(
                "Store", "LiveProvider", "Available", "$2.00", "Online", "Shipping",
                "https://example.com", "", true);
        RetailerAlternative alternative = new RetailerAlternative(
                "Alternative", "Brand", "Reason", "Signal", "Available at Store",
                "https://example.com/alternative", "", 80, 2.0, 1.0, "LiveProvider");

        assertEquals(MarketplaceStateResolver.UiState.LIVE,
                MarketplaceStateResolver.fromResult(resultWith(item, alternative, RetailerEndpointResult.SourceMode.LIVE)));
        assertEquals(MarketplaceStateResolver.UiState.LIVE,
                MarketplaceStateResolver.fromResult(resultWith(item, alternative, RetailerEndpointResult.SourceMode.MIXED)));
        assertEquals(MarketplaceStateResolver.UiState.MOCK,
                MarketplaceStateResolver.fromResult(resultWith(item, alternative, RetailerEndpointResult.SourceMode.MOCK)));
        assertEquals(MarketplaceStateResolver.UiState.EMPTY,
                MarketplaceStateResolver.fromResult(new RetailerMarketplaceResult(
                        Collections.emptyList(), Collections.emptyList(), RetailerEndpointResult.SourceMode.EMPTY)));
        assertEquals(MarketplaceStateResolver.UiState.EMPTY,
                MarketplaceStateResolver.fromResult(new RetailerMarketplaceResult(
                        Collections.singletonList(item), Collections.emptyList(), RetailerEndpointResult.SourceMode.LIVE)));
    }

    @Test
    public void timeoutAndBackendFailureRemainDifferentStates() {
        assertEquals(MarketplaceStateResolver.UiState.TIMEOUT,
                MarketplaceStateResolver.fromError(new IOException("request", new SocketTimeoutException("timeout"))));
        assertEquals(MarketplaceStateResolver.UiState.ERROR,
                MarketplaceStateResolver.fromError(new IOException("HTTP 500")));
    }

    private RetailerMarketplaceResult resultWith(RetailerAvailability item,
                                                  RetailerAlternative alternative,
                                                  RetailerEndpointResult.SourceMode sourceMode) {
        return new RetailerMarketplaceResult(
                Collections.singletonList(item), Collections.singletonList(alternative), sourceMode);
    }
}
