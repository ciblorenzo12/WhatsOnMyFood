package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {

    public int status;

    @SerializedName("product")
    public ProductData product;

    public static class ProductData {
        @SerializedName("product_name")
        public String productName;

        public String brands;
        public String quantity;

        @SerializedName("image_url")
        public String imageUrl;

        public String labels;
        public String packaging;
        public String categories;

        @SerializedName("serving_size")
        public String servingSize;

        @SerializedName("nutriscore_grade")
        public String nutriscoreGrade;

        @SerializedName("nova_group")
        public String novaGroup;

        @SerializedName("ecoscore_grade")
        public String ecoscoreGrade;

        // Using the raw text field for ingredients to ensure data integrity
        @SerializedName("ingredients_text")
        public String ingredientsText;

        public NutrimentsData nutriments;

        // The ingredients array is now only a fallback
        public IngredientsData[] ingredients;
    }

    public static class NutrimentsData {
        // CORRECTED: Added all missing @SerializedName annotations
        @SerializedName("energy-kcal_100g")
        public double energy;
        @SerializedName("fat_100g")
        public double fat;
        @SerializedName("saturated-fat_100g")
        public double saturatedFat;
        @SerializedName("carbohydrates_100g")
        public double carbohydrates;
        @SerializedName("sugars_100g")
        public double sugars;
        @SerializedName("proteins_100g")
        public double proteins;
        @SerializedName("salt_100g")
        public double salt;
        @SerializedName("fiber_100g")
        public double fiber;
    }

    public static class IngredientsData {
        public String text;
        public int rank;
    }
}
