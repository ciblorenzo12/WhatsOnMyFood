package com.example.myapplication.analysis;

import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.rules.AddedSugarRule;
import com.example.myapplication.analysis.rules.AnalysisResult;
import com.example.myapplication.analysis.rules.ArtificialColorsRule;
import com.example.myapplication.analysis.rules.ArtificialSweetenersRule;
import com.example.myapplication.analysis.rules.BadVegetableOilsRule;
import com.example.myapplication.analysis.rules.GoodOilsRule;
import com.example.myapplication.analysis.rules.HighFructoseCornSyrupRule;
import com.example.myapplication.analysis.rules.HighSaturatedFatRule;
import com.example.myapplication.analysis.rules.HighSodiumRule;
import com.example.myapplication.analysis.rules.MilkRule;
import com.example.myapplication.analysis.rules.NaturalFlavorRule;
import com.example.myapplication.analysis.rules.NonOrganicWheatRule;
import com.example.myapplication.analysis.rules.OrganicIngredientRule;
import com.example.myapplication.analysis.rules.OrganicWheatRule;
import com.example.myapplication.analysis.rules.PartiallyHydrogenatedOilsRule;
import com.example.myapplication.analysis.rules.ProcessedMeatPreservativesRule;
import com.example.myapplication.analysis.rules.ProductAnalysisRule;
import com.example.myapplication.analysis.rules.ProteinAndFiberRule;
import com.example.myapplication.analysis.rules.ShortIngredientListRule;
import com.example.myapplication.analysis.rules.SugarAsMainIngredientRule;
import com.example.myapplication.analysis.rules.TextureAdditivesRule;
import com.example.myapplication.analysis.rules.UltraProcessedFoodRule;
import com.example.myapplication.analysis.rules.WholeGrainsRule;

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
                new OrganicWheatRule()
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
