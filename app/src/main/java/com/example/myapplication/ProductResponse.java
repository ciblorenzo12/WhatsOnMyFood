package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {
    public int status;
    public ProductData product;

    public static class ProductData {
        @SerializedName("product_name")
        public String productName;
        @SerializedName("product_name_en")
        public String productNameEn;
        @SerializedName("product_name_es")
        public String productNameEs;
        @SerializedName("product_name_fr")
        public String productNameFr;
        @SerializedName("generic_name")
        public String genericName;
        @SerializedName("generic_name_en")
        public String genericNameEn;
        @SerializedName("generic_name_es")
        public String genericNameEs;
        @SerializedName("generic_name_fr")
        public String genericNameFr;
        public String brands;
        public String quantity;
        @SerializedName("image_url")
        public String imageUrl;
        @SerializedName("image_front_url")
        public String imageFrontUrl;
        public String labels;
        @SerializedName("labels_en")
        public String labelsEn;
        @SerializedName("labels_es")
        public String labelsEs;
        @SerializedName("labels_fr")
        public String labelsFr;
        @SerializedName("labels_tags")
        public String[] labelsTags;
        public String packaging;
        @SerializedName("packaging_en")
        public String packagingEn;
        @SerializedName("packaging_es")
        public String packagingEs;
        @SerializedName("packaging_fr")
        public String packagingFr;
        public String categories;
        @SerializedName("categories_en")
        public String categoriesEn;
        @SerializedName("categories_es")
        public String categoriesEs;
        @SerializedName("categories_fr")
        public String categoriesFr;
        public String countries;
        @SerializedName("countries_tags")
        public String[] countriesTags;
        public String lang;
        @SerializedName("serving_size")
        public String servingSize;
        @SerializedName("nutriscore_grade")
        public String nutriscoreGrade;
        @SerializedName("nova_group")
        public String novaGroup;
        @SerializedName("ecoscore_grade")
        public String ecoscoreGrade;
        public NutrimentsData nutriments;
        @SerializedName("ingredients_text")
        public String ingredientsText;
        @SerializedName("ingredients_text_en")
        public String ingredientsTextEn;
        @SerializedName("ingredients_text_es")
        public String ingredientsTextEs;
        @SerializedName("ingredients_text_fr")
        public String ingredientsTextFr;
        @SerializedName("ingredients_text_with_allergens_en")
        public String ingredientsTextWithAllergensEn;
        @SerializedName("ingredients_text_with_allergens")
        public String ingredientsTextWithAllergens;
        @SerializedName("ingredients_text_with_allergens_es")
        public String ingredientsTextWithAllergensEs;
        @SerializedName("ingredients_text_with_allergens_fr")
        public String ingredientsTextWithAllergensFr;
        public String allergens;
        @SerializedName("allergens_tags")
        public String[] allergensTags;

        public IngredientsData[] ingredients;
    }

    public static class NutrimentsData {
        @SerializedName("energy-kcal_100g")
        public Double energy;
        @SerializedName("energy-kj_100g")
        public Double energyKj;
        @SerializedName("fat_100g")
        public Double fat;
        @SerializedName("saturated-fat_100g")
        public Double saturatedFat;
        @SerializedName("monounsaturated-fat_100g")
        public Double monounsaturatedFat;
        @SerializedName("polyunsaturated-fat_100g")
        public Double polyunsaturatedFat;
        @SerializedName("trans-fat_100g")
        public Double transFat;
        @SerializedName("cholesterol_100g")
        public Double cholesterol;
        @SerializedName("carbohydrates_100g")
        public Double carbohydrates;
        @SerializedName("sugars_100g")
        public Double sugars;
        @SerializedName("added-sugars_100g")
        public Double addedSugars;
        @SerializedName("sucrose_100g")
        public Double sucrose;
        @SerializedName("glucose_100g")
        public Double glucose;
        @SerializedName("fructose_100g")
        public Double fructose;
        @SerializedName("lactose_100g")
        public Double lactose;
        @SerializedName("maltose_100g")
        public Double maltose;
        @SerializedName("maltodextrins_100g")
        public Double maltodextrins;
        @SerializedName("starch_100g")
        public Double starch;
        @SerializedName("polyols_100g")
        public Double polyols;
        @SerializedName("fiber_100g")
        public Double fiber;
        @SerializedName("proteins_100g")
        public Double proteins;
        @SerializedName("salt_100g")
        public Double salt;
        @SerializedName("sodium_100g")
        public Double sodium;
        public Double alcohol;
        @SerializedName("vitamin-a_100g")
        public Double vitaminA;
        @SerializedName("vitamin-d_100g")
        public Double vitaminD;
        @SerializedName("vitamin-e_100g")
        public Double vitaminE;
        @SerializedName("vitamin-k_100g")
        public Double vitaminK;
        @SerializedName("vitamin-c_100g")
        public Double vitaminC;
        @SerializedName("vitamin-b1_100g")
        public Double vitaminB1;
        @SerializedName("vitamin-b2_100g")
        public Double vitaminB2;
        @SerializedName("vitamin-pp_100g")
        public Double vitaminPP;
        @SerializedName("vitamin-b6_100g")
        public Double vitaminB6;
        @SerializedName("vitamin-b9_100g")
        public Double vitaminB9;
        @SerializedName("vitamin-b12_100g")
        public Double vitaminB12;
        @SerializedName("biotin_100g")
        public Double biotin;
        @SerializedName("pantothenic-acid_100g")
        public Double pantothenicAcid;
        @SerializedName("silica_100g")
        public Double silica;
        @SerializedName("bicarbonate_100g")
        public Double bicarbonate;
        @SerializedName("potassium_100g")
        public Double potassium;
        @SerializedName("chloride_100g")
        public Double chloride;
        @SerializedName("calcium_100g")
        public Double calcium;
        @SerializedName("phosphorus_100g")
        public Double phosphorus;
        @SerializedName("iron_100g")
        public Double iron;
        @SerializedName("magnesium_100g")
        public Double magnesium;
        @SerializedName("zinc_100g")
        public Double zinc;
        @SerializedName("copper_100g")
        public Double copper;
        @SerializedName("manganese_100g")
        public Double manganese;
        @SerializedName("fluoride_100g")
        public Double fluoride;
        @SerializedName("selenium_100g")
        public Double selenium;
        @SerializedName("chromium_100g")
        public Double chromium;
        @SerializedName("molybdenum_100g")
        public Double molybdenum;
        @SerializedName("iodine_100g")
        public Double iodine;
        @SerializedName("caffeine_100g")
        public Double caffeine;
        @SerializedName("taurine_100g")
        public Double taurine;
        @SerializedName("omega-3-fat_100g")
        public Double omega3Fat;
        @SerializedName("omega-6-fat_100g")
        public Double omega6Fat;
        @SerializedName("omega-9-fat_100g")
        public Double omega9Fat;
        @SerializedName("oleic-acid_100g")
        public Double oleicAcid;
        @SerializedName("linoleic-acid_100g")
        public Double linoleicAcid;
        @SerializedName("gamma-linolenic-acid_100g")
        public Double gammaLinolenicAcid;
        @SerializedName("dihomo-gamma-linolenic-acid_100g")
        public Double dihomoGammaLinolenicAcid;
        @SerializedName("arachidonic-acid_100g")
        public Double arachidonicAcid;
        @SerializedName("alpha-linolenic-acid_100g")
        public Double alphaLinolenicAcid;
        @SerializedName("eicosapentaenoic-acid_100g")
        public Double eicosapentaenoicAcid;
        @SerializedName("docosahexaenoic-acid_100g")
        public Double docosahexaenoicAcid;
        @SerializedName("carbon-footprint_100g")
        public Double carbonFootprint;
    }

    public static class IngredientsData {
        public String id;
        public int rank;
        public String text;
    }
}
