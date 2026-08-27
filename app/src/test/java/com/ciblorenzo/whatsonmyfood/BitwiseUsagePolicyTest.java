package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BitwiseUsagePolicyTest {

    @Test
    public void participantTestingAccessNeverRunsOutOrRecordsUsage() {
        assertTrue(BitwiseUsagePolicy.isUnlimited(true, false));
        assertTrue(BitwiseUsagePolicy.canUse(true, false, 0));
        assertFalse(BitwiseUsagePolicy.shouldRecordUse(true, false));
    }

    @Test
    public void paidAccessRemainsUnlimitedOutsideTestingBuilds() {
        assertTrue(BitwiseUsagePolicy.isUnlimited(false, true));
        assertTrue(BitwiseUsagePolicy.canUse(false, true, 0));
        assertFalse(BitwiseUsagePolicy.shouldRecordUse(false, true));
    }

    @Test
    public void productionFreeTierStillEnforcesAndRecordsItsLimit() {
        assertFalse(BitwiseUsagePolicy.isUnlimited(false, false));
        assertTrue(BitwiseUsagePolicy.canUse(false, false, 1));
        assertFalse(BitwiseUsagePolicy.canUse(false, false, 0));
        assertTrue(BitwiseUsagePolicy.shouldRecordUse(false, false));
    }
}
