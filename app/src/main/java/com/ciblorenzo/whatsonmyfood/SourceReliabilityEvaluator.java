package com.ciblorenzo.whatsonmyfood;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Fast, offline source-quality estimate used when rendering scientific references. */
public final class SourceReliabilityEvaluator {

    private static final List<String> PRIMARY_AUTHORITIES = Arrays.asList(
            "fda.gov", "nih.gov", "ncbi.nlm.nih.gov", "cdc.gov", "usda.gov",
            "who.int", "efsa.europa.eu", "cochranelibrary.com"
    );
    private static final List<String> RESEARCH_HOSTS = Arrays.asList(
            "pubmed.ncbi.nlm.nih.gov", "pmc.ncbi.nlm.nih.gov", "doi.org", "bmj.com"
    );
    private static final List<String> PROFESSIONAL_HEALTH_HOSTS = Arrays.asList("heart.org");

    public enum Level {
        VERY_STRONG,
        STRONG,
        MODERATE,
        LIMITED
    }

    public static final class Rating {
        public final int score;
        public final Level level;

        private Rating(int score, Level level) {
            this.score = score;
            this.level = level;
        }
    }

    private SourceReliabilityEvaluator() {
    }

    public static Rating evaluate(String name, String url, String searchQuery) {
        String host = host(url);
        int authority = authorityScore(host);
        int evidence = evidenceScore(host);
        int relevance = relevanceScore(name, url, searchQuery);
        int traceability = traceabilityScore(name, url);
        int transport = url != null && url.trim().toLowerCase(Locale.US).startsWith("https://") ? 8 : 0;
        return fromScore(authority + evidence + relevance + traceability + transport);
    }

    public static Rating fromServerScore(int score) {
        return fromScore(score);
    }

    private static Rating fromScore(int value) {
        int score = Math.max(0, Math.min(100, value));
        Level level = score >= 90
                ? Level.VERY_STRONG
                : score >= 75
                ? Level.STRONG
                : score >= 60
                ? Level.MODERATE
                : Level.LIMITED;
        return new Rating(score, level);
    }

    private static int authorityScore(String host) {
        if (matchesAny(host, PRIMARY_AUTHORITIES)) return 30;
        if (matchesAny(host, RESEARCH_HOSTS) || host.endsWith(".edu")) return 27;
        if (matchesAny(host, PROFESSIONAL_HEALTH_HOSTS)) return 22;
        if (host.endsWith(".gov") || host.endsWith(".int")) return 24;
        if (host.endsWith(".org")) return 12;
        return 6;
    }

    private static int evidenceScore(String host) {
        if (matchesAny(host, PRIMARY_AUTHORITIES) || matchesAny(host, RESEARCH_HOSTS)
                || host.endsWith(".edu")) return 24;
        if (matchesAny(host, PROFESSIONAL_HEALTH_HOSTS)) return 18;
        if (hostMatches(host, "openfoodfacts.org")) return 12;
        return 8;
    }

    private static int relevanceScore(String name, String url, String searchQuery) {
        String query = clean(searchQuery);
        if (query.isEmpty()) return 8;
        String sourceText = clean(name) + " " + clean(url);
        int matches = 0;
        for (String token : query.split("[^a-z0-9]+")) {
            if (token.length() >= 4 && sourceText.contains(token)) matches++;
        }
        return matches >= 2 ? 20 : matches == 1 ? 16 : 14;
    }

    private static int traceabilityScore(String name, String url) {
        int score = clean(name).isEmpty() || "source".equals(clean(name)) ? 0 : 5;
        try {
            String path = URI.create(url == null ? "" : url.trim()).getPath();
            if (path != null && path.length() > 1) score += 5;
        } catch (Exception ignored) {
            // An invalid URL receives no traceability credit.
        }
        return score;
    }

    private static String host(String value) {
        try {
            String host = URI.create(value == null ? "" : value.trim()).getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean matchesAny(String host, List<String> candidates) {
        for (String candidate : candidates) {
            if (hostMatches(host, candidate)) return true;
        }
        return false;
    }

    private static boolean hostMatches(String host, String candidate) {
        return host.equals(candidate) || host.endsWith("." + candidate);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}
