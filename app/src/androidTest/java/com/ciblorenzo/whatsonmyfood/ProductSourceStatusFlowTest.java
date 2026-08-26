package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProductSourceStatusFlowTest {

    @Test
    public void onlineCurrentData_isAttributedWithoutClaimingGuaranteedFreshness() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("online")) {
            scenario.onActivity(activity -> {
                assertEquals("PRODUCT DATABASE", text(activity, R.id.source_status_indicator_text_view));
                assertContains(text(activity, R.id.source_status_text_view), "Updated from product database");
                assertContains(
                        text(activity, R.id.source_status_interpretation_text_view),
                        "depends on the information supplied by the product source"
                );
            });
        }
    }

    @Test
    public void freshCache_isClearlyLabeledAsRecentSavedData() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("fresh_cache")) {
            scenario.onActivity(activity -> {
                assertEquals("RECENT SAVED RESULT", text(activity, R.id.source_status_indicator_text_view));
                assertContains(text(activity, R.id.source_status_text_view), "Fresh cached result");
                assertContains(
                        text(activity, R.id.source_status_interpretation_text_view),
                        "depends on the information supplied by the product source"
                );
            });
        }
    }

    @Test
    public void databaseAndRecoveredIngredients_showSourceAndReviewContext() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("recovered")) {
            scenario.onActivity(activity -> {
                assertEquals(View.VISIBLE, activity.findViewById(R.id.source_status_container).getVisibility());
                assertContains(text(activity, R.id.source_status_indicator_text_view), "RECOVERED INGREDIENTS");
                assertContains(text(activity, R.id.source_status_text_view), "Ingredients recovered");
                assertContains(
                        text(activity, R.id.source_status_interpretation_text_view),
                        "compare them with the package label"
                );
            });
        }
    }

    @Test
    public void staleCache_requestsRefreshWithoutClaimingCurrentData() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("stale")) {
            scenario.onActivity(activity -> {
                assertEquals("REFRESH RECOMMENDED", text(activity, R.id.source_status_indicator_text_view));
                assertContains(text(activity, R.id.source_status_text_view), "may be outdated");
                assertContains(text(activity, R.id.source_status_interpretation_text_view), "may be older");
            });
        }
    }

    @Test
    public void offlineCopy_isClearlyDistinguishedFromStaleCache() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("offline")) {
            scenario.onActivity(activity -> {
                assertEquals("OFFLINE COPY", text(activity, R.id.source_status_indicator_text_view));
                assertContains(text(activity, R.id.source_status_interpretation_text_view),
                        "cannot confirm freshness while offline");
            });
        }
    }

    @Test
    public void aiUnavailable_keepsProductAndRuleBasedFallbackVisible() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("ai_unavailable")) {
            scenario.onActivity(activity -> {
                assertContains(text(activity, R.id.source_status_indicator_text_view), "AI UNAVAILABLE");
                assertEquals(activity.getString(R.string.bitwise_retry_explanation),
                        text(activity, R.id.ai_summary_text_view));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.health_findings_card).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.product_name_text_view).getVisibility());
                RecyclerView findings = activity.findViewById(R.id.analysis_recycler_view);
                assertTrue(findings.getAdapter() != null && findings.getAdapter().getItemCount() > 0);
            });
        }
    }

    private ActivityScenario<ProductDetailLayoutPreviewActivity> launch(String scenario) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ProductDetailLayoutPreviewActivity.class);
        intent.putExtra("source_scenario", scenario);
        return ActivityScenario.launch(intent);
    }

    private static String text(ProductDetailLayoutPreviewActivity activity, int viewId) {
        return ((TextView) activity.findViewById(viewId)).getText().toString();
    }

    private static void assertContains(String value, String expectedPart) {
        assertTrue("Expected <" + value + "> to contain <" + expectedPart + ">",
                value.contains(expectedPart));
    }
}
