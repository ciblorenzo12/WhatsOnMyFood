package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdditiveEntryTest {

    @Test
    public void matchesAConservativeIngredientMisspelling() {
        AdditiveEntry erythritol = entry("Erythritol", "Sugar alcohol", "Sweetener");

        assertTrue(erythritol.matches("ethritol"));
        assertFalse(erythritol.matches("glycerol"));
    }

    @Test
    public void rejectsLiteralNullFieldsFromAiJson() {
        assertFalse(entry("null", "null", "null").isValid());
        assertFalse(entry("Erythritol", "null", "Sweetener").isValid());
    }

    private AdditiveEntry entry(String name, String category, String function) {
        return new AdditiveEntry(
                name,
                category,
                "",
                function,
                "Explanation",
                "Note",
                "Source",
                "https://example.com"
        );
    }
}
