package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PantryRecallLayoutTest {

    @Test
    public void recallActionExistsOnPhoneAndTabletLayouts() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int widthDp : new int[]{360, 1000}) {
                Configuration config = new Configuration(base.getResources().getConfiguration());
                config.screenWidthDp = widthDp;
                config.smallestScreenWidthDp = widthDp;
                Context context = new ContextThemeWrapper(
                        base.createConfigurationContext(config), R.style.Theme_MyApplication);
                View pantry = LayoutInflater.from(context).inflate(R.layout.activity_pantry, null);
                assertNotNull("Missing recall action at " + widthDp + "dp",
                        pantry.findViewById(R.id.pantry_recall_notifications));
            }
        });
    }
}
