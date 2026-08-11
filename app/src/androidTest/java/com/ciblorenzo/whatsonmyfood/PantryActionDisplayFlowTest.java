package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PantryActionDisplayFlowTest {

    @Test
    public void productNotSaved_showsOnlyEnabledSaveAction() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("available")) {
            scenario.onActivity(activity -> {
                Button add = activity.findViewById(R.id.add_to_pantry_button);
                Button remove = activity.findViewById(R.id.remove_from_pantry_button);
                assertEquals(View.VISIBLE, add.getVisibility());
                assertTrue(add.isEnabled());
                assertEquals(activity.getString(R.string.add_to_pantry), add.getText().toString());
                assertEquals(View.GONE, remove.getVisibility());
            });
        }
    }

    @Test
    public void productSaved_showsOnlyRemoveAction() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("saved")) {
            scenario.onActivity(activity -> {
                Button add = activity.findViewById(R.id.add_to_pantry_button);
                Button remove = activity.findViewById(R.id.remove_from_pantry_button);
                assertEquals(View.GONE, add.getVisibility());
                assertEquals(View.VISIBLE, remove.getVisibility());
                assertTrue(remove.isEnabled());
            });
        }
    }

    @Test
    public void saveInProgress_disablesSaveToPreventRepeatedRequests() {
        try (ActivityScenario<ProductDetailLayoutPreviewActivity> scenario = launch("saving")) {
            scenario.onActivity(activity -> {
                Button add = activity.findViewById(R.id.add_to_pantry_button);
                assertEquals(View.VISIBLE, add.getVisibility());
                assertFalse(add.isEnabled());
                assertEquals(activity.getString(R.string.pantry_saving), add.getText().toString());
            });
        }
    }

    private ActivityScenario<ProductDetailLayoutPreviewActivity> launch(String state) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ProductDetailLayoutPreviewActivity.class);
        intent.putExtra("pantry_state", state);
        return ActivityScenario.launch(intent);
    }
}
