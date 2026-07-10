package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BitwiseEntitlementManagerTest {

    @Test
    public void createsAStablePlayBillingAccountIdentifier() {
        String first = BitwiseEntitlementManager.sha256("firebase-user-123");
        String second = BitwiseEntitlementManager.sha256("firebase-user-123");

        assertEquals(64, first.length());
        assertEquals(first, second);
        assertNotEquals(first, BitwiseEntitlementManager.sha256("another-user"));
    }
}
