package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class ProductDetailLayoutContractTest {

    @Test
    public void productDetailHierarchy_prioritizesIdentityFindingsExplanationAndActions() throws Exception {
        String layout = readProjectFile("src/main/res/layout/fragment_product_details.xml");

        assertOrdered(layout,
                "@+id/product_image_view",
                "@+id/product_name_text_view",
                "@+id/product_brand_text_view",
                "@+id/source_status_container",
                "@+id/health_findings_card",
                "@+id/analysis_recycler_view",
                "@+id/ai_summary_container",
                "@+id/ingredients_card",
                "@+id/nutrition_details_card",
                "@+id/product_actions_card"
        );

        assertTrue("Health findings must be grouped in a card",
                layout.contains("@+id/health_findings_card"));
        assertTrue("Source status must have a labeled container",
                layout.contains("@+id/source_status_container"));
        assertTrue("Product actions must remain full-width and discoverable",
                layout.contains("@+id/product_actions_card"));
        assertTrue("The findings list must not compete with the page scroll",
                layout.contains("android:nestedScrollingEnabled=\"false\""));
        assertTrue("The production fragment must keep its bottom-sheet affordance",
                layout.contains("@+id/drag_handle"));
    }

    @Test
    public void productDetailDimensions_includePhoneAndTabletValues() throws Exception {
        String phoneDimensions = readProjectFile("src/main/res/values/dimens.xml");
        String tabletDimensions = readProjectFile("src/main/res/values-sw600dp/dimens.xml");

        assertTrue(phoneDimensions.contains("name=\"product_detail_content_padding\">18dp"));
        assertTrue(phoneDimensions.contains("name=\"product_detail_hero_height\">300dp"));
        assertTrue(phoneDimensions.contains("name=\"product_detail_fragment_image_height\">220dp"));
        assertTrue(tabletDimensions.contains("name=\"product_detail_content_padding\">64dp"));
        assertTrue(tabletDimensions.contains("name=\"product_detail_hero_height\">380dp"));
        assertTrue(tabletDimensions.contains("name=\"product_detail_fragment_image_height\">300dp"));
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

    private static String readProjectFile(String relativePath) throws IOException {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
