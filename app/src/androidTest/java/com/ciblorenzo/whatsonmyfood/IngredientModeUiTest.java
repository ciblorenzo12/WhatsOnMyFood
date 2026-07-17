package com.ciblorenzo.whatsonmyfood;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class IngredientModeUiTest {

    private final GrantPermissionRule cameraPermission = GrantPermissionRule.grant(Manifest.permission.CAMERA);
    private final ActivityTestRule<ScanBarcodeActivity> activityRule =
            new ActivityTestRule<>(ScanBarcodeActivity.class, true, false);

    @Rule
    public final TestRule rules = RuleChain.outerRule(cameraPermission).around(activityRule);

    @Before
    public void launchScanner() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ScanBarcodeActivity.class);
        intent.putExtra(ScanBarcodeActivity.EXTRA_DISABLE_SCAN_TIMEOUTS_FOR_TESTS, true);
        activityRule.launchActivity(intent);
    }

    @Test
    public void modeRowSwitchesBetweenBarcodeAndIngredientInstructions() {
        onView(withId(R.id.mode_text_view)).check(matches(withText(R.string.barcode_mode)));

        onView(withId(R.id.mode_switch_container)).perform(click());
        onView(withId(R.id.mode_text_view)).check(matches(withText(R.string.ingredient_mode)));
        onView(withId(R.id.status_text_view)).check(matches(withText(R.string.point_camera_ingredients)));

        onView(withId(R.id.mode_switch_container)).perform(click());
        onView(withId(R.id.mode_text_view)).check(matches(withText(R.string.barcode_mode)));
        onView(withId(R.id.status_text_view)).check(matches(withText(R.string.point_camera_barcode)));
    }
}
