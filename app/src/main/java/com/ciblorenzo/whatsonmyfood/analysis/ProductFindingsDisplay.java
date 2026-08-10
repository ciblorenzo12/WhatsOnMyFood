package com.ciblorenzo.whatsonmyfood.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds the complete, deduplicated findings state shown on product details. */
public final class ProductFindingsDisplay {

    public enum State {
        CONTENT,
        NO_FINDINGS,
        INGREDIENTS_REQUIRED,
        ANALYSIS_UNAVAILABLE
    }

    private final List<AnalysisResult> findings;
    private final State state;

    private ProductFindingsDisplay(List<AnalysisResult> findings, State state) {
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
        this.state = state;
    }

    public static ProductFindingsDisplay fromReport(
            ProductAnalysisReport report,
            boolean ingredientsAvailable
    ) {
        return combine(report, Collections.emptyList(), ingredientsAvailable);
    }

    public static ProductFindingsDisplay combine(
            ProductAnalysisReport report,
            List<AnalysisResult> supplementalFindings,
            boolean ingredientsAvailable
    ) {
        if (!ingredientsAvailable) {
            return new ProductFindingsDisplay(Collections.emptyList(), State.INGREDIENTS_REQUIRED);
        }

        if (report == null && (supplementalFindings == null || supplementalFindings.isEmpty())) {
            return new ProductFindingsDisplay(Collections.emptyList(), State.ANALYSIS_UNAVAILABLE);
        }

        List<AnalysisResult> combined = new ArrayList<>();
        if (report != null && report.getResults() != null) {
            combined.addAll(report.getResults());
        }
        if (supplementalFindings != null) {
            combined.addAll(supplementalFindings);
        }

        List<AnalysisResult> deduplicated = AnalysisResultDeduplicator.deduplicate(combined);
        State state = deduplicated.isEmpty() ? State.NO_FINDINGS : State.CONTENT;
        return new ProductFindingsDisplay(deduplicated, state);
    }

    public List<AnalysisResult> getFindings() {
        return findings;
    }

    public State getState() {
        return state;
    }

    public boolean hasFindings() {
        return state == State.CONTENT && !findings.isEmpty();
    }
}
