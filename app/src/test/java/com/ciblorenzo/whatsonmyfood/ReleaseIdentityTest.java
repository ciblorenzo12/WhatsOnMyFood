package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseIdentityTest {

    @Test
    public void firstBetaUsesTheMilestoneVersionName() {
        assertEquals("1.0.0-beta1", BuildConfig.VERSION_NAME);
    }

    @Test
    public void debugParticipantCandidateHasUnlimitedAiTestingAccess() {
        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
    }
}
