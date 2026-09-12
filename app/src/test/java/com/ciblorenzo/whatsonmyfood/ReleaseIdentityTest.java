package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseIdentityTest {

    @Test
    public void candidateUsesTheRequestedRcVersionIdentity() {
        assertEquals("1.19-rc-1", BuildConfig.VERSION_NAME);
        assertEquals(15, BuildConfig.VERSION_CODE);
    }

    @Test
    public void debugParticipantCandidateHasUnlimitedAiTestingAccess() {
        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
    }
}
