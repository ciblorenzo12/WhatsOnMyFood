package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class ProductDetailsFlowTest {

    private static final String TAG = "ProductDetailsFlowTest";
    private static final String TEST_BARCODE = "030000010402"; // Quaker Oats

    private final CountingIdlingResource authIdlingResource = new CountingIdlingResource("FirebaseAuth");

    @Rule
    public ActivityTestRule<ProductDetailsActivity> activityRule = 
            new ActivityTestRule<>(ProductDetailsActivity.class, true, false);

    @Before
    public void setUp() {
        IdlingRegistry.getInstance().register(authIdlingResource);
        IdlingRegistry.getInstance().register(ProductRepository.idlingResource);
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(authIdlingResource);
        IdlingRegistry.getInstance().unregister(ProductRepository.idlingResource);
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void scanKnownBarcode_displaysFetchedProductDetails() {
        // ARRANGE: Sign in an anonymous user to satisfy the activity's prerequisite.
        authIdlingResource.increment();
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Firebase anonymous sign-in successful.");
            } else {
                Log.e(TAG, "Firebase anonymous sign-in failed!", task.getException());
                fail("Firebase anonymous sign-in failed. IMPORTANT: Please go to the Firebase Console, select your project, go to Authentication > Sign-in method, and ensure that the 'Anonymous' provider is ENABLED.");
            }
            authIdlingResource.decrement();
        });

        // Espresso will wait here until sign-in is complete.

        Context targetContext = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(targetContext, ProductDetailsActivity.class);
        intent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, TEST_BARCODE);

        // ACT: Launch the activity.
        activityRule.launchActivity(intent);

        // ASSERT: 
        onView(withId(R.id.product_name_text_view)).check(matches(isDisplayed()));
        onView(withId(R.id.product_name_text_view)).check(matches(not(withText("N/A"))));
    }
}
