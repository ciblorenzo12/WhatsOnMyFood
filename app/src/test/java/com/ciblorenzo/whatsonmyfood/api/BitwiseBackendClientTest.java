package com.ciblorenzo.whatsonmyfood.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitwiseBackendClientTest {

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
}
