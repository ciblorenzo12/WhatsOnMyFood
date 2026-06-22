package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "ingredients",
        foreignKeys = @ForeignKey(entity = Product.class,
                                  parentColumns = "barcode",
                                  childColumns = "barcode",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "barcode")})
public class Ingredient {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String barcode;

    public String text;
    public int rank;

    public Ingredient(@NonNull String barcode, String text, int rank) {
        this.barcode = barcode;
        this.text = text;
        this.rank = rank;
    }
}
