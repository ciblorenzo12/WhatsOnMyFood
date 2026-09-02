package com.ciblorenzo.whatsonmyfood.recall;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class FoodRecallLayoutContractTest {

    @Test
    public void recallScreenKeepsProductContextBeforeResultAndDisclaimer() throws Exception {
        String layout = readProjectFile("src/main/res/layout/activity_food_recall.xml");

        assertOrdered(
                layout,
                "@+id/food_recall_toolbar",
                "@+id/food_recall_entry_context",
                "@+id/food_recall_product_card",
                "@+id/food_recall_state_card",
                "@+id/food_recall_state_badge",
                "@+id/food_recall_state_title",
                "@+id/food_recall_state_message",
                "@+id/food_recall_primary_action",
                "@+id/food_recall_official_source_action",
                "@+id/food_recall_disclaimer"
        );
    }

    @Test
    public void scanAndPantryDetailsUseTheSameRecallEntryLayout() throws Exception {
        String activityLayout = readProjectFile("src/main/res/layout/activity_product_details.xml");
        String fragmentLayout = readProjectFile("src/main/res/layout/fragment_product_details.xml");
        String manifest = readProjectFile("src/main/AndroidManifest.xml");

        assertTrue(activityLayout.contains("@layout/layout_food_recall_entry"));
        assertTrue(fragmentLayout.contains("@layout/layout_food_recall_entry"));
        assertTrue(manifest.contains(".recall.FoodRecallActivity"));
    }

    @Test
    public void copyDoesNotTreatNoMatchAsSafetyGuarantee() throws Exception {
        String strings = readProjectFile("src/main/res/values/strings.xml");

        assertTrue(strings.contains("not a guarantee that a product is safe"));
        assertTrue(strings.contains("lot, date, and package details"));
        assertTrue(strings.contains("official FDA recall source"));
    }

    private static void assertOrdered(String source, String... values) {
        int previous = -1;
        for (String value : values) {
            int current = source.indexOf(value);
            assertTrue("Missing layout marker: " + value, current >= 0);
            assertTrue("Layout marker is out of order: " + value, current > previous);
            previous = current;
        }
    }

    private static String readProjectFile(String relativePath) throws Exception {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
