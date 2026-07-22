package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.List;

/** Surfaces label allergen statements without treating them as scored ingredients. */
public class AllergenStatementsRule implements ProductAnalysisRule {

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null) return results;

        if (productWithDetails.containsAllergens != null && !productWithDetails.containsAllergens.isEmpty()) {
            results.add(new AnalysisResult(
                    "Allergen statement: Contains",
                    AnalysisResult.WarningLevel.INFO,
                    0,
                    String.join(", ", productWithDetails.containsAllergens),
                    "The package explicitly lists these allergens. This statement is kept separate from the ingredient list and does not change the health score."
            ));
        }
        if (productWithDetails.mayContainAllergens != null && !productWithDetails.mayContainAllergens.isEmpty()) {
            results.add(new AnalysisResult(
                    "Allergen advisory: May contain",
                    AnalysisResult.WarningLevel.INFO,
                    0,
                    String.join(", ", productWithDetails.mayContainAllergens),
                    "The package gives a precautionary cross-contact warning. This statement is kept separate from the ingredient list and does not change the health score."
            ));
        }
        return results;
    }

    @Override
    public String getRuleDescription() {
        return "Allergen statements: reports Contains and May contain warnings as information with a 0-point score effect; allergen warnings are never added to the ingredient list.";
    }

    @Override
    public String getScoringGroup() {
        // Both statement types must remain visible, so their distinct messages drive deduplication.
        return "";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.ALLERGENS;
    }
}
