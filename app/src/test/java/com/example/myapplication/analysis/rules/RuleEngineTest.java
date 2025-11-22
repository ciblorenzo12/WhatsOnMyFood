package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.Nutriments;
import com.example.myapplication.Product;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.ProductAnalysisReport;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class RuleEngineTest {

    private RuleEngine ruleEngine;

    @Before
    public void setUp() {
        ruleEngine = new RuleEngine();
    }

    private static class ProductBuilder {
        private final String barcode;
        private List<Ingredient> ingredients = Collections.emptyList();
        private String novaGroup;
        private final Double[] nutrientValues = new Double[66];

        public ProductBuilder(String barcode) {
            this.barcode = barcode;
            Arrays.fill(nutrientValues, 0.0);
        }

        public ProductBuilder withIngredients(List<Ingredient> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public ProductBuilder withNovaGroup(String novaGroup) {
            this.novaGroup = novaGroup;
            return this;
        }

        public ProductBuilder withAddedSugars(double value) { nutrientValues[7] = value; return this; }
        public ProductBuilder withSugars(double value) { nutrientValues[6] = value; return this; }
        public ProductBuilder withSaturatedFat(double value) { nutrientValues[16] = value; return this; }
        public ProductBuilder withSodium(double value) { nutrientValues[61] = value; return this; }

        public ProductWithDetails build() {
            ProductWithDetails product = new ProductWithDetails();
            product.product = new Product(barcode, "Test Product", "Test Brand", null, null, null, null, null, null, null, novaGroup, null);
            product.ingredients = ingredients;
            product.nutriments = new Nutriments(barcode,
                nutrientValues[0], nutrientValues[1], nutrientValues[2], nutrientValues[3], 
                nutrientValues[4], nutrientValues[5], nutrientValues[6], nutrientValues[7], 
                nutrientValues[8], nutrientValues[9], nutrientValues[10], nutrientValues[11], 
                nutrientValues[12], nutrientValues[13], nutrientValues[14], nutrientValues[15], 
                nutrientValues[16], nutrientValues[17], nutrientValues[18], nutrientValues[19], 
                nutrientValues[20], nutrientValues[21], nutrientValues[22], nutrientValues[23], 
                nutrientValues[24], nutrientValues[25], nutrientValues[26], nutrientValues[27], 
                nutrientValues[28], nutrientValues[29], nutrientValues[30], nutrientValues[31], 
                nutrientValues[32], nutrientValues[33], nutrientValues[34], nutrientValues[35], 
                nutrientValues[36], nutrientValues[37], nutrientValues[38], nutrientValues[39], 
                nutrientValues[40], nutrientValues[41], nutrientValues[42], nutrientValues[43], 
                nutrientValues[44], nutrientValues[45], nutrientValues[46], nutrientValues[47], 
                nutrientValues[48], nutrientValues[49], nutrientValues[50], nutrientValues[51], 
                nutrientValues[52], nutrientValues[53], nutrientValues[54], nutrientValues[55], 
                nutrientValues[56], nutrientValues[57], nutrientValues[58], nutrientValues[59], 
                nutrientValues[60], nutrientValues[61], nutrientValues[62], nutrientValues[63], 
                nutrientValues[64], nutrientValues[65]
            );
            return product;
        }
    }

    @Test
    public void analyze_withCleanProduct_returnsFullScore() {
        ProductWithDetails product = new ProductBuilder("clean_barcode")
                .withIngredients(Collections.singletonList(new Ingredient("clean_barcode", "Water", 1)))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        assertTrue("A clean product should have a score of at least 100", report.getOverallScore() >= 100);
    }

    @Test
    public void analyze_withMultiplePenalties_calculatesCorrectScore() {
        // ARRANGE: A product designed to trigger multiple rules.
        List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("bad_barcode", "Water", 1),
                new Ingredient("bad_barcode", "Sugar", 2), // Now second, will trigger SugarAsMainIngredientRule
                new Ingredient("bad_barcode", "Natural flavor", 3)
        );
        ProductWithDetails product = new ProductBuilder("bad_barcode")
                .withIngredients(ingredients)
                .withAddedSugars(15.0)
                .withSugars(25.0)
                .withNovaGroup("4")
                .build();

        // ACT
        ProductAnalysisReport report = ruleEngine.analyze(product);

        // ASSERT: 100 - 50(Added) - 20(Main) - 20(Flavor) - 15(High) - 10(Ultra) = -15, which clamps to 0.
        assertEquals("The score should clamp to 0 when total penalties exceed 100", 0, report.getOverallScore());
    }

    @Test
    public void analyze_whenScoreGoesBelowZero_clampsScoreAtZero() {
        List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("worst_barcode", "Palm oil", 1),
                new Ingredient("worst_barcode", "Natural flavor", 2),
                new Ingredient("worst_barcode", "Sugar", 3),
                new Ingredient("worst_barcode", "Red 40", 4)
        );
        ProductWithDetails product = new ProductBuilder("worst_barcode")
                .withIngredients(ingredients)
                .withAddedSugars(35.0)
                .withSaturatedFat(25.0)
                .withSodium(2.0) // 2g of Sodium = 5g of Salt
                .withNovaGroup("4")
                .build();

        // ACT
        ProductAnalysisReport report = ruleEngine.analyze(product);

        // ASSERT
        assertEquals("The score should be clamped at 0 and not go negative", 0, report.getOverallScore());
    }
}
