package com.ciblorenzo.whatsonmyfood;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class OpenFoodFactsOcrSampleTest {

    private static final List<String> SAMPLE_FILES = Arrays.asList(
            "012000161155-LIFEWTR_pH_balanced-ingredients.jpg",
            "016000275287-Cheerios-ingredients.jpg",
            "028400090896-Doritos_Nacho_Cheese-ingredients.jpg",
            "038000138416-Pringles_Original-ingredients.jpg",
            "3017620422003-Nutella-ingredients.jpg",
            "5000159484695-Twix_ice_cream-ingredients.jpg",
            "5449000000996-Coca-Cola-ingredients.jpg",
            "7622210449283-Prince_chocolate_biscuits-ingredients.jpg",
            "8000500310427-Nutella_Biscuits-ingredients.jpg",
            "8076809513753-Pesto_alla_Genovese-ingredients.jpg"
    );

    @Test
    public void clearIngredientImagesProduceReadableParsedLabels() throws Exception {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        List<String> failures = new ArrayList<>();
        try {
            for (String filename : SAMPLE_FILES) {
                Bitmap original;
                try (InputStream stream = InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getAssets()
                        .open("openfoodfacts-ingredients/" + filename)) {
                    original = BitmapFactory.decodeStream(stream);
                }

                String originalText = Tasks.await(
                        recognizer.process(InputImage.fromBitmap(original, 0)),
                        20,
                        TimeUnit.SECONDS
                ).getText();
                Bitmap enhanced = OcrImagePreprocessor.enhance(original);
                String enhancedText = Tasks.await(
                        recognizer.process(InputImage.fromBitmap(enhanced, 0)),
                        20,
                        TimeUnit.SECONDS
                ).getText();
                String bestText = IngredientOcrHeuristics.chooseBest(originalText, enhancedText);
                IngredientLabelValidator.Result result = IngredientLabelValidator.validate(bestText);
                if (!result.readable) {
                    failures.add(filename + " => " + result.failureReason + " | OCR: " + abbreviate(bestText));
                }
                enhanced.recycle();
                original.recycle();
            }
        } finally {
            recognizer.close();
        }

        assertTrue("Unreadable Open Food Facts samples:\n" + String.join("\n", failures), failures.isEmpty());
    }

    @Test
    public void preprocessingRecoversDimmedSkewedAndDownscaledLabels() throws Exception {
        List<String> variationSamples = Arrays.asList(
                "016000275287-Cheerios-ingredients.jpg",
                "028400090896-Doritos_Nacho_Cheese-ingredients.jpg",
                "3017620422003-Nutella-ingredients.jpg"
        );
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        List<String> failures = new ArrayList<>();
        try {
            for (String filename : variationSamples) {
                Bitmap source = loadBitmap(filename);
                Bitmap degraded = createDimmedSkewedCopy(source);
                Bitmap enhanced = OcrImagePreprocessor.enhance(degraded);
                String recoveredText = Tasks.await(
                        recognizer.process(InputImage.fromBitmap(enhanced, 0)),
                        20,
                        TimeUnit.SECONDS
                ).getText();
                IngredientLabelValidator.Result result = IngredientLabelValidator.validate(recoveredText);
                if (!result.readable) {
                    failures.add(filename + " => " + result.failureReason + " | OCR: " + abbreviate(recoveredText));
                }
                enhanced.recycle();
                degraded.recycle();
                source.recycle();
            }
        } finally {
            recognizer.close();
        }

        assertTrue("Unrecovered image variations:\n" + String.join("\n", failures), failures.isEmpty());
    }

    private static Bitmap loadBitmap(String filename) throws Exception {
        try (InputStream stream = InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getAssets()
                .open("openfoodfacts-ingredients/" + filename)) {
            return BitmapFactory.decodeStream(stream);
        }
    }

    private static Bitmap createDimmedSkewedCopy(Bitmap source) {
        int reducedWidth = Math.max(1, source.getWidth() / 2);
        int reducedHeight = Math.max(1, source.getHeight() / 2);
        Bitmap reduced = Bitmap.createScaledBitmap(source, reducedWidth, reducedHeight, true);
        Bitmap softened = Bitmap.createScaledBitmap(reduced, source.getWidth(), source.getHeight(), true);
        reduced.recycle();

        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.rgb(28, 28, 28));
        Matrix transform = new Matrix();
        transform.setRotate(8f, source.getWidth() / 2f, source.getHeight() / 2f);
        transform.postScale(0.9f, 0.9f, source.getWidth() / 2f, source.getHeight() / 2f);
        ColorMatrix dim = new ColorMatrix();
        dim.setScale(0.62f, 0.62f, 0.62f, 1f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(dim));
        canvas.drawBitmap(softened, transform, paint);
        softened.recycle();
        return result;
    }

    private static String abbreviate(String text) {
        if (text == null) return "";
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 160 ? oneLine : oneLine.substring(0, 160) + "...";
    }
}
