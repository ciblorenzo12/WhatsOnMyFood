package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutriments",
        foreignKeys = @ForeignKey(entity = Product.class,
                                  parentColumns = "barcode",
                                  childColumns = "productBarcode",
                                  onDelete = ForeignKey.CASCADE))
public class Nutriments {

    @PrimaryKey
    @NonNull
    public String productBarcode;

    public double energy;
    public double fat;
    public double saturatedFat;
    public double carbohydrate;
    public double sugar;
    public double proteins;
    public double salt;
    public double fiber;

    public Nutriments(@NonNull String productBarcode, double energy, double fat, double saturatedFat, double carbohydrate, double sugar, double proteins, double salt, double fiber) {
        this.productBarcode = productBarcode;
        this.energy = energy;
        this.fat = fat;
        this.saturatedFat = saturatedFat;
        this.carbohydrate = carbohydrate;
        this.sugar = sugar;
        this.proteins = proteins;
        this.salt = salt;
        this.fiber = fiber;
    }
}
