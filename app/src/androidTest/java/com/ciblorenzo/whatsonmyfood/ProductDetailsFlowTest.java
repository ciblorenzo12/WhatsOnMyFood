package com.ciblorenzo.whatsonmyfood;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ProductDetailsFlowTest {

    @Rule
    public ActivityTestRule<ProductDetailLayoutPreviewActivity> activityRule =
            new ActivityTestRule<>(ProductDetailLayoutPreviewActivity.class);

    @Test
    public void productDetailFragment_prioritizesFindingsAndKeepsActionsReachable() {
        onView(withId(R.id.bottom_sheet)).check(matches(isDisplayed()));
        onView(withId(R.id.drag_handle)).check(matches(isDisplayed()));
        onView(withId(R.id.product_image_view)).check(matches(isDisplayed()));
        onView(withId(R.id.product_name_text_view)).check(matches(isDisplayed()));
        onView(withId(R.id.source_status_container)).check(matches(isDisplayed()));
        onView(withId(R.id.health_score_text_view))
                .check(matches(isDescendantOfA(withId(R.id.health_findings_card))));
        onView(withId(R.id.analysis_recycler_view))
                .check(matches(isDescendantOfA(withId(R.id.health_findings_card))));
        onView(withId(R.id.product_actions_card)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.update_product_button)).check(matches(isDisplayed()));
    }
}
