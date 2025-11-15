package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "pantry",
        foreignKeys = @ForeignKey(entity = Product.class,
                                  parentColumns = "barcode",
                                  childColumns = "barcode",
                                  onDelete = ForeignKey.CASCADE))
public class Pantry {

    @PrimaryKey
    @NonNull
    public String barcode;

    public Pantry(@NonNull String barcode) {
        this.barcode = barcode;
    }
}
