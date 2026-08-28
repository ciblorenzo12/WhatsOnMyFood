package com.ciblorenzo.whatsonmyfood.api;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResilientRequestPolicyTest {

    @Test
    public void retries502503And504OnlyOnce() {
        assertTrue(ResilientRequestPolicy.shouldRetryStatus(502, 0));
        assertTrue(ResilientRequestPolicy.shouldRetryStatus(503, 0));
        assertTrue(ResilientRequestPolicy.shouldRetryStatus(504, 0));

        assertFalse(ResilientRequestPolicy.shouldRetryStatus(502, 1));
        assertFalse(ResilientRequestPolicy.shouldRetryStatus(429, 0));
        assertFalse(ResilientRequestPolicy.shouldRetryStatus(500, 0));
        assertFalse(ResilientRequestPolicy.shouldRetryStatus(400, 0));
    }

    @Test
    public void retriesTimeoutAndConnectionFailureButNotProtocolOrTlsErrors() {
        assertTrue(ResilientRequestPolicy.shouldRetryFailure(new SocketTimeoutException("timeout"), 0));
        assertTrue(ResilientRequestPolicy.shouldRetryFailure(new ConnectException("refused"), 0));

        assertFalse(ResilientRequestPolicy.shouldRetryFailure(new SocketTimeoutException("timeout"), 1));
        assertFalse(ResilientRequestPolicy.shouldRetryFailure(new ProtocolException("bad protocol"), 0));
        assertFalse(ResilientRequestPolicy.shouldRetryFailure(new SSLHandshakeException("bad certificate"), 0));
        assertFalse(ResilientRequestPolicy.shouldRetryFailure(new IOException("invalid response"), 0));
    }

    @Test
    public void retriesHtmlStartupPageOnceAndRejectsInvalidContent() {
        String startup = "<!DOCTYPE html><title>Waiting for service to respond - RunPod</title>";
        assertTrue(ResilientRequestPolicy.shouldRetryResponse(200, startup, 0));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(200, startup, 1));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(429, startup, 0));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(404, startup, 0));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(200, "not valid JSON", 0));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(200, "{\"content\":\"ok\"}", 0));
    }

    @Test
    public void bitwiseColdStartBudgetAllowsTwoRetriesWithoutChangingSharedDefault() {
        String startup = "<!DOCTYPE html><title>Waiting for service to respond - RunPod</title>";

        assertTrue(ResilientRequestPolicy.shouldRetryResponse(503, startup, 0, 2));
        assertTrue(ResilientRequestPolicy.shouldRetryResponse(503, startup, 1, 2));
        assertFalse(ResilientRequestPolicy.shouldRetryResponse(503, startup, 2, 2));
        assertTrue(ResilientRequestPolicy.shouldRetryFailure(new SocketTimeoutException("timeout"), 1, 2));
        assertFalse(ResilientRequestPolicy.shouldRetryFailure(new SocketTimeoutException("timeout"), 2, 2));

        assertFalse(ResilientRequestPolicy.shouldRetryStatus(503, 1));
    }

    @Test
    public void coldStartBackoffGivesTheHostedWorkerTimeToWake() {
        assertEquals(1_500L, ResilientRequestPolicy.coldStartRetryDelayMs(0));
        assertEquals(5_000L, ResilientRequestPolicy.coldStartRetryDelayMs(1));
    }
}
