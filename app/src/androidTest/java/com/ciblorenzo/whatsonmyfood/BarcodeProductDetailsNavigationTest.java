package com.ciblorenzo.whatsonmyfood;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BarcodeProductDetailsNavigationTest {

    private static final String TEST_GTIN = "3017620422003";

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
    public void validatedBarcodeAttachesProductDetailsFragmentWithExactGtin() throws Exception {
        ScanBarcodeActivity activity = activityRule.getActivity();
        CountDownLatch attached = new CountDownLatch(1);
        AtomicReference<String> attachedBarcode = new AtomicReference<>();

        FragmentManager.FragmentLifecycleCallbacks callback = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentPreAttached(
                    @NonNull FragmentManager fragmentManager,
                    @NonNull Fragment fragment,
                    @NonNull Context context
            ) {
                if (fragment instanceof ProductDetailsFragment) {
                    attachedBarcode.set(fragment.requireArguments().getString("barcode"));
                    attached.countDown();
                }
            }
        };

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(callback, false);
            activity.handleBarcode(TEST_GTIN);
        });

        assertTrue("ProductDetailsFragment was not attached", attached.await(3, TimeUnit.SECONDS));
        assertEquals(TEST_GTIN, attachedBarcode.get());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                activity.getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(callback)
        );
    }
}
