package com.ciblorenzo.whatsonmyfood;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MlKitIngredientTranslatorTest {

    @Test
    public void translatesSpanishIngredientsToEnglish() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<MlKitIngredientTranslator.TranslationResult> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        MlKitIngredientTranslator.translateToEnglish(
                "Agua, azucar, sal, harina de trigo",
                new MlKitIngredientTranslator.Callback() {
                    @Override
                    public void onSuccess(MlKitIngredientTranslator.TranslationResult translation) {
                        result.set(translation);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception translationError) {
                        error.set(translationError);
                        latch.countDown();
                    }
                }
        );

        assertTrue("Ingredient translation timed out", latch.await(120, TimeUnit.SECONDS));
        if (error.get() != null) throw error.get();
        assertNotNull(result.get());
        String english = result.get().englishText.toLowerCase(Locale.US);
        assertTrue(english.contains("water"));
        assertTrue(english.contains("salt"));
    }
}
