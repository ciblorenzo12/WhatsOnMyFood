package com.ciblorenzo.whatsonmyfood.analysis.rules;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared interpretation of organic wording; this does not independently verify certification. */
final class OrganicClaim {
    private static final Pattern ORGANIC = Pattern.compile("\\borganic\\b");
    private static final Pattern NEGATED = Pattern.compile(
            "\\b(?:non|not|no)\\s+organic\\b|\\borganic\\s+(?:not\\s+)?(?:unverified|not verified)\\b");

    private OrganicClaim() {}

    static boolean isOrganic(String text) {
        if (text == null) return false;
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Pd}_]+", " ").replaceAll("\\s+", " ");
        return ORGANIC.matcher(normalized).find() && !NEGATED.matcher(normalized).find();
    }

    static boolean isWheat(String text) {
        return text != null && Pattern.compile("\\bwheat\\b", Pattern.CASE_INSENSITIVE)
                .matcher(text).find();
    }
}
