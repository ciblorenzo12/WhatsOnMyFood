package com.ciblorenzo.whatsonmyfood;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ciblorenzo.whatsonmyfood.retail.MarketplaceActivity;
import com.ciblorenzo.whatsonmyfood.retail.MarketplaceNavigation;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MarketplaceNavigationFlowTest {

    @Test
    public void supportedProductCarriesItsContextAndReturnsToDetails() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                MarketplaceActivity.class.getName(),
                null,
                false
        );

        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch(null)) {
            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.comparison_view_button);
                assertTrue(action.isEnabled());
                assertTrue("The comparison action did not handle the tap.", action.performClick());
            });

            Activity marketplace = monitor.waitForActivityWithTimeout(5_000L);
            assertNotNull("The alternatives screen did not open.", marketplace);

            ProductWithDetails passedProduct = MarketplaceNavigation.readProduct(marketplace.getIntent());
            assertNotNull("The alternatives screen did not receive a valid product.", passedProduct);
            assertEquals("012345678905", passedProduct.product.barcode);
            assertEquals("Whole Grain Oat Cereal", passedProduct.product.productName);
            assertEquals("Sample Market Foods", passedProduct.product.brands);
            assertEquals("Breakfast cereals, whole grain foods", passedProduct.product.categories);

            instrumentation.runOnMainSync(marketplace::finish);
            instrumentation.waitForIdleSync();
            scenario.onActivity(details -> {
                assertFalse(details.isFinishing());
                assertEquals(
                        "Whole Grain Oat Cereal",
                        ((TextView) details.findViewById(R.id.product_name_text_view)).getText().toString()
                );
            });
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void unsupportedProductExplainsWhyComparisonIsUnavailable() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("unsupported")) {
            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.comparison_view_button);
                TextView explanation = activity.findViewById(R.id.comparison_unavailable_text);
                assertFalse(action.isEnabled());
                assertEquals(View.VISIBLE, explanation.getVisibility());
                assertEquals(activity.getString(R.string.comparison_unavailable), explanation.getText().toString());
            });
        }
    }

    @Test
    public void malformedMarketplacePayloadIsRejectedSafely() {
        Intent malformed = new Intent().putExtra(
                MarketplaceActivity.EXTRA_PRODUCT_JSON,
                "{not-valid-product-json"
        );
        assertNull(MarketplaceNavigation.readProduct(malformed));
    }

    private ActivityScenario<ProductDetailLayoutPreviewActivity> launch(String comparisonScenario) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ProductDetailLayoutPreviewActivity.class);
        if (comparisonScenario != null) {
            intent.putExtra("comparison_scenario", comparisonScenario);
        }
        return ActivityScenario.launch(intent);
    }
}
