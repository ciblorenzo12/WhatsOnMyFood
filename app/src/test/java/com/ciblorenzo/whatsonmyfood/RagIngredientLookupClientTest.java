package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RagIngredientLookupClientTest {

    @Test
    public void ragRecoveryUsesShortExplicitAttemptTimeouts() {
        assertEquals(5, RagIngredientLookupClient.CONNECT_TIMEOUT_SECONDS);
        assertEquals(8, RagIngredientLookupClient.READ_TIMEOUT_SECONDS);
        assertEquals(10, RagIngredientLookupClient.CALL_TIMEOUT_SECONDS);
    }
}
