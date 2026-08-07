package com.ciblorenzo.whatsonmyfood.api;

import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivacySafeRequestDiagnosticsTest {

    @Test
    public void createsSafeCorrelationIdsAndClassifiesExpectedFailures() {
        assertTrue(PrivacySafeRequestDiagnostics.newCorrelationId().matches("[A-Za-z0-9_-]{8,64}"));
        assertEquals("timeout", PrivacySafeRequestDiagnostics.classifyFailure(
                new SocketTimeoutException("private network detail")
        ));
        assertEquals("rate_limit", PrivacySafeRequestDiagnostics.classifyStatus(429));
        assertEquals("provider_unavailable", PrivacySafeRequestDiagnostics.classifyStatus(503));
    }

    @Test
    public void formatsOnlyAllowlistedFieldsAndUsesTheRagRouteTemplate() {
        String line = PrivacySafeRequestDiagnostics.format(
                "android-request-123",
                PrivacySafeRequestDiagnostics.RAG_ROUTE,
                "failure",
                503,
                250,
                "provider_unavailable"
        );

        assertTrue(line.contains("correlation_id=android-request-123"));
        assertTrue(line.contains("route=/api/retail/products/:barcode/ingredients/rag"));
        assertTrue(line.contains("error_category=provider_unavailable"));
        assertFalse(line.contains("prompt"));
        assertFalse(line.contains("token"));
        assertFalse(line.contains("image"));
        assertFalse(line.contains("012345678905"));
    }

    @Test
    public void replacesUnexpectedValuesInsteadOfLoggingThem() {
        String line = PrivacySafeRequestDiagnostics.format(
                "too short",
                "/api/retail/products/012345678905/ingredients/rag",
                "Organic Yogurt",
                500,
                -3,
                "super-secret-token"
        );

        assertTrue(line.contains("correlation_id=invalid-correlation-id"));
        assertTrue(line.contains("route=protected_route"));
        assertTrue(line.contains("outcome=failure"));
        assertTrue(line.contains("error_category=server_error"));
        assertFalse(line.contains("012345678905"));
        assertFalse(line.contains("Organic Yogurt"));
        assertFalse(line.contains("super-secret-token"));
    }
}
