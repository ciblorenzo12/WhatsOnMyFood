package com.ciblorenzo.whatsonmyfood;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.Collections;
import java.util.List;

public class ProductWithDetails {

    @Embedded
    public Product product;

    @Relation(
            parentColumn = "barcode",
            entityColumn = "barcode"
    )
    public Nutriments nutriments;

    @Relation(
            parentColumn = "barcode",
            entityColumn = "barcode"
    )
    public List<Ingredient> ingredients;

    /** Parsed label statements supplied to the analysis layer, kept separate from ingredients. */
    @Ignore
    public List<String> containsAllergens = Collections.emptyList();

    /** Precautionary cross-contact statements supplied to the analysis layer. */
    @Ignore
    public List<String> mayContainAllergens = Collections.emptyList();
}
