package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PantryNavigationTest {

    @Test
    public void pantryProductCreatesDetailsIntentForItsExactBarcode() {
        Context context = ApplicationProvider.getApplicationContext();
        Product product = new Product(
                "m4-05-target", "Target Product", "Test Foods", "12 oz", "", "", "Box",
                "Test", "1 serving", "b", "2", "b", null, 76
        );

        Intent intent = PantryNavigation.productDetailsIntent(context, product);

        assertEquals(ProductDetailsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("m4-05-target", intent.getStringExtra(ProductDetailsActivity.EXTRA_BARCODE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingBarcodeCannotOpenAnUnrelatedProduct() {
        Context context = ApplicationProvider.getApplicationContext();
        Product product = new Product(
                "", "Invalid Product", "Test Foods", "", "", "", "", "", "", "", "", ""
        );
        PantryNavigation.productDetailsIntent(context, product);
    }
}
