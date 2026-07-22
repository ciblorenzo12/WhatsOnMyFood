package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IngredientNormalizerTest {

    @Test
    public void normalizesCapitalizationSpacesAndBoundaryPunctuation() {
        assertEquals("cane sugar", IngredientNormalizer.normalize("  CANE   SUGAR.;  "));
        assertEquals("rose water", IngredientNormalizer.normalize("*** Rose Water !!!"));
        assertEquals("cocoa", IngredientNormalizer.normalize("cocoa)"));
    }

    @Test
    public void mergesOnlyExactNormalizedDuplicates() {
        List<String> normalized = IngredientNormalizer.normalizeAndDeduplicate(Arrays.asList(
                "Water", " water. ", "WATER", "Cane-Sugar", "cane sugar"
        ));

        assertEquals(Arrays.asList("water", "cane sugar"), normalized);
    }

    @Test
    public void preservesAndNormalizesMeaningfulParentheticalIngredients() {
        assertEquals(
                "chocolate (sugar, cocoa butter, milk powder)",
                IngredientNormalizer.normalize("Chocolate ( Sugar,Cocoa Butter,  Milk Powder )")
        );
    }

    @Test
    public void standardizesCommonAdditiveAliases() {
        List<String> normalized = IngredientNormalizer.normalizeAndDeduplicate(Arrays.asList(
                "E 330", "Citric Acid (E330)", "MSG", "monosodium glutamate (MSG)", "E-415", "Baking Soda"
        ));

        assertEquals(
                Arrays.asList("citric acid", "monosodium glutamate", "xanthan gum", "sodium bicarbonate"),
                normalized
        );
    }

    @Test
    public void doesNotCombineDifferentIngredients() {
        List<String> ingredients = Arrays.asList(
                "sugar",
                "brown sugar",
                "cocoa",
                "cocoa butter",
                "milk",
                "milk powder",
                "palm oil",
                "palm kernel oil",
                "natural flavor (vanilla)",
                "natural flavor (lemon)",
                "olive oil",
                "olive oil 5%"
        );

        assertEquals(ingredients, IngredientNormalizer.normalizeAndDeduplicate(ingredients));
    }
}
