package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicator;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleEngine {

    public static final int STARTING_SCORE = 100;
    public static final int MINIMUM_SCORE = 0;
    public static final int MAXIMUM_SCORE = 100;

    private final List<ProductAnalysisRule> rules;

    public RuleEngine() {
        this.rules = Arrays.asList(
                // Negative Rules
                new ArtificialColorsRule(),
                new PartiallyHydrogenatedOilsRule(),
                new TransFatNutritionRule(),
                new JunkOilsRule(),
                new HighFructoseCornSyrupRule(),
                new ProcessedMeatPreservativesRule(),
                new PreservativesRule(),
                new AddedSugarRule(),
                new SugarAsMainIngredientRule(),
                new RefinedFlourRule(),
                new ArtificialSweetenersRule(),
                new HighSodiumRule(),
                new HighSugarRule(),
                new TextureAdditivesRule(),
                new ArtificialFlavorRule(),
                new NutriScoreRule(),
                new HighSaturatedFatRule(),
                new NonOrganicWheatRule(),
                new MilkRule(),

                // Informational / Warning Rules
                new AllergenStatementsRule(),
                new NaturalFlavorRule(),

                // Positive Rules
                new ShortIngredientListRule(),
                new WholeGrainsRule(),
                new ProteinAndFiberRule(),
                new GoodOilsRule(),
                new OrganicIngredientRule(),
                new OrganicWheatRule(),
                new OrganicMilkRule()
        );
    }

    public ProductAnalysisReport analyze(ProductWithDetails productWithDetails) {
        List<AnalysisResult> allResults = new ArrayList<>();

        for (ProductAnalysisRule rule : rules) {
            List<AnalysisResult> currentRuleResults = rule.evaluate(productWithDetails);
            if (currentRuleResults != null && !currentRuleResults.isEmpty()) {
                for (AnalysisResult result : currentRuleResults) {
                    if (result != null) result.setScoringGroup(rule.getScoringGroup());
                }
                allResults.addAll(currentRuleResults);
            }
        }

        List<AnalysisResult> dedupedResults = AnalysisResultDeduplicator.deduplicate(allResults);
        int rawScore = STARTING_SCORE;
        for (AnalysisResult result : dedupedResults) {
            rawScore += result.getScoreAdjustment();
        }

        int overallScore = Math.max(MINIMUM_SCORE, Math.min(MAXIMUM_SCORE, rawScore));
        return new ProductAnalysisReport(STARTING_SCORE, rawScore, overallScore, dedupedResults);
    }

    public List<String> getRuleDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (ProductAnalysisRule rule : rules) {
            descriptions.add(rule.getRuleDescription());
        }
        return descriptions;
    }

    public Map<ProductAnalysisRule.RuleCategory, List<String>> getRuleDescriptionsByCategory() {
        Map<ProductAnalysisRule.RuleCategory, List<String>> descriptions = new LinkedHashMap<>();
        for (ProductAnalysisRule rule : rules) {
            descriptions.computeIfAbsent(rule.getRuleCategory(), ignored -> new ArrayList<>())
                    .add(rule.getRuleDescription());
        }
        return descriptions;
    }
}
