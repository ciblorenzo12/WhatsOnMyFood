package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FoodRecallPresentationTest {

    @Test
    public void confirmedMatchRequiresImmediateAttentionAndOfficialSource() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.CONFIRMED_MATCH);

        assertEquals(R.string.food_recall_confirmed_badge, model.badgeText);
        assertEquals(R.color.recall_critical, model.statusColor);
        assertTrue(model.showPrimaryAction);
        assertTrue(model.showOfficialSource);
        assertEquals(R.string.food_recall_guidance_confirmed, model.guidanceText);
        assertTrue(model.urgentAlert);
        assertFalse(model.showFallbackPanel);
        assertTrue(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.CONFIRMED_MATCH));
    }

    @Test
    public void possibleMatchIsCautiousWithoutBeingPresentedAsConfirmed() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.POSSIBLE_MATCH);

        assertEquals(R.string.food_recall_possible_badge, model.badgeText);
        assertEquals(R.color.recall_caution, model.statusColor);
        assertEquals(R.string.food_recall_guidance_possible, model.guidanceText);
        assertTrue(model.urgentAlert);
        assertTrue(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.POSSIBLE_MATCH));
    }

    @Test
    public void noKnownMatchKeepsSourceAndRetryAvailable() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.NO_KNOWN_MATCH);

        assertEquals(R.color.recall_clear, model.statusColor);
        assertTrue(model.showPrimaryAction);
        assertTrue(model.showOfficialSource);
        assertEquals(R.string.food_recall_guidance_clear, model.guidanceText);
        assertFalse(model.showFallbackPanel);
        assertFalse(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.NO_KNOWN_MATCH));
    }

    @Test
    public void checkingStateOnlyShowsProgress() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.CHECKING);

        assertTrue(model.showProgress);
        assertFalse(model.showPrimaryAction);
        assertFalse(model.showOfficialSource);
        assertEquals(0, model.guidanceText);
        assertFalse(model.showFallbackPanel);
    }

    @Test
    public void unknownStateNameFallsBackToReady() {
        assertEquals(FoodRecallState.READY, FoodRecallState.fromName("not-a-state"));
        assertEquals(FoodRecallState.READY, FoodRecallState.fromName(null));
    }

    @Test
    public void recoveryStatesShowFallbackPanelWithSpecificGuidance() {
        FoodRecallUiModel stale = FoodRecallPresentation.forState(FoodRecallState.STALE);
        FoodRecallUiModel unavailable = FoodRecallPresentation.forState(FoodRecallState.UNAVAILABLE);
        FoodRecallUiModel error = FoodRecallPresentation.forState(FoodRecallState.ERROR);

        assertTrue(stale.showFallbackPanel);
        assertTrue(unavailable.showFallbackPanel);
        assertTrue(error.showFallbackPanel);
        assertEquals(R.string.food_recall_guidance_stale, stale.guidanceText);
        assertEquals(R.string.food_recall_guidance_unavailable, unavailable.guidanceText);
        assertEquals(R.string.food_recall_guidance_error, error.guidanceText);
    }
}
