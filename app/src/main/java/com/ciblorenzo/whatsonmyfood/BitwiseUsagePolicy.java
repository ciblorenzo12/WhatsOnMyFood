package com.ciblorenzo.whatsonmyfood;

/** Keeps the participant-test override separate from verified paid entitlement state. */
final class BitwiseUsagePolicy {

    private BitwiseUsagePolicy() {
    }

    static boolean isUnlimited(boolean testingAccess, boolean premiumActive) {
        return testingAccess || premiumActive;
    }

    static boolean canUse(boolean testingAccess, boolean premiumActive, int remainingFreeUses) {
        return isUnlimited(testingAccess, premiumActive) || remainingFreeUses > 0;
    }

    static boolean shouldRecordUse(boolean testingAccess, boolean premiumActive) {
        return !isUnlimited(testingAccess, premiumActive);
    }
}
