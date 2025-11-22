package com.example.myapplication;

import androidx.room.Embedded;
import androidx.room.Relation;

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
}
