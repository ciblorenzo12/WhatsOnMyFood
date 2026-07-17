package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IngredientLabelValidatorTest {

    @Test
    public void acceptsTenRepresentativePackagedFoodLabels() {
        List<String> samples = Arrays.asList(
                "INGREDIENTS: Sugar, palm oil, hazelnuts, skim milk powder, cocoa, soy lecithin, vanillin.",
                "Ingredients: Carbonated water, high fructose corn syrup, caramel color, phosphoric acid, natural flavors, caffeine.",
                "INGREDIENTS: Wheat flour, sugar, palm oil, cocoa powder, glucose syrup, salt.",
                "Ingredients: Wheat flour, sugar, palm oil, hazelnuts, cocoa, skim milk powder, butter.",
                "Ingredients: Skimmed milk, sugar, glucose syrup, cream, cocoa powder, stabilizer guar gum.",
                "Ingredients: Basil, sunflower oil, cashews, Parmigiano Reggiano cheese, salt, garlic.",
                "Ingredients: Purified water, magnesium sulfate, potassium bicarbonate.",
                "Ingredients: Corn, vegetable oil, maltodextrin, cheddar cheese, salt, natural flavor.",
                "Ingredients: Whole grain oats, corn starch, sugar, salt, tripotassium phosphate.",
                "Ingredients: Dried potatoes, vegetable oil, rice flour, wheat starch, salt."
        );

        for (String sample : samples) {
            IngredientLabelValidator.Result result = IngredientLabelValidator.validate(sample);
            assertTrue(sample, result.readable);
            assertTrue(sample, result.ingredients.size() >= 3);
        }
    }

    @Test
    public void acceptsFrenchAndSpanishIngredientHeadings() {
        IngredientLabelValidator.Result french = IngredientLabelValidator.validate(
                "Ingrédients : farine de blé, sucre, huile de palme, cacao. Allergènes : blé."
        );
        IngredientLabelValidator.Result spanish = IngredientLabelValidator.validate(
                "Ingredientes: maíz, aceite vegetal, sal, queso cheddar. Alérgenos: leche."
        );

        assertTrue(french.readable);
        assertEquals("farine de blé", french.ingredients.get(0));
        assertTrue(spanish.readable);
        assertEquals("maíz", spanish.ingredients.get(0));
    }

    @Test
    public void acceptsCorrectedLowQualityOcr() {
        IngredientLabelValidator.Result result = IngredientLabelValidator.validate(
                "INGRED1ENTS: whole grain oat-\n flour, sugar, corn starch, salt"
        );

        assertTrue(result.readable);
        assertTrue(result.cleanedText.startsWith("ingredients:"));
        assertEquals(4, result.ingredients.size());
    }

    @Test
    public void rejectsEmptyHeadingNutritionOnlyAndOcrFailure() {
        assertEquals(
                IngredientLabelValidator.FailureReason.HEADING_WITHOUT_INGREDIENTS,
                IngredientLabelValidator.validate("INGREDIENTS:").failureReason
        );
        assertEquals(
                IngredientLabelValidator.FailureReason.NUTRITION_PANEL_ONLY,
                IngredientLabelValidator.validate("Nutrition Facts Calories 150 Serving Size 1 cup Daily Value 5%").failureReason
        );
        assertEquals(
                IngredientLabelValidator.FailureReason.OCR_PLACEHOLDER,
                IngredientLabelValidator.validate("Image upload (OCR failed)").failureReason
        );
        assertFalse(IngredientLabelValidator.validate("NET WT 12 OZ KEEP REFRIGERATED").readable);
    }

    @Test
    public void permitsSingleIngredientLabelButNotUnlabeledMarketingText() {
        assertTrue(IngredientLabelValidator.validate("Ingredients: Water").readable);
        assertFalse(IngredientLabelValidator.validate("Pure refreshing natural spring water").readable);
    }
}
