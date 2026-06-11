package com.example.myapplication.analysis;

import java.util.List;

public final class HealthVerdict {

    public enum Status {
        HEALTHY,
        NOT_HEALTHY,
        REVIEW
    }

    private final Status status;
    private final String label;
    private final String reason;

    private HealthVerdict(Status status, String label, String reason) {
        this.status = status;
        this.label = label;
        this.reason = reason;
    }

    public static HealthVerdict fromResults(List<AnalysisResult> results, int ingredientCount) {
        if (ingredientCount <= 0) {
            return new HealthVerdict(Status.REVIEW, "Needs Review", "No reliable ingredient list was detected.");
        }

        int severeCount = 0;
        int warningCount = 0;
        int positiveCount = 0;

        if (results != null) {
            for (AnalysisResult result : results) {
                if (result == null || result.getLevel() == null) continue;
                switch (result.getLevel()) {
                    case SEVERE:
                        severeCount++;
                        break;
                    case WARNING:
                        warningCount++;
                        break;
                    case POSITIVE:
                        positiveCount++;
                        break;
                    default:
                        break;
                }
            }
        }

        if (severeCount > 0) {
            return new HealthVerdict(Status.NOT_HEALTHY, "Not Healthy", "Contains a high-concern ingredient or nutrition signal.");
        }

        if (warningCount >= 3 || (warningCount >= 2 && positiveCount == 0)) {
            return new HealthVerdict(Status.NOT_HEALTHY, "Not Healthy", "Multiple caution signals make this a poor everyday choice.");
        }

        return new HealthVerdict(Status.HEALTHY, "Healthy", "No high-concern ingredients were detected.");
    }

    public static HealthVerdict fromAiVerdict(String aiVerdict, String aiReason, List<AnalysisResult> fallbackResults, int ingredientCount) {
        HealthVerdict fallback = fromResults(fallbackResults, ingredientCount);
        if (fallback.getStatus() == Status.NOT_HEALTHY) {
            return fallback;
        }

        String normalized = aiVerdict != null ? aiVerdict.trim().toUpperCase().replace('-', '_').replace(' ', '_') : "";
        String reason = aiReason != null && !aiReason.trim().isEmpty() ? aiReason.trim() : fallback.getReason();

        if (normalized.equals("NOT_HEALTHY") || normalized.equals("NOT_APPROVED")) {
            return new HealthVerdict(Status.NOT_HEALTHY, "Not Healthy", reason);
        }

        if (normalized.equals("HEALTHY") || normalized.equals("APPROVED")) {
            if (ingredientCount <= 0) {
                return fallback;
            }
            return new HealthVerdict(Status.HEALTHY, "Healthy", reason);
        }

        if (normalized.equals("REVIEW") || normalized.equals("NEEDS_REVIEW")) {
            return new HealthVerdict(Status.REVIEW, "Needs Review", reason);
        }

        return fallback;
    }

    public Status getStatus() {
        return status;
    }

    public String getLabel() {
        return label;
    }

    public String getReason() {
        return reason;
    }
}
