package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseIdentityTest {

    @Test
    public void milestoneSevenCandidateUsesTheRequestedRcVersionName() {
        assertEquals("1.11.0", BuildConfig.VERSION_NAME);
    }

    @Test
    public void debugParticipantCandidateHasUnlimitedAiTestingAccess() {
        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
    }
}
