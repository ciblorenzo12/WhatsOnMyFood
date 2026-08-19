package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ciblorenzo.whatsonmyfood.retail.MarketplaceActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MarketplaceStateDisplayFlowTest {

    @Test
    public void everyMarketplaceStateIsClearAndKeepsNavigationAvailable() {
        assertState("live", "LIVE RETAILER DATA", "Alternatives are ready", false, 1, "LIVE PROVIDER");
        assertState("mock", "DEVELOPMENT SAMPLE", "Showing simulated results", false, 1, "DEVELOPMENT SAMPLE");
        assertState("empty", "NO RESULTS", "No alternatives found.", true, 0, null);
        assertState("timeout", "REQUEST TIMED OUT", "The retailer search took too long", true, 0, null);
        assertState("error", "SERVICE UNAVAILABLE", "Retailer results are unavailable", true, 0, null);
    }

    private void assertState(
            String state,
            String expectedBadge,
            String expectedTitle,
            boolean retryVisible,
            int expectedItems,
            String expectedSourceLabel
    ) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, MarketplaceActivity.class)
                .putExtra(MarketplaceActivity.EXTRA_DEBUG_STATE, state);

        try (ActivityScenario<MarketplaceActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertEquals(expectedBadge, text(activity, R.id.marketplace_state_badge));
                assertEquals(expectedTitle, text(activity, R.id.marketplace_state_title));
                assertEquals(
                        retryVisible ? View.VISIBLE : View.GONE,
                        activity.findViewById(R.id.marketplace_retry_button).getVisibility()
                );
                RecyclerView results = activity.findViewById(R.id.marketplace_recycler_view);
                assertNotNull(results.getAdapter());
                assertEquals(expectedItems, results.getAdapter().getItemCount());
                if (expectedSourceLabel != null) {
                    assertEquals(expectedSourceLabel, sourceLabel(results));
                }
                assertEquals(View.VISIBLE, activity.findViewById(R.id.marketplace_toolbar).getVisibility());
            });
        }
    }

    private static String text(MarketplaceActivity activity, int viewId) {
        return ((TextView) activity.findViewById(viewId)).getText().toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String sourceLabel(RecyclerView results) {
        RecyclerView.Adapter adapter = results.getAdapter();
        RecyclerView.ViewHolder holder = adapter.createViewHolder(results, 0);
        adapter.bindViewHolder(holder, 0);
        return ((TextView) holder.itemView.findViewById(R.id.provider_source_text))
                .getText()
                .toString();
    }
}
