package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;
import com.example.myapplication.analysis.ProductAnalysisReport;

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
                new HighFructoseCornSyrupRule(),
                new ProcessedMeatPreservativesRule(),
                new AddedSugarRule(),
                new SugarAsMainIngredientRule(),
                new ArtificialSweetenersRule(),
                new BadVegetableOilsRule(),
                new HighSodiumRule(),
                new HighSugarRule(),
                new TextureAdditivesRule(),
                new UltraProcessedFoodRule(),
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

    // CORRECTED VERSION: The engine now evaluates all rules correctly.
    public ProductAnalysisReport analyze(ProductWithDetails productWithDetails) {
        int overallScore = 100;
        List<AnalysisResult> allResults = new ArrayList<>();

        for (ProductAnalysisRule rule : rules) {
            List<AnalysisResult> currentRuleResults = rule.evaluate(productWithDetails);
            if (currentRuleResults != null && !currentRuleResults.isEmpty()) {
                // CORRECTED: This loop now correctly iterates over the results
                // from the CURRENT rule only, applying each penalty just once.
                for (AnalysisResult result : currentRuleResults) {
                    overallScore -= result.getScorePenalty();
                }
                allResults.addAll(currentRuleResults);
            }
        }

        return new ProductAnalysisReport(Math.max(0, Math.min(100, overallScore)), allResults);
    }

    public List<String> getRuleDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (ProductAnalysisRule rule : rules) {
            descriptions.add(rule.getRuleDescription());
        }
        return descriptions;
    }
}
