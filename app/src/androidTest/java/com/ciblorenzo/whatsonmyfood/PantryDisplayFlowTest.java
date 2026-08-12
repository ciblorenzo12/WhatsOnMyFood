package com.ciblorenzo.whatsonmyfood;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PantryDisplayFlowTest {

    @Test
    public void emptyPantryShowsGuidanceInsteadOfAnEmptyList() {
        try (ActivityScenario<PantryLayoutPreviewActivity> scenario = launch(true)) {
            scenario.onActivity(activity -> {
                assertEquals(View.VISIBLE, activity.findViewById(R.id.pantry_empty_state).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.pantry_recycler_view).getVisibility());
            });
        }
    }

    @Test
    public void populatedPantryShowsProductsAndEachRowSelectsItsOwnBarcode() {
        try (ActivityScenario<PantryLayoutPreviewActivity> scenario = launch(false)) {
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.pantry_recycler_view);
                assertEquals(View.VISIBLE, list.getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.pantry_empty_state).getVisibility());
                assertEquals(3, list.getAdapter().getItemCount());
            });

            assertRowSelectsBarcode(scenario, "Apple Oats", "m4-05-apple");
            assertRowSelectsBarcode(scenario, "Banana Cereal", "m4-05-banana");
            assertRowSelectsBarcode(scenario, "Zucchini Crackers", "m4-05-zucchini");
        }
    }

    private void assertRowSelectsBarcode(
            ActivityScenario<PantryLayoutPreviewActivity> scenario,
            String productName,
            String expectedBarcode
    ) {
        Context context = ApplicationProvider.getApplicationContext();
        onView(withContentDescription(context.getString(R.string.open_product_details, productName)))
                .perform(click());
        scenario.onActivity(activity -> assertEquals(expectedBarcode, activity.getSelectedBarcode()));
    }

    private ActivityScenario<PantryLayoutPreviewActivity> launch(boolean empty) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, PantryLayoutPreviewActivity.class);
        intent.putExtra("empty", empty);
        return ActivityScenario.launch(intent);
    }
}
