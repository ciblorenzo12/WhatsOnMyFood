package com.example.myapplication.analysis.rules;

import com.example.myapplication.ProductWithDetails;
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

    public ProductAnalysisReport analyze(ProductWithDetails productWithDetails) {
        int overallScore = 100;
        List<AnalysisResult> allResults = new ArrayList<>();

        for (ProductAnalysisRule rule : rules) {
            List<AnalysisResult> results = rule.evaluate(productWithDetails);
            if (results != null && !results.isEmpty()) {
                for (AnalysisResult result : results) {
                    overallScore -= result.getScorePenalty();
                }
                allResults.addAll(results);
            }
        }

        return new ProductAnalysisReport(Math.max(0, Math.min(100, overallScore)), allResults);
    }
}
