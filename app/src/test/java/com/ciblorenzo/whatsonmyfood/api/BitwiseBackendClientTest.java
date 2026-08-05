package com.ciblorenzo.whatsonmyfood.api;

import org.junit.Test;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitwiseBackendClientTest {

    @Test
    public void interactiveAnalysisUsesOneBoundedRetryWindow() {
        assertEquals(1, BitwiseBackendClient.MAX_TRANSIENT_RETRIES);
        assertEquals(10, BitwiseBackendClient.ANALYSIS_CONNECT_TIMEOUT_SECONDS);
        assertEquals(20, BitwiseBackendClient.ANALYSIS_READ_TIMEOUT_SECONDS);
        assertEquals(15, BitwiseBackendClient.ANALYSIS_WRITE_TIMEOUT_SECONDS);
        assertEquals(45, BitwiseBackendClient.ANALYSIS_CALL_TIMEOUT_SECONDS);
    }

    @Test
    public void detectsRunPodHtmlResponse() {
        String body = "<!DOCTYPE html><html><head><title>Waiting for service to respond - RunPod</title></head></html>";

        assertTrue(BitwiseBackendClient.looksLikeHtml(body));
        assertFalse(BitwiseBackendClient.looksLikeHtml("{\"content\":\"ok\"}"));
    }

    @Test
    public void hidesRawServerBodyFromUserFacingErrors() {
        String body = "<!DOCTYPE html><style>body{background:black}</style>";

        assertEquals(
                "Bitwise is starting up. Please try again in a moment.",
                BitwiseBackendClient.friendlyErrorMessage(502, body)
        );
    }

    @Test
    public void handlesRateLimitWithoutRetryLoopWording() {
        assertEquals(
                "Bitwise has reached its request limit. Please try again in 30 seconds.",
                BitwiseBackendClient.friendlyErrorMessage(429, "raw server detail", "30")
        );
        assertFalse(ResilientRequestPolicy.shouldRetryStatus(429, 0));
    }

    @Test
    public void reportsTimeoutWithoutExposingNetworkDetails() {
        assertEquals(
                "Bitwise took too long to respond. Please try again.",
                BitwiseBackendClient.friendlyFailureMessage(new SocketTimeoutException("socket detail"))
        );
    }

    @Test
    public void buildsStructuredProductAndRuleRequest() {
        JsonObject body = BitwiseBackendClient.buildRequestBody(
                "Analyze this product",
                "Name: Oat cereal\nIngredients: oats, sugar, salt",
                Arrays.asList("Flag added sugar", "Preserve deterministic findings")
        );

        assertEquals(1, body.get("requestVersion").getAsInt());
        assertEquals("Analyze this product", body.get("prompt").getAsString());
        assertTrue(body.getAsJsonObject("productContext").get("raw").getAsString().contains("Oat cereal"));
        assertEquals(2, body.getAsJsonArray("rules").size());
    }

    @Test
    public void requiresConfiguredBackendUrlWithoutSourceCodeFallback() {
        assertEquals("", BitwiseBackendClient.configuredBaseUrl("", ""));
        assertEquals(
                "https://backend.example/",
                BitwiseBackendClient.configuredBaseUrl("https://backend.example", "https://ignored.example")
        );
    }
}
