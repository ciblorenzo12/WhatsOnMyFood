package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class IngredientListSanitizerTest {

    @Test
    public void removesHeadingsAndRepairsSeparatedVitaminFormsFromCachedList() {
        List<String> cached = Arrays.asList(
                "Oats", "Corn starch", "Sugar (Added Sugar)", "Salt", "Tripotassium phosphate",
                "Vitamin e", "Added to preserve freshness", "Vitamins", "Minerals", "Iron", "Zinc",
                "Vitamin c", "A b vitamin", "Vitamin b6", "Pyridoxine hydrochloride", "Vitamin a",
                "Vitamin b1", "A b vitamins", "Folic acid", "Vitamin b12", "Vitamin d3", "Vitamin d3"
        );
        List<Ingredient> ingredients = new ArrayList<>();
        for (int i = 0; i < cached.size(); i++) {
            ingredients.add(new Ingredient("016000275263", cached.get(i), i));
        }

        List<String> cleaned = IngredientListSanitizer.sanitize(ingredients).stream()
                .map(value -> value.text)
                .collect(Collectors.toList());

        assertEquals(
                Arrays.asList(
                        "Oats", "Corn starch", "Sugar (Added Sugar)", "Salt", "Tripotassium phosphate",
                        "Vitamin E", "Iron", "Zinc", "Vitamin C",
                        "Vitamin B6 (pyridoxine hydrochloride)", "Vitamin A", "Vitamin B1",
                        "B vitamin (folic acid)", "Vitamin B12", "Vitamin D3"
                ),
                cleaned
        );
    }

    @Test
    public void preservesDistinctVitaminsAndOrdinaryIngredients() {
        List<Ingredient> ingredients = Arrays.asList(
                new Ingredient("1", "Vitamin a", 0),
                new Ingredient("1", "Vitamin c", 1),
                new Ingredient("1", "Vitamin d3", 2),
                new Ingredient("1", "Black currant", 3)
        );

        assertEquals(4, IngredientListSanitizer.sanitize(ingredients).size());
    }
}
