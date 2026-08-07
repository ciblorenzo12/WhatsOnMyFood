package com.ciblorenzo.whatsonmyfood;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HostedBackendConnectivityTest {

    @Test
    public void deviceRecoversIngredientsThroughHostedHttpsRagService() throws Exception {
        assertTrue(BuildConfig.RETAILER_BACKEND_BASE_URL.startsWith("https://"));
        assertTrue(BuildConfig.BITWISE_LLM_BASE_URL.startsWith("https://"));
        assertEquals(BuildConfig.RETAILER_BACKEND_BASE_URL, BuildConfig.BITWISE_LLM_BASE_URL);
        assertFalse(BuildConfig.BITWISE_APP_TOKEN.trim().isEmpty());

        ProductResponse response = new RagIngredientLookupClient().getIngredients(
                "051500255162",
                "CREAMY PEANUT BUTTER",
                "Jif"
        );

        assertNotNull(response);
        assertEquals(1, response.status);
        assertNotNull(response.product);
        String ingredients = response.product.ingredientsTextEn != null
                ? response.product.ingredientsTextEn
                : response.product.ingredientsText;
        assertNotNull(ingredients);
        assertFalse(ingredients.trim().isEmpty());
        assertTrue(ingredients.toLowerCase().contains("peanut"));
    }
}
