package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SourceStatusPresentationTest {

    @Test
    public void databaseResult_usesSourceDependentLanguageInsteadOfCertaintyClaim() {
        SourceStatusPresentation.Model model = SourceStatusPresentation.resolve(
                Collections.singletonList(ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE)
        );

        assertEquals(
                Collections.singletonList(SourceStatusPresentation.Indicator.PRODUCT_DATABASE),
                model.indicators
        );
        assertEquals(
                SourceStatusPresentation.InterpretationNote.SOURCE_RECORD_DEPENDENT,
                model.interpretationNote
        );
    }

    @Test
    public void recoveredIngredients_areMarkedForPackageReview() {
        SourceStatusPresentation.Model model = SourceStatusPresentation.resolve(Arrays.asList(
                ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE,
                ProductRepository.SourceStatus.INGREDIENTS_RECOVERED_FROM_LABEL_OR_SUPPORTING_SERVICE
        ));

        assertEquals(Arrays.asList(
                SourceStatusPresentation.Indicator.PRODUCT_DATABASE,
                SourceStatusPresentation.Indicator.RECOVERED_INGREDIENTS
        ), model.indicators);
        assertEquals(
                SourceStatusPresentation.InterpretationNote.RECOVERED_INGREDIENTS_REVIEW,
                model.interpretationNote
        );
    }

    @Test
    public void staleCache_requestsRefresh() {
        SourceStatusPresentation.Model model = SourceStatusPresentation.resolve(
                Collections.singletonList(ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED)
        );

        assertEquals(
                Collections.singletonList(SourceStatusPresentation.Indicator.REFRESH_RECOMMENDED),
                model.indicators
        );
        assertEquals(
                SourceStatusPresentation.InterpretationNote.SAVED_INFORMATION_MAY_BE_OLDER,
                model.interpretationNote
        );
    }

    @Test
    public void offlineCache_doesNotRepeatStaleIndicator() {
        SourceStatusPresentation.Model model = SourceStatusPresentation.resolve(Arrays.asList(
                ProductRepository.SourceStatus.SAVED_OFFLINE_RESULT,
                ProductRepository.SourceStatus.INFORMATION_MAY_BE_OUTDATED
        ));

        assertEquals(
                Collections.singletonList(SourceStatusPresentation.Indicator.OFFLINE_COPY),
                model.indicators
        );
        assertEquals(
                SourceStatusPresentation.InterpretationNote.OFFLINE_FRESHNESS_UNCONFIRMED,
                model.interpretationNote
        );
    }

    @Test
    public void unavailableAi_keepsRuleFindingsAsFallback() {
        SourceStatusPresentation.Model model = SourceStatusPresentation.resolve(Arrays.asList(
                ProductRepository.SourceStatus.UPDATED_FROM_PRODUCT_DATABASE,
                ProductRepository.SourceStatus.AI_EXPLANATION_UNAVAILABLE
        ));

        assertEquals(Arrays.asList(
                SourceStatusPresentation.Indicator.PRODUCT_DATABASE,
                SourceStatusPresentation.Indicator.AI_UNAVAILABLE
        ), model.indicators);
        assertEquals(
                SourceStatusPresentation.InterpretationNote.RULE_FINDINGS_REMAIN_AVAILABLE,
                model.interpretationNote
        );
    }
}
