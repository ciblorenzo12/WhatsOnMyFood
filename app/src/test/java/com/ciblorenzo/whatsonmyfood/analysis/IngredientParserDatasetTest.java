package com.ciblorenzo.whatsonmyfood.analysis;

import com.ciblorenzo.whatsonmyfood.IngredientOcrHeuristics;
import com.google.gson.Gson;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IngredientParserDatasetTest {

    private static final String DATASET_PATH = "ingredient-parser/ingredient_parser_cases.json";
    private static final Set<String> REQUIRED_CATEGORIES = new HashSet<>(Arrays.asList(
            "clean",
            "noisy_ocr",
            "nutrition_facts",
            "allergen_statements",
            "measurements_percentages",
            "duplicate_ingredients",
            "multilingual",
            "malformed"
    ));

    @Test
    public void datasetCasesProduceExpectedIngredientsAndAllergens() throws Exception {
        Dataset dataset = loadDataset();
        assertEquals(1, dataset.version);
        assertNotNull(dataset.cases);
        assertTrue("Dataset should contain broad coverage", dataset.cases.size() >= 20);

        Set<String> coveredCategories = new HashSet<>();
        for (ParserCase parserCase : dataset.cases) {
            coveredCategories.addAll(parserCase.categories);
            String normalizedOcr = IngredientOcrHeuristics.trimUiNoise(parserCase.input);
            IngredientTextParser.ParsedLabel actual = IngredientTextParser.parseLabel(normalizedOcr);

            assertEquals(parserCase.id + " ingredients", parserCase.expectedIngredients, actual.ingredients);
            assertEquals(parserCase.id + " contains allergens", parserCase.containsAllergens, actual.containsAllergens);
            assertEquals(parserCase.id + " may-contain allergens", parserCase.mayContainAllergens, actual.mayContainAllergens);
        }

        assertTrue(
                "Missing required dataset categories: " + missingCategories(coveredCategories),
                coveredCategories.containsAll(REQUIRED_CATEGORIES)
        );
    }

    private Dataset loadDataset() throws Exception {
        ClassLoader loader = IngredientParserDatasetTest.class.getClassLoader();
        assertNotNull(loader);
        try (InputStream stream = loader.getResourceAsStream(DATASET_PATH)) {
            assertNotNull("Missing dataset resource: " + DATASET_PATH, stream);
            return new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Dataset.class
            );
        }
    }

    private Set<String> missingCategories(Set<String> coveredCategories) {
        Set<String> missing = new HashSet<>(REQUIRED_CATEGORIES);
        missing.removeAll(coveredCategories);
        return missing;
    }

    private static final class Dataset {
        int version;
        String description;
        List<ParserCase> cases;
    }

    private static final class ParserCase {
        String id;
        List<String> categories;
        String input;
        List<String> expectedIngredients;
        List<String> containsAllergens;
        List<String> mayContainAllergens;
    }
}
