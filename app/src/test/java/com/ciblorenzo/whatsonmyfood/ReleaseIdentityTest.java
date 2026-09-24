package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseIdentityTest {

    @Test
    public void candidateUsesTheRequestedRcVersionIdentity() {
        assertEquals("1.30.0-rc1", BuildConfig.VERSION_NAME);
        assertEquals(18, BuildConfig.VERSION_CODE);
    }

    @Test
    public void participantCandidateHasUnlimitedAiTestingAccess() {
        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
    }
}
