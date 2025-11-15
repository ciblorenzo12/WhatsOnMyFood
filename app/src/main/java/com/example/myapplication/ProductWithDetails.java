package com.example.myapplication;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class ProductWithDetails {

    @Embedded
    public Product product;

    @Relation(
            parentColumn = "barcode",
            entityColumn = "productBarcode"
    )
    public Nutriments nutriments;

    @Relation(
            parentColumn = "barcode",
            entityColumn = "productBarcode"
    )
    public List<Ingredient> ingredients;
}
