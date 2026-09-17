package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.R;
import java.text.DateFormat;
import java.util.Date;

public final class RecallStatusText {
    private RecallStatusText() {}
    public static String describe(Context context, Product product, PantryRecallStatus status) {
        if (status == null) return context.getString(R.string.recall_pending);
        FoodRecallState state = status.displayState(product, System.currentTimeMillis());
        String title = context.getString(state == FoodRecallState.READY ? R.string.recall_pending
                : FoodRecallPresentation.forState(state).titleText);
        return title + "\n" + checked(context, status)
                + (!status.isCurrent(product, System.currentTimeMillis()) && status.lastSuccessfulAt > 0
                ? "\n" + context.getString(R.string.recall_outdated) : "");
    }
    public static String checked(Context context, PantryRecallStatus status) {
        String time = status == null || status.lastSuccessfulAt == 0
                ? context.getString(R.string.recall_never_checked)
                : context.getString(R.string.recall_last_success, DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(status.lastSuccessfulAt)));
        return time + (status != null && status.failed ? "\n" + context.getString(R.string.recall_refresh_failed) : "");
    }
}
