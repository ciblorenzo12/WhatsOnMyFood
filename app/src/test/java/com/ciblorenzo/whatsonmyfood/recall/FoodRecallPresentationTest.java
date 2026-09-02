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
        assertTrue(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.CONFIRMED_MATCH));
    }

    @Test
    public void possibleMatchIsCautiousWithoutBeingPresentedAsConfirmed() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.POSSIBLE_MATCH);

        assertEquals(R.string.food_recall_possible_badge, model.badgeText);
        assertEquals(R.color.recall_caution, model.statusColor);
        assertTrue(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.POSSIBLE_MATCH));
    }

    @Test
    public void noKnownMatchKeepsSourceAndRetryAvailable() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.NO_KNOWN_MATCH);

        assertEquals(R.color.recall_clear, model.statusColor);
        assertTrue(model.showPrimaryAction);
        assertTrue(model.showOfficialSource);
        assertFalse(FoodRecallPresentation.requiresImmediateAttention(FoodRecallState.NO_KNOWN_MATCH));
    }

    @Test
    public void checkingStateOnlyShowsProgress() {
        FoodRecallUiModel model = FoodRecallPresentation.forState(FoodRecallState.CHECKING);

        assertTrue(model.showProgress);
        assertFalse(model.showPrimaryAction);
        assertFalse(model.showOfficialSource);
    }

    @Test
    public void unknownStateNameFallsBackToReady() {
        assertEquals(FoodRecallState.READY, FoodRecallState.fromName("not-a-state"));
        assertEquals(FoodRecallState.READY, FoodRecallState.fromName(null));
    }
}
