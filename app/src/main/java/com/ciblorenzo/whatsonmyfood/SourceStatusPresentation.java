package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts repository source states into shopper-facing indicator categories.
 * The categories describe provenance and freshness, not a statistical confidence score.
 */
public final class SourceStatusPresentation {

    public enum Indicator {
        PRODUCT_DATABASE,
        RECENT_SAVED_RESULT,
        SUPPORTING_SOURCE,
        RECOVERED_INGREDIENTS,
        REFRESH_RECOMMENDED,
        OFFLINE_COPY,
        AI_UNAVAILABLE
    }

    public enum InterpretationNote {
        SOURCE_RECORD_DEPENDENT,
        SUPPORTING_SOURCE_DEPENDENT,
        RECOVERED_INGREDIENTS_REVIEW,
        SAVED_INFORMATION_MAY_BE_OLDER,
        OFFLINE_FRESHNESS_UNCONFIRMED,
        RULE_FINDINGS_REMAIN_AVAILABLE
    }

    public static final class Model {
        public final List<Indicator> indicators;
        public final InterpretationNote interpretationNote;

        private Model(List<Indicator> indicators, InterpretationNote interpretationNote) {
            this.indicators = indicators;
            this.interpretationNote = interpretationNote;
        }
    }

    private SourceStatusPresentation() {
    }

    public static Model resolve(List<ProductRepository.SourceStatus> statuses) {
        Set<ProductRepository.SourceStatus> unique = new LinkedHashSet<>();
        if (statuses != null) unique.addAll(statuses);

        List<Indicator> indicators = new ArrayList<>();
        if (unique.contains(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE)) {
            indicators.add(Indicator.PRODUCT_DATABASE);
        }
        if (unique.contains(ProductRepository.SourceStatus.FRESH_CACHED_RESULT)) {
            indicators.add(Indicator.RECENT_SAVED_RESULT);
        }
        if (unique.contains(ProductRepository.SourceStatus.FALLBACK_PRODUCT_SOURCE)) {
            indicators.add(Indicator.SUPPORTING_SOURCE);
        }
        if (unique.contains(ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE)) {
            indicators.add(Indicator.RECOVERED_INGREDIENTS);
        }
        if (unique.contains(ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT)) {
            indicators.add(Indicator.OFFLINE_COPY);
        } else if (unique.contains(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED)) {
            indicators.add(Indicator.REFRESH_RECOMMENDED);
        }
        if (unique.contains(ProductRepository.SourceStatus.AI_EXPLANATION_UNAVAILABLE)) {
            indicators.add(Indicator.AI_UNAVAILABLE);
        }

        return new Model(indicators, resolveInterpretationNote(unique));
    }

    private static InterpretationNote resolveInterpretationNote(Set<ProductRepository.SourceStatus> statuses) {
        if (statuses.contains(ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT)) {
            return InterpretationNote.OFFLINE_FRESHNESS_UNCONFIRMED;
        }
        if (statuses.contains(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED)) {
            return InterpretationNote.SAVED_INFORMATION_MAY_BE_OLDER;
        }
        if (statuses.contains(ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE)) {
            return InterpretationNote.RECOVERED_INGREDIENTS_REVIEW;
        }
        if (statuses.contains(ProductRepository.SourceStatus.AI_EXPLANATION_UNAVAILABLE)) {
            return InterpretationNote.RULE_FINDINGS_REMAIN_AVAILABLE;
        }
        if (statuses.contains(ProductRepository.SourceStatus.FALLBACK_PRODUCT_SOURCE)) {
            return InterpretationNote.SUPPORTING_SOURCE_DEPENDENT;
        }
        return InterpretationNote.SOURCE_RECORD_DEPENDENT;
    }
}
