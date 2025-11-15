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

        public NutrimentsData nutriments;
        public IngredientsData[] ingredients;
    }

    public static class NutrimentsData {
        public double energy;
        public double fat;

        @SerializedName("saturated-fat")
        public double saturatedFat;

        public double carbohydrates;
        public double sugars;
        public double proteins;
        public double salt;
        public double fiber;
    }

    public static class IngredientsData {
        public String text;
        public int rank;
    }
}
