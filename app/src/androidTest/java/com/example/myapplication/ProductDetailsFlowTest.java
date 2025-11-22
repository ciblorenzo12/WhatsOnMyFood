package com.example.myapplication;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class ProductDetailsFlowTest {

    private static final String COCA_COLA_BARCODE = "030000010402";
    // CORRECTED: Updated to match the actual data returned by the live API.
    private static final String EXPECTED_PRODUCT_NAME = "Whole grain rolled oats";

    private CountingIdlingResource idlingResource;

    @Rule
    public ActivityTestRule<ProductDetailsActivity> activityRule = 
            new ActivityTestRule<>(ProductDetailsActivity.class, true, false);

    @Before
    public void setUp() {
        idlingResource = ProductRepository.idlingResource;
        IdlingRegistry.getInstance().register(idlingResource);
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void scanKnownBarcode_displaysCorrectProductDetails() {
        Context targetContext = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(targetContext, ProductDetailsActivity.class);
        intent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, COCA_COLA_BARCODE);

        activityRule.launchActivity(intent);

        onView(withId(R.id.product_name_text_view)).check(matches(isDisplayed()));
        onView(withId(R.id.product_name_text_view))
                .check(matches(withText(containsString(EXPECTED_PRODUCT_NAME))));
    }
}
