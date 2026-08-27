package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ParticipantTestingAccessTest {

    @Test
    public void debugParticipantBuildAllowsRepeatedAiUsesWithoutCountingThem() {
        Context context = ApplicationProvider.getApplicationContext();
        BitwiseEntitlementManager manager = new BitwiseEntitlementManager(context);
        int usageBefore = manager.getDailyUsage();

        manager.start();
        for (int i = 0; i < 20; i++) {
            assertTrue(manager.canUseBitwise());
            manager.recordBitwiseUse();
        }

        assertTrue(BuildConfig.UNLIMITED_AI_TESTING);
        assertTrue(manager.isTestingAccessEnabled());
        assertTrue(manager.hasUnlimitedBitwiseAccess());
        assertEquals(BitwiseEntitlementManager.AccessState.ACTIVE, manager.getAccessState());
        assertEquals(usageBefore, manager.getDailyUsage());
        manager.end();
    }
}
