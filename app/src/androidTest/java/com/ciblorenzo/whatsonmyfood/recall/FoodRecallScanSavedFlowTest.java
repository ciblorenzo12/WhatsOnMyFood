package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class FoodRecallScanSavedFlowTest {

    @Test
    public void scannedProductKeepsScanContextAndShowsConfirmedAlert() {
        ProductWithDetails product = product(
                "721582132834",
                "Pillsbury Bread Rolls Hard Roll Dough",
                "Pillsbury"
        );

        try (ActivityScenario<FoodRecallActivity> ignored = launch(
                product,
                FoodRecallNavigation.EntryPoint.SCAN_RESULT,
                FoodRecallState.CONFIRMED_MATCH
        )) {
            onView(withId(R.id.food_recall_entry_context))
                    .check(matches(withText(R.string.food_recall_scanned_context)));
            onView(withId(R.id.food_recall_product_name))
                    .check(matches(withText("Pillsbury Bread Rolls Hard Roll Dough")));
            onView(withId(R.id.food_recall_product_barcode))
                    .check(matches(withText("Barcode: 721582132834")));
            onView(withId(R.id.food_recall_state_badge))
                    .check(matches(withText(R.string.food_recall_confirmed_badge)));
            onView(withId(R.id.food_recall_state_title))
                    .check(matches(withText(R.string.food_recall_confirmed_title)));
        }
    }

    @Test
    public void savedProductKeepsPantryContextAndShowsNoKnownMatch() {
        ProductWithDetails product = product(
                "051500255162",
                "Creamy Peanut Butter",
                "Jif"
        );

        try (ActivityScenario<FoodRecallActivity> ignored = launch(
                product,
                FoodRecallNavigation.EntryPoint.SAVED_PRODUCT,
                FoodRecallState.NO_KNOWN_MATCH
        )) {
            onView(withId(R.id.food_recall_entry_context))
                    .check(matches(withText(R.string.food_recall_saved_context)));
            onView(withId(R.id.food_recall_product_name))
                    .check(matches(withText("Creamy Peanut Butter")));
            onView(withId(R.id.food_recall_product_brand))
                    .check(matches(withText("Jif")));
            onView(withId(R.id.food_recall_state_badge))
                    .check(matches(withText(R.string.food_recall_clear_badge)));
            onView(withId(R.id.food_recall_state_title))
                    .check(matches(withText(R.string.food_recall_clear_title)));
        }
    }

    private static ActivityScenario<FoodRecallActivity> launch(
            ProductWithDetails product,
            FoodRecallNavigation.EntryPoint entryPoint,
            FoodRecallState debugState
    ) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = FoodRecallNavigation.createIntent(context, product, entryPoint)
                .putExtra(FoodRecallActivity.EXTRA_DEBUG_STATE, debugState.name());
        return ActivityScenario.launch(intent);
    }

    private static ProductWithDetails product(String barcode, String name, String brand) {
        ProductWithDetails details = new ProductWithDetails();
        details.product = new Product(
                barcode, name, brand, "", "", "", "", "", "", "", "", ""
        );
        return details;
    }
}
