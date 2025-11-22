package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextureAdditivesRule implements ProductAnalysisRule {

    private static final List<String> ADDITIVES = Arrays.asList("carboxymethylcellulose", "polysorbate 80", "lecithins", "mono- and diglycerides", "xanthan gum", "guar gum", "carrageenan", "gellan gum", "locust bean gum");
    private static final String EXPLANATION = "⚠️ Contains several texture additives (gums, emulsifiers). These help keep ultra-processed foods smooth and shelf-stable, but they don’t add nutrition. Some emulsifiers (like carrageenan or certain cellulose derivatives) have been questioned in research for potential gut irritation in sensitive individuals. If a long list of gums/emulsifiers is present, it’s usually a sign of heavy processing.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails != null && productWithDetails.ingredients != null) {
            int additiveCount = 0;
            List<String> foundAdditives = new ArrayList<>();
            for (Ingredient ingredient : productWithDetails.ingredients) {
                if (ingredient.text != null) {
                    String lowerCaseIngredient = ingredient.text.toLowerCase();
                    for (String additive : ADDITIVES) {
                        if (lowerCaseIngredient.contains(additive) && !foundAdditives.contains(additive)) {
                            additiveCount++;
                            foundAdditives.add(additive);
                        }
                    }
                }
            }
            if (additiveCount > 2) { // Trigger if more than 2 are found
                results.add(new AnalysisResult("Contains multiple texture additives", AnalysisResult.WarningLevel.WARNING, 10, String.join(", ", foundAdditives), EXPLANATION));
            }
        }
        return results;
    }
}
