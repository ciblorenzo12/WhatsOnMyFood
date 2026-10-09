package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseIdentityTest {

    @Test
    public void candidateUsesTheRequestedBetaVersionIdentity() {
        assertEquals("2.0.0-beta", BuildConfig.VERSION_NAME);
        assertEquals(21, BuildConfig.VERSION_CODE);
    }

    @Test
    public void participantCandidateHasUnlimitedAiTestingAccess() {
        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
    }
}
