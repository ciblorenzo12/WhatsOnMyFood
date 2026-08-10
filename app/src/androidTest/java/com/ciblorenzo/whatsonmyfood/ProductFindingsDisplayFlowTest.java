package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ProductFindingsDisplayFlowTest {

    @Rule
    public ActivityTestRule<ProductDetailLayoutPreviewActivity> activityRule =
            new ActivityTestRule<>(ProductDetailLayoutPreviewActivity.class, true, false);

    @Test
    public void completeFindings_showAllTypesWithoutDuplicateCardsAndSavedExplanation() {
        ProductDetailLayoutPreviewActivity activity = launch(null);
        RecyclerView findings = activity.findViewById(R.id.analysis_recycler_view);

        assertNotNull(findings.getAdapter());
        assertEquals(3, findings.getAdapter().getItemCount());
        onView(allOf(withText("Whole grain oats"), isDescendantOfA(withId(R.id.analysis_recycler_view))))
                .check(matches(withEffectiveVisibility(VISIBLE)));
        onView(allOf(withText("Added sugar"), isDescendantOfA(withId(R.id.analysis_recycler_view))))
                .check(matches(withEffectiveVisibility(VISIBLE)));
        onView(allOf(withText("Allergen information"), isDescendantOfA(withId(R.id.analysis_recycler_view))))
                .check(matches(withEffectiveVisibility(VISIBLE)));
        onView(withId(R.id.ai_summary_container)).check(matches(withEffectiveVisibility(VISIBLE)));
        onView(withId(R.id.findings_empty_state_text_view)).check(matches(withEffectiveVisibility(GONE)));
    }

    @Test
    public void completedAnalysisWithNoFindings_showsUsefulEmptyState() {
        ProductDetailLayoutPreviewActivity activity = launch("empty");
        RecyclerView findings = activity.findViewById(R.id.analysis_recycler_view);

        assertNull(findings.getAdapter());
        onView(withText(R.string.findings_none_identified))
                .check(matches(withEffectiveVisibility(VISIBLE)));
    }

    @Test
    public void unavailableAnalysis_showsFallbackWhileProductDetailsRemain() {
        ProductDetailLayoutPreviewActivity activity = launch("unavailable");
        RecyclerView findings = activity.findViewById(R.id.analysis_recycler_view);

        assertNull(findings.getAdapter());
        onView(withText(R.string.findings_analysis_unavailable))
                .check(matches(withEffectiveVisibility(VISIBLE)));
        onView(withId(R.id.product_name_text_view)).check(matches(withEffectiveVisibility(VISIBLE)));
    }

    private ProductDetailLayoutPreviewActivity launch(String scenario) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ProductDetailLayoutPreviewActivity.class);
        if (scenario != null) intent.putExtra("findings_scenario", scenario);
        return activityRule.launchActivity(intent);
    }
}
