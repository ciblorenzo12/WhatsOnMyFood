package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResultDeduplicator;
import com.ciblorenzo.whatsonmyfood.analysis.ProductAnalysisReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RuleEngine {

    private final List<ProductAnalysisRule> rules;

    public RuleEngine() {
        this.rules = Arrays.asList(
                // Negative Rules
                new ArtificialColorsRule(),
                new PartiallyHydrogenatedOilsRule(),
                new TransFatNutritionRule(),
                new HighFructoseCornSyrupRule(),
                new ProcessedMeatPreservativesRule(),
                new PreservativesRule(),
                new AddedSugarRule(),
                new SugarAsMainIngredientRule(),
                new RefinedFlourRule(),
                new ArtificialSweetenersRule(),
                new BadVegetableOilsRule(),
                new HighSodiumRule(),
                new HighSugarRule(),
                new TextureAdditivesRule(),
                new ArtificialFlavorRule(),
                new UltraProcessedFoodRule(),
                new NutriScoreRule(),
                new HighSaturatedFatRule(),
                new NonOrganicWheatRule(),
                new MilkRule(),

                // Informational / Warning Rules
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
                allResults.addAll(currentRuleResults);
            }
        }

        List<AnalysisResult> dedupedResults = AnalysisResultDeduplicator.deduplicate(allResults);
        int overallScore = 100;
        for (AnalysisResult result : dedupedResults) {
            overallScore -= result.getScorePenalty();
        }

        return new ProductAnalysisReport(Math.max(0, Math.min(100, overallScore)), dedupedResults);
    }

    public List<String> getRuleDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (ProductAnalysisRule rule : rules) {
            descriptions.add(rule.getRuleDescription());
        }
        return descriptions;
    }
}
