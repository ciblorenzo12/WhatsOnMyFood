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

        @SerializedName("ingredients_text")
        public String ingredientsText;

        public NutrimentsData nutriments;

        public IngredientsData[] ingredients;
    }

    public static class NutrimentsData {
        // Energy
        @SerializedName("energy-kcal_100g") public Double energy;
        @SerializedName("energy-kj_100g") public Double energyKj;

        // Macros
        @SerializedName("proteins_100g") public Double proteins;
        @SerializedName("carbohydrates_100g") public Double carbohydrates;
        @SerializedName("fat_100g") public Double fat;
        @SerializedName("fiber_100g") public Double fiber;

        // Sugar Types
        @SerializedName("sugars_100g") public Double sugars;
        @SerializedName("added-sugars_100g") public Double addedSugars;
        @SerializedName("sucrose_100g") public Double sucrose;
        @SerializedName("glucose_100g") public Double glucose;
        @SerializedName("fructose_100g") public Double fructose;
        @SerializedName("lactose_100g") public Double lactose;
        @SerializedName("maltose_100g") public Double maltose;
        @SerializedName("maltodextrins_100g") public Double maltodextrins;
        @SerializedName("starch_100g") public Double starch;
        @SerializedName("polyols_100g") public Double polyols;

        // Fats & Fatty Acids
        @SerializedName("saturated-fat_100g") public Double saturatedFat;
        @SerializedName("monounsaturated-fat_100g") public Double monounsaturatedFat;
        @SerializedName("polyunsaturated-fat_100g") public Double polyunsaturatedFat;
        @SerializedName("trans-fat_100g") public Double transFat;
        @SerializedName("cholesterol_100g") public Double cholesterol;
        @SerializedName("omega-3-fat_100g") public Double omega3Fat;
        @SerializedName("alpha-linolenic-acid_100g") public Double alphaLinolenicAcid;
        @SerializedName("eicosapentaenoic-acid_100g") public Double eicosapentaenoicAcid;
        @SerializedName("docosahexaenoic-acid_100g") public Double docosahexaenoicAcid;
        @SerializedName("omega-6-fat_100g") public Double omega6Fat;
        @SerializedName("linoleic-acid_100g") public Double linoleicAcid;
        @SerializedName("arachidonic-acid_100g") public Double arachidonicAcid;
        @SerializedName("gamma-linolenic-acid_100g") public Double gammaLinolenicAcid;
        @SerializedName("dihomo-gamma-linolenic-acid_100g") public Double dihomoGammaLinolenicAcid;
        @SerializedName("omega-9-fat_100g") public Double omega9Fat;
        @SerializedName("oleic-acid_100g") public Double oleicAcid;
        
        // Vitamins
        @SerializedName("vitamin-a_100g") public Double vitaminA;
        @SerializedName("vitamin-d_100g") public Double vitaminD;
        @SerializedName("vitamin-e_100g") public Double vitaminE;
        @SerializedName("vitamin-k_100g") public Double vitaminK;
        @SerializedName("vitamin-c_100g") public Double vitaminC;
        @SerializedName("vitamin-b1_100g") public Double vitaminB1;
        @SerializedName("vitamin-b2_100g") public Double vitaminB2;
        @SerializedName("vitamin-pp_100g") public Double vitaminPP;
        @SerializedName("vitamin-b6_100g") public Double vitaminB6;
        @SerializedName("vitamin-b9_100g") public Double vitaminB9;
        @SerializedName("vitamin-b12_100g") public Double vitaminB12;
        @SerializedName("biotin_100g") public Double biotin;
        @SerializedName("pantothenic-acid_100g") public Double pantothenicAcid;

        // Minerals & Trace Elements
        @SerializedName("silica_100g") public Double silica;
        @SerializedName("bicarbonate_100g") public Double bicarbonate;
        @SerializedName("potassium_100g") public Double potassium;
        @SerializedName("chloride_100g") public Double chloride;
        @SerializedName("calcium_100g") public Double calcium;
        @SerializedName("phosphorus_100g") public Double phosphorus;
        @SerializedName("iron_100g") public Double iron;
        @SerializedName("magnesium_100g") public Double magnesium;
        @SerializedName("zinc_100g") public Double zinc;
        @SerializedName("copper_100g") public Double copper;
        @SerializedName("manganese_100g") public Double manganese;
        @SerializedName("fluoride_100g") public Double fluoride;
        @SerializedName("selenium_100g") public Double selenium;
        @SerializedName("chromium_100g") public Double chromium;
        @SerializedName("molybdenum_100g") public Double molybdenum;
        @SerializedName("iodine_100g") public Double iodine;

        // Other
        @SerializedName("sodium_100g") public Double sodium;
        @SerializedName("alcohol_100g") public Double alcohol;
        @SerializedName("caffeine_100g") public Double caffeine;
        @SerializedName("taurine_100g") public Double taurine;
        @SerializedName("carbon-footprint_100g") public Double carbonFootprint;
    }

    public static class IngredientsData {
        public String text;
        public int rank;
    }
}
