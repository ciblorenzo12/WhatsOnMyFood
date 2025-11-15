package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ingredients",
        foreignKeys = @ForeignKey(entity = Product.class,
                                  parentColumns = "barcode",
                                  childColumns = "productBarcode",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "productBarcode")})
public class Ingredient {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String productBarcode;

    public String text;
    public int rank;

    public Ingredient(@NonNull String productBarcode, String text, int rank) {
        this.productBarcode = productBarcode;
        this.text = text;
        this.rank = rank;
    }
}
