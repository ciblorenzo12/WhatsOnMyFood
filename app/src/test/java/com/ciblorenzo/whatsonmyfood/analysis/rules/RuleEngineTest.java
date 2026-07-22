package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.Nutriments;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        private String labels;
        private String novaGroup;
        private final Double[] nutrientValues = new Double[67];

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

        public ProductBuilder withLabels(String labels) {
            this.labels = labels;
            return this;
        }

        public ProductBuilder withAddedSugars(double value) { nutrientValues[10] = value; return this; }
        public ProductBuilder withSugars(double value) { nutrientValues[9] = value; return this; }
        public ProductBuilder withSaturatedFat(double value) { nutrientValues[3] = value; return this; }
        public ProductBuilder withSodium(double value) { nutrientValues[22] = value; return this; }

        public ProductWithDetails build() {
            ProductWithDetails product = new ProductWithDetails();
            product.product = new Product(barcode, "Test Product", "Test Brand", null, null, labels, null, null, null, null, novaGroup, null);
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
                nutrientValues[64], nutrientValues[65], nutrientValues[66]
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

        assertEquals("A clean product should stop at the maximum score", RuleEngine.MAXIMUM_SCORE, report.getOverallScore());
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
                .withAddedSugars(51.0)
                .withSugars(25.0)
                .build();

        // ACT
        ProductAnalysisReport report = ruleEngine.analyze(product);

        // NOVA is no longer an automatic penalty; the score reflects the label's actual sugar and flavor signals.
        assertEquals("Only the strongest added-sugar penalty should count", 40, report.getOverallScore());
        int calculatedRawScore = report.getStartingScore();
        for (AnalysisResult result : report.getResults()) {
            calculatedRawScore += result.getScoreAdjustment();
        }
        assertEquals("Every displayed result must reconcile to the raw score", calculatedRawScore, report.getRawScore());
        assertTrue(report.getScoreExplanation().contains("Raw score: " + calculatedRawScore));
    }

    @Test
    public void analyze_novaGroupAloneDoesNotCreateANegativeFinding() {
        ProductWithDetails product = new ProductBuilder("nova_only")
                .withNovaGroup("4")
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        assertEquals(100, report.getOverallScore());
        assertFalse(report.getResults().stream()
                .anyMatch(result -> result.getMessage().contains("NOVA")));
    }

    @Test
    public void analyze_vegetableOilsApplyOnePenalty() {
        ProductWithDetails product = new ProductBuilder("oil_only")
                .withIngredients(Arrays.asList(
                        new Ingredient("oil_only", "Sunflower oil", 1),
                        new Ingredient("oil_only", "Palm oil", 2)
                ))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        long oilPenalties = report.getResults().stream()
                .filter(result -> "Contains vegetable oil".equals(result.getMessage()))
                .count();

        assertEquals(1, oilPenalties);
        assertEquals(95, report.getOverallScore()); // 100 - 15 oil penalty + 10 short-list adjustment
    }

    @Test
    public void analyze_oliveAvocadoAndCoconutOilsArePositiveAndNotPenalized() {
        ProductWithDetails product = new ProductBuilder("positive_oils")
                .withIngredients(Arrays.asList(
                        new Ingredient("positive_oils", "Olive oil", 1),
                        new Ingredient("positive_oils", "Avocado oil", 2),
                        new Ingredient("positive_oils", "Coconut oil", 3)
                ))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        assertFalse(report.getResults().stream()
                .anyMatch(result -> "Contains vegetable oil".equals(result.getMessage())));
        assertTrue(report.getResults().stream()
                .anyMatch(result -> "Contains beneficial oil".equals(result.getMessage())
                        && result.getRuleEffect() == AnalysisResult.RuleEffect.POSITIVE));
        assertEquals(115, report.getRawScore());
        assertEquals(100, report.getOverallScore());
    }

    @Test
    public void analyze_unlistedVegetableOilStillAppliesPolicyPenalty() {
        ProductWithDetails product = new ProductBuilder("unlisted_oil")
                .withIngredients(Collections.singletonList(new Ingredient("unlisted_oil", "Hazelnut oil", 1)))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        assertTrue(report.getResults().stream()
                .anyMatch(result -> "Contains vegetable oil".equals(result.getMessage())));
        assertEquals(95, report.getOverallScore());
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
                .withAddedSugars(51.0)
                .withSaturatedFat(25.0)
                .withSodium(2.0) // 2g of Sodium = 5g of Salt
                .withNovaGroup("4")
                .build();

        // ACT
        ProductAnalysisReport report = ruleEngine.analyze(product);

        // ASSERT
        assertEquals("The score should be clamped at 0 and not go negative", 0, report.getOverallScore());
        assertTrue(report.getRawScore() < RuleEngine.MINIMUM_SCORE);
        assertTrue(report.getScoreExplanation().contains("limited to 0-100"));
    }

    @Test
    public void analyze_withMultipleOrganicIngredients_reportsOrganicOnlyOnce() {
        List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("organic_barcode", "Organic oats", 1),
                new Ingredient("organic_barcode", "Organic cane sugar", 2),
                new Ingredient("organic_barcode", "Organic cinnamon", 3)
        );
        ProductWithDetails product = new ProductBuilder("organic_barcode")
                .withIngredients(ingredients)
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);
        long organicCount = report.getResults().stream()
                .filter(result -> "Contains organic ingredients".equals(result.getMessage()))
                .count();

        assertEquals(1, organicCount);
    }

    @Test
    public void analyze_withMilkWithoutOrganicOrNonGmoClaim_reportsSevereFinding() {
        ProductWithDetails product = new ProductBuilder("milk_barcode")
                .withIngredients(Collections.singletonList(new Ingredient("milk_barcode", "Milk", 1)))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);
        AnalysisResult milkResult = report.getResults().stream()
                .filter(result -> "Conventional milk without organic/Non-GMO claim".equals(result.getMessage()))
                .findFirst()
                .orElse(null);

        assertNotNull(milkResult);
        assertEquals(AnalysisResult.WarningLevel.SEVERE, milkResult.getLevel());
    }

    @Test
    public void analyze_withOrganicMilkClaim_keepsMilkInformational() {
        ProductWithDetails product = new ProductBuilder("organic_milk_barcode")
                .withLabels("Organic, Non-GMO")
                .withIngredients(Collections.singletonList(new Ingredient("organic_milk_barcode", "Milk", 1)))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);
        AnalysisResult milkResult = report.getResults().stream()
                .filter(result -> "Contains milk".equals(result.getMessage()))
                .findFirst()
                .orElse(null);

        assertNotNull(milkResult);
        assertEquals(AnalysisResult.WarningLevel.INFO, milkResult.getLevel());
    }

    @Test
    public void analyze_sameInputAlwaysReturnsSameScoreAndExplanations() {
        ProductWithDetails first = new ProductBuilder("deterministic_one")
                .withIngredients(Arrays.asList(
                        new Ingredient("deterministic_one", "Whole oats", 1),
                        new Ingredient("deterministic_one", "Sugar", 2),
                        new Ingredient("deterministic_one", "Natural flavor", 3)
                ))
                .build();
        ProductWithDetails second = new ProductBuilder("deterministic_two")
                .withIngredients(Arrays.asList(
                        new Ingredient("deterministic_two", "Whole oats", 1),
                        new Ingredient("deterministic_two", "Sugar", 2),
                        new Ingredient("deterministic_two", "Natural flavor", 3)
                ))
                .build();

        ProductAnalysisReport firstReport = ruleEngine.analyze(first);
        ProductAnalysisReport secondReport = ruleEngine.analyze(second);

        assertEquals(firstReport.getOverallScore(), secondReport.getOverallScore());
        assertEquals(firstReport.getRawScore(), secondReport.getRawScore());
        assertEquals(firstReport.getResults().size(), secondReport.getResults().size());
        for (int i = 0; i < firstReport.getResults().size(); i++) {
            assertEquals(firstReport.getResults().get(i).getMessage(), secondReport.getResults().get(i).getMessage());
            assertEquals(firstReport.getResults().get(i).getScoringExplanation(), secondReport.getResults().get(i).getScoringExplanation());
        }
    }

    @Test
    public void analyze_overlappingSugarRulesCountOnlyStrongestPenalty() {
        ProductWithDetails product = new ProductBuilder("sugar_overlap")
                .withIngredients(Collections.singletonList(new Ingredient("sugar_overlap", "High fructose corn syrup", 1)))
                .withAddedSugars(51.0)
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);
        long sugarFindings = report.getResults().stream()
                .filter(result -> "added_sugar".equals(result.getScoringGroup()))
                .count();

        assertEquals(1, sugarFindings);
        assertEquals(60, report.getRawScore()); // 100 - 50 added sugar + 10 short-list adjustment
    }

    @Test
    public void analyze_positiveAdjustmentIsCappedAtMaximumAndExplained() {
        ProductWithDetails product = new ProductBuilder("positive_cap")
                .withIngredients(Collections.singletonList(new Ingredient("positive_cap", "Water", 1)))
                .build();

        ProductAnalysisReport report = ruleEngine.analyze(product);

        assertEquals(110, report.getRawScore());
        assertEquals(RuleEngine.MAXIMUM_SCORE, report.getOverallScore());
        assertTrue(report.getScoreExplanation().contains("restores 10"));
        assertTrue(report.getScoreExplanation().contains("limited to 0-100"));
    }

    @Test
    public void getRuleDescriptions_explainsEveryActiveRuleAndScoreEffect() {
        List<String> descriptions = ruleEngine.getRuleDescriptions();

        assertEquals(28, descriptions.size());
        for (String description : descriptions) {
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
            assertTrue(description, description.contains("points") || description.contains("score"));
        }
    }

    @Test
    public void getRuleDescriptionsByCategory_assignsEveryActiveRuleExactlyOnce() {
        Map<ProductAnalysisRule.RuleCategory, List<String>> categories = ruleEngine.getRuleDescriptionsByCategory();

        assertEquals(4, categories.get(ProductAnalysisRule.RuleCategory.SUGAR).size());
        assertEquals(1, categories.get(ProductAnalysisRule.RuleCategory.SODIUM).size());
        assertEquals(4, categories.get(ProductAnalysisRule.RuleCategory.OILS).size());
        assertEquals(5, categories.get(ProductAnalysisRule.RuleCategory.ADDITIVES_AND_PRESERVATIVES).size());
        assertEquals(2, categories.get(ProductAnalysisRule.RuleCategory.FLAVORS).size());
        assertEquals(1, categories.get(ProductAnalysisRule.RuleCategory.PROCESSING_LEVEL).size());
        assertEquals(6, categories.get(ProductAnalysisRule.RuleCategory.POSITIVE_INGREDIENT_SIGNALS).size());
        assertEquals(2, categories.get(ProductAnalysisRule.RuleCategory.GENERAL_NUTRITION).size());
        assertEquals(2, categories.get(ProductAnalysisRule.RuleCategory.INGREDIENT_SOURCING).size());
        assertEquals(1, categories.get(ProductAnalysisRule.RuleCategory.ALLERGENS).size());

        int categorizedRuleCount = categories.values().stream().mapToInt(List::size).sum();
        assertEquals(ruleEngine.getRuleDescriptions().size(), categorizedRuleCount);
    }

    @Test
    public void analyze_allergenStatementsReachAnalysisAsSeparateInformationalFindings() {
        ProductWithDetails product = new ProductBuilder("allergen_analysis")
                .withIngredients(Collections.singletonList(new Ingredient("allergen_analysis", "Oats", 1)))
                .build();
        product.containsAllergens = Arrays.asList("milk", "wheat");
        product.mayContainAllergens = Collections.singletonList("peanuts");

        ProductAnalysisReport report = ruleEngine.analyze(product);
        AnalysisResult contains = report.getResults().stream()
                .filter(result -> "Allergen statement: Contains".equals(result.getMessage()))
                .findFirst()
                .orElse(null);
        AnalysisResult mayContain = report.getResults().stream()
                .filter(result -> "Allergen advisory: May contain".equals(result.getMessage()))
                .findFirst()
                .orElse(null);

        assertNotNull(contains);
        assertNotNull(mayContain);
        assertEquals(AnalysisResult.RuleEffect.INFORMATIONAL, contains.getRuleEffect());
        assertEquals(AnalysisResult.RuleEffect.INFORMATIONAL, mayContain.getRuleEffect());
        assertEquals(0, contains.getScorePenalty());
        assertEquals(0, mayContain.getScorePenalty());
    }
}
