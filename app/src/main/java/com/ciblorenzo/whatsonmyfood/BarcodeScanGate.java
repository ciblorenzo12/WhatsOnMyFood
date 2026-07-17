package com.ciblorenzo.whatsonmyfood;

/**
 * Validates GTIN values and permits only one product launch per scanner session.
 * The gate is deliberately independent of Android so its duplicate behavior can be unit tested.
 */
public final class BarcodeScanGate {

    private boolean locked;

    /**
     * Returns the normalized GTIN when this event wins the gate, or {@code null} when the
     * value is invalid or another event already won this scanner session.
     */
    public synchronized String tryAcquire(String rawValue) {
        if (locked) return null;
        String normalized = normalizeAndValidate(rawValue);
        if (normalized == null) return null;
        locked = true;
        return normalized;
    }

    public synchronized void reset() {
        locked = false;
    }

    public synchronized boolean isLocked() {
        return locked;
    }

    /** Accepts GTIN-8, UPC-A, EAN-13, and GTIN-14 values with a valid check digit. */
    public static String normalizeAndValidate(String rawValue) {
        if (rawValue == null) return null;
        StringBuilder digits = new StringBuilder(rawValue.length());
        for (int i = 0; i < rawValue.length(); i++) {
            char c = rawValue.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (!Character.isWhitespace(c) && c != '-') {
                return null;
            }
        }

        int length = digits.length();
        if (length != 8 && length != 12 && length != 13 && length != 14) return null;
        return hasValidCheckDigit(digits) ? digits.toString() : null;
    }

    private static boolean hasValidCheckDigit(CharSequence digits) {
        int sum = 0;
        boolean weightThree = true;
        for (int i = digits.length() - 2; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            sum += digit * (weightThree ? 3 : 1);
            weightThree = !weightThree;
        }
        int expectedCheckDigit = (10 - (sum % 10)) % 10;
        return expectedCheckDigit == digits.charAt(digits.length() - 1) - '0';
    }
}
