package com.example.myapplication.analysis.rules;

import com.example.myapplication.Ingredient;
import com.example.myapplication.ProductWithDetails;
import com.example.myapplication.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextureAdditivesRule implements ProductAnalysisRule {

    private static final List<String> ADDITIVES = Arrays.asList(
            "carboxymethylcellulose", "polysorbate 80", "lecithins", "mono- and diglycerides", "xanthan gum",
            "guar gum", "carrageenan", "gellan gum", "locust bean gum"
    );
    private static final String EXPLANATION = "Contains several texture additives such as gums or emulsifiers. These can be useful for texture, but several together often point to a more engineered product.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) {
            return results;
        }

        List<String> foundAdditives = new ArrayList<>();
        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String lowerCaseIngredient = ingredient.text.toLowerCase();
            for (String additive : ADDITIVES) {
                if (lowerCaseIngredient.contains(additive) && !foundAdditives.contains(additive)) {
                    foundAdditives.add(additive);
                }
            }
        }

        if (foundAdditives.size() > 2) {
            results.add(new AnalysisResult("Contains multiple texture additives", AnalysisResult.WarningLevel.WARNING, 10, String.join(", ", foundAdditives), EXPLANATION));
        }

        return results;
    }
}
