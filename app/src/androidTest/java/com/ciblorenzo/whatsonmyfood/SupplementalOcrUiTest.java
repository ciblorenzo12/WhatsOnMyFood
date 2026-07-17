package com.ciblorenzo.whatsonmyfood;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class SupplementalOcrUiTest {

    private final GrantPermissionRule cameraPermission = GrantPermissionRule.grant(Manifest.permission.CAMERA);
    private final ActivityTestRule<ScanBarcodeActivity> activityRule =
            new ActivityTestRule<>(ScanBarcodeActivity.class, true, false);

    @Rule
    public final TestRule rules = RuleChain.outerRule(cameraPermission).around(activityRule);

    private void launchRecovery(String target, String existingIngredientText) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ScanBarcodeActivity.class);
        intent.putExtra(ScanBarcodeActivity.EXTRA_DISABLE_SCAN_TIMEOUTS_FOR_TESTS, true);
        intent.putExtra(ScanBarcodeActivity.EXTRA_SUPPLEMENTAL_TARGET, target);
        intent.putExtra(
                ScanBarcodeActivity.EXTRA_EXISTING_INGREDIENT_TEXT,
                existingIngredientText
        );
        activityRule.launchActivity(intent);
    }

    @Test
    public void asksUserToScanFrontWhenProductNameIsMissing() {
        launchRecovery(
                ScanBarcodeActivity.TARGET_PRODUCT_NAME,
                "Ingredients: sugar, cocoa, hazelnuts"
        );
        onView(withText(R.string.missing_product_name_scan_title))
                .check(matches(withText(R.string.missing_product_name_scan_title)));
        onView(withText(R.string.missing_product_name_scan_message))
                .check(matches(withText(R.string.missing_product_name_scan_message)));
    }

    @Test
    public void asksUserToScanIngredientsWhenIngredientListIsMissing() {
        launchRecovery(ScanBarcodeActivity.TARGET_INGREDIENTS, "");
        onView(withText(R.string.missing_ingredients_scan_title))
                .check(matches(withText(R.string.missing_ingredients_scan_title)));
        onView(withText(R.string.missing_ingredients_scan_message))
                .check(matches(withText(R.string.missing_ingredients_scan_message)));
    }
}
