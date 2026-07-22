package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtificialColorsRule implements ProductAnalysisRule {

    private static final List<String> ARTIFICIAL_COLORS = Arrays.asList(
            "red 40", "yellow 5", "yellow 6", "blue 1", "blue 2", "green 3", "caramel", "150a", "150b", "150c", "150d"
    );

    private static final String EXPLANATION = "❌ Contains artificial food dyes like Red 40 / Yellow 5. These are synthetic colors often added to make ultra-processed foods more appealing. Some studies and regulatory reviews have raised concerns about their potential impact on sensitive children (e.g., hyperactivity) and they don’t add any nutritional value. Choosing products colored with real foods (like paprika, beet juice, turmeric) is usually a safer bet.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String color : ARTIFICIAL_COLORS) {
                        if (lowerCaseIngredient.contains(color)) {
                            results.add(new AnalysisResult("Contains " + color, AnalysisResult.WarningLevel.SEVERE, 20, ingredient.text, EXPLANATION));
                        }
                    }
                }
            }
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Artificial color: subtracts 20 points when a listed dye or caramel-color term is found. Multiple color matches are reported as one scoring penalty.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.ADDITIVES_AND_PRESERVATIVES;
    }
}
