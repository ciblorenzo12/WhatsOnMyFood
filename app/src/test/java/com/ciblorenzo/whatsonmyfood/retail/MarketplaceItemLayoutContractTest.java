package com.ciblorenzo.whatsonmyfood.retail;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarketplaceItemLayoutContractTest {

    @Test
    public void comparisonCardHasIdentityComparisonRetailerAndActionSections() throws Exception {
        String layout = readProjectFile("src/main/res/layout/marketplace_item.xml");

        assertTrue(layout.contains("@+id/marketplace_item_role"));
        assertTrue(layout.contains("@+id/product_image"));
        assertTrue(layout.contains("@+id/product_name"));
        assertTrue(layout.contains("@+id/brand_name"));
        assertTrue(layout.contains("@+id/comparison_cue_text"));
        assertTrue(layout.contains("@+id/health_score"));
        assertTrue(layout.contains("@+id/retailer_name"));
        assertTrue(layout.contains("@+id/distance_text"));
        assertTrue(layout.contains("@+id/price_text"));
        assertTrue(layout.contains("@+id/retailer_action_button"));
        assertTrue(layout.contains("@string/marketplace_comparison_disclaimer"));
    }

    @Test
    public void comparisonCopyAvoidsMedicalSuperiorityClaims() throws Exception {
        String strings = readProjectFile("src/main/res/values/strings.xml").toLowerCase(Locale.US);

        assertTrue(strings.contains("not medical advice"));
        assertFalse(strings.contains("medically superior"));
        assertFalse(strings.contains("healthier alternative"));
        assertFalse(strings.contains("healthiest alternative"));
    }

    private static String readProjectFile(String relativePath) throws Exception {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
