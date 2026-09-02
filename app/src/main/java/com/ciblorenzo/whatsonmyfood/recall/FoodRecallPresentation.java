package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.R;

/** Maps recall results to calm, explicit, and testable user-facing states. */
public final class FoodRecallPresentation {
    private FoodRecallPresentation() {}

    public static FoodRecallUiModel forState(FoodRecallState state) {
        FoodRecallState safeState = state == null ? FoodRecallState.READY : state;
        switch (safeState) {
            case CHECKING:
                return model(
                        R.string.food_recall_checking_badge,
                        R.string.food_recall_checking_title,
                        R.string.food_recall_checking_message,
                        0,
                        R.color.recall_neutral,
                        true,
                        false,
                        false
                );
            case NO_KNOWN_MATCH:
                return model(
                        R.string.food_recall_clear_badge,
                        R.string.food_recall_clear_title,
                        R.string.food_recall_clear_message,
                        R.string.food_recall_check_again,
                        R.color.recall_clear,
                        false,
                        true,
                        true
                );
            case POSSIBLE_MATCH:
                return model(
                        R.string.food_recall_possible_badge,
                        R.string.food_recall_possible_title,
                        R.string.food_recall_possible_message,
                        R.string.food_recall_view_notice,
                        R.color.recall_caution,
                        false,
                        true,
                        true
                );
            case CONFIRMED_MATCH:
                return model(
                        R.string.food_recall_confirmed_badge,
                        R.string.food_recall_confirmed_title,
                        R.string.food_recall_confirmed_message,
                        R.string.food_recall_view_notice,
                        R.color.recall_critical,
                        false,
                        true,
                        true
                );
            case STALE:
                return model(
                        R.string.food_recall_stale_badge,
                        R.string.food_recall_stale_title,
                        R.string.food_recall_stale_message,
                        R.string.food_recall_check_again,
                        R.color.recall_caution,
                        false,
                        true,
                        true
                );
            case UNAVAILABLE:
                return model(
                        R.string.food_recall_unavailable_badge,
                        R.string.food_recall_unavailable_title,
                        R.string.food_recall_unavailable_message,
                        R.string.food_recall_check_again,
                        R.color.recall_caution,
                        false,
                        true,
                        true
                );
            case ERROR:
                return model(
                        R.string.food_recall_error_badge,
                        R.string.food_recall_error_title,
                        R.string.food_recall_error_message,
                        R.string.food_recall_check_again,
                        R.color.recall_critical,
                        false,
                        true,
                        true
                );
            case READY:
            default:
                return model(
                        R.string.food_recall_ready_badge,
                        R.string.food_recall_ready_title,
                        R.string.food_recall_ready_message,
                        R.string.food_recall_action,
                        R.color.recall_neutral,
                        false,
                        true,
                        true
                );
        }
    }

    public static boolean requiresImmediateAttention(FoodRecallState state) {
        return state == FoodRecallState.CONFIRMED_MATCH || state == FoodRecallState.POSSIBLE_MATCH;
    }

    private static FoodRecallUiModel model(
            int badge,
            int title,
            int message,
            int action,
            int color,
            boolean progress,
            boolean primaryAction,
            boolean officialSource
    ) {
        return new FoodRecallUiModel(
                badge,
                title,
                message,
                action,
                color,
                progress,
                primaryAction,
                officialSource
        );
    }
}
