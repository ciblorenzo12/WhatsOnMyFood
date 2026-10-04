package com.ciblorenzo.whatsonmyfood.analysis.rules;

import com.ciblorenzo.whatsonmyfood.Ingredient;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class WheatSourcingRuleTest {
    private ProductWithDetails product(String labels, String ingredient) {
        ProductWithDetails p = new ProductWithDetails();
        p.product = new Product("test", "Test", null, null, null, labels,
                null, null, null, null, null, null);
        p.ingredients = Arrays.asList(new Ingredient("test", ingredient, 1));
        return p;
    }

    @Test public void productOrganicLabelsPreventContradictoryWheatWarning() {
        for (String label : Arrays.asList("USDA Organic", "en:organic", "Organic verified",
                "en:usda-organic, en:non-gmo-project-verified", "Certified ORGANIC")) {
            ProductWithDetails p = product(label, "whole grain wheat");
            assertTrue(label, new NonOrganicWheatRule().evaluate(p).isEmpty());
            assertFalse(new WholeGrainsRule().evaluate(p).isEmpty());
        }
    }

    @Test public void organicWheatIsPositiveWithoutProductLabel() {
        ProductWithDetails p = product(null, "Organic whole grain wheat");
        assertTrue(new NonOrganicWheatRule().evaluate(p).isEmpty());
        assertEquals(1, new OrganicWheatRule().evaluate(p).size());
    }

    @Test public void nonGmoAndWholeGrainDoNotImplyOrganic() {
        for (String label : Arrays.asList(null, "Non-GMO Project Verified", "Whole grain",
                "non-organic", "not organic", "organic not verified", "inorganic")) {
            assertEquals(String.valueOf(label), 1,
                    new NonOrganicWheatRule().evaluate(product(label, "whole grain wheat")).size());
        }
    }

    @Test public void explicitlyNonOrganicIngredientDoesNotReceiveOrganicBonus() {
        ProductWithDetails p = product(null, "Non-organic wheat");
        assertEquals(1, new NonOrganicWheatRule().evaluate(p).size());
        assertTrue(new OrganicWheatRule().evaluate(p).isEmpty());
        assertTrue(new OrganicIngredientRule().evaluate(p).isEmpty());
    }

    @Test public void buckwheatDoesNotMatchWheat() {
        ProductWithDetails p = product(null, "Organic buckwheat");
        assertTrue(new NonOrganicWheatRule().evaluate(p).isEmpty());
        assertTrue(new OrganicWheatRule().evaluate(p).isEmpty());
    }

    @Test public void missingIngredientEntriesAreIgnored() {
        ProductWithDetails p = product(null, null);
        p.ingredients = Arrays.asList(null, new Ingredient("test", null, 1));
        assertTrue(new NonOrganicWheatRule().evaluate(p).isEmpty());
        assertTrue(new OrganicWheatRule().evaluate(p).isEmpty());
    }

    @Test public void warningTargetsActualIngredientRatherThanEveryWheatMention() {
        ProductWithDetails p = product(null, "unbleached wheat flour");
        assertEquals("unbleached wheat flour",
                new NonOrganicWheatRule().evaluate(p).get(0).getTriggeringIngredient());
    }
}
