package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {

    @PrimaryKey
    @NonNull
    public String barcode;

    public String productName;
    public String brands;
    public String quantity;
    public String imageUrl;
    public String labels;
    public String packaging;
    public String categories;
    public String servingSize;
    public String nutriscoreGrade;
    public String novaGroup;
    public String ecoscoreGrade;

    public Product(@NonNull String barcode, String productName, String brands, String quantity, String imageUrl, String labels, String packaging, String categories, String servingSize, String nutriscoreGrade, String novaGroup, String ecoscoreGrade) {
        this.barcode = barcode;
        this.productName = productName;
        this.brands = brands;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.labels = labels;
        this.packaging = packaging;
        this.categories = categories;
        this.servingSize = servingSize;
        this.nutriscoreGrade = nutriscoreGrade;
        this.novaGroup = novaGroup;
        this.ecoscoreGrade = ecoscoreGrade;
    }
}
