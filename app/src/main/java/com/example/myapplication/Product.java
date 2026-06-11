package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
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
    public String aiInsight;
    public Integer healthScore;
    @ColumnInfo(defaultValue = "0")
    public Integer userIngredientRiskScore;

    @ColumnInfo(defaultValue = "0")
    public boolean isFavorite;

    public boolean isValid() {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        if (productName == null || productName.trim().isEmpty()) return false;
        
        String lowerName = productName.toLowerCase().trim();
        if (lowerName.equals("scanned product") || 
            lowerName.equals("unknown product") || 
            lowerName.equals("name") ||
            lowerName.equals("product name") ||
            lowerName.equals("brand unknown") ||
            lowerName.equals("brand")) return false;
            
        // Stricter check: if the name is very short and doesn't look like a product
        if (productName.length() < 2) return false;
        
        return true;
    }

    public Product(@NonNull String barcode, String productName, String brands, String quantity, String imageUrl, String labels, String packaging, String categories, String servingSize, String nutriscoreGrade, String novaGroup, String ecoscoreGrade, String aiInsight, Integer healthScore, Integer userIngredientRiskScore) {
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
        this.aiInsight = aiInsight;
        this.healthScore = healthScore;
        this.userIngredientRiskScore = userIngredientRiskScore == null ? 0 : userIngredientRiskScore;
        this.isFavorite = false; // Default value for new products
    }

    @Ignore
    public Product(@NonNull String barcode, String productName, String brands, String quantity, String imageUrl, String labels, String packaging, String categories, String servingSize, String nutriscoreGrade, String novaGroup, String ecoscoreGrade, String aiInsight, Integer healthScore) {
        this(barcode, productName, brands, quantity, imageUrl, labels, packaging, categories, servingSize, nutriscoreGrade, novaGroup, ecoscoreGrade, aiInsight, healthScore, 0);
    }

    @Ignore
    public Product(@NonNull String barcode, String productName, String brands, String quantity, String imageUrl, String labels, String packaging, String categories, String servingSize, String nutriscoreGrade, String novaGroup, String ecoscoreGrade, String aiInsight) {
        this(barcode, productName, brands, quantity, imageUrl, labels, packaging, categories, servingSize, nutriscoreGrade, novaGroup, ecoscoreGrade, aiInsight, null);
    }

    @Ignore
    public Product(@NonNull String barcode, String productName, String brands, String quantity, String imageUrl, String labels, String packaging, String categories, String servingSize, String nutriscoreGrade, String novaGroup, String ecoscoreGrade) {
        this(barcode, productName, brands, quantity, imageUrl, labels, packaging, categories, servingSize, nutriscoreGrade, novaGroup, ecoscoreGrade, null, null);
    }
}
