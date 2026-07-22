package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.analysis.AnalysisResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Applies the product policy for vegetable oils other than olive, avocado, and coconut oil. */
public class JunkOilsRule implements ProductAnalysisRule {

    private static final List<String> PENALIZED_OIL_MARKERS = Arrays.asList(
            "vegetable", "seed", "canola", "rapeseed", "sunflower", "safflower", "soybean",
            "corn", "cottonseed", "palm", "grapeseed", "rice bran", "peanut",
            "sesame", "flaxseed", "linseed", "hemp", "mustard", "walnut"
    );
    private static final List<String> EXEMPT_OILS = Arrays.asList("olive oil", "avocado oil", "coconut oil");
    private static final List<String> NON_VEGETABLE_OILS = Arrays.asList("fish oil", "cod liver oil", "krill oil", "algal oil");
    private static final String EXPLANATION = "The ingredient list contains a vegetable oil other than olive, avocado, or coconut oil. Under this app's oil policy, that produces one oil penalty even if several vegetable oils are listed.";

    @Override
    public List<AnalysisResult> evaluate(ProductWithDetails productWithDetails) {
        List<AnalysisResult> results = new ArrayList<>();
        if (productWithDetails == null || productWithDetails.ingredients == null) return results;

        for (Ingredient ingredient : productWithDetails.ingredients) {
            if (ingredient == null || ingredient.text == null) continue;
            String normalized = ingredient.text.toLowerCase(Locale.US);
            if (normalized.contains("shortening")) {
                results.add(resultFor(ingredient.text));
                return results;
            }
            if (!normalized.contains("oil")) continue;

            for (String marker : PENALIZED_OIL_MARKERS) {
                if (normalized.contains(marker)) {
                    results.add(resultFor(ingredient.text));
                    return results;
                }
            }

            if (!containsAny(normalized, EXEMPT_OILS) && !containsAny(normalized, NON_VEGETABLE_OILS)) {
                // Unknown food oils default to the vegetable-oil policy unless explicitly exempted.
                results.add(resultFor(ingredient.text));
                return results;
            }
        }
        return results;
    }

    private boolean containsAny(String ingredient, List<String> terms) {
        for (String term : terms) {
            if (ingredient.contains(term)) return true;
        }
        return false;
    }

    private AnalysisResult resultFor(String ingredient) {
        return new AnalysisResult(
                "Contains vegetable oil",
                AnalysisResult.WarningLevel.WARNING,
                15,
                ingredient,
                EXPLANATION
        );
    }

    @Override
    public String getRuleDescription() {
        return "Vegetable oil: subtracts 15 points when an oil other than olive, avocado, or coconut oil is listed. Multiple vegetable oils count as one penalty.";
    }

    @Override
    public RuleCategory getRuleCategory() {
        return RuleCategory.OILS;
    }
}
