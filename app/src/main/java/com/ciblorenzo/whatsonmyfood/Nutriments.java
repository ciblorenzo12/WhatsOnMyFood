package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "nutriments")
public class Nutriments {

    @PrimaryKey
    @NonNull
    public String barcode;

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

    @SerializedName("alcohol")
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

    public Nutriments(@NonNull String barcode, Double energy, Double energyKj, Double fat, Double saturatedFat, Double monounsaturatedFat, Double polyunsaturatedFat, Double transFat, Double cholesterol, Double carbohydrates, Double sugars, Double addedSugars, Double sucrose, Double glucose, Double fructose, Double lactose, Double maltose, Double maltodextrins, Double starch, Double polyols, Double fiber, Double proteins, Double salt, Double sodium, Double alcohol, Double vitaminA, Double vitaminD, Double vitaminE, Double vitaminK, Double vitaminC, Double vitaminB1, Double vitaminB2, Double vitaminPP, Double vitaminB6, Double vitaminB9, Double vitaminB12, Double biotin, Double pantothenicAcid, Double silica, Double bicarbonate, Double potassium, Double chloride, Double calcium, Double phosphorus, Double iron, Double magnesium, Double zinc, Double copper, Double manganese, Double fluoride, Double selenium, Double chromium, Double molybdenum, Double iodine, Double caffeine, Double taurine, Double omega3Fat, Double omega6Fat, Double omega9Fat, Double oleicAcid, Double linoleicAcid, Double gammaLinolenicAcid, Double dihomoGammaLinolenicAcid, Double arachidonicAcid, Double alphaLinolenicAcid, Double eicosapentaenoicAcid, Double docosahexaenoicAcid, Double carbonFootprint) {
        this.barcode = barcode;
        this.energy = energy;
        this.energyKj = energyKj;
        this.fat = fat;
        this.saturatedFat = saturatedFat;
        this.monounsaturatedFat = monounsaturatedFat;
        this.polyunsaturatedFat = polyunsaturatedFat;
        this.transFat = transFat;
        this.cholesterol = cholesterol;
        this.carbohydrates = carbohydrates;
        this.sugars = sugars;
        this.addedSugars = addedSugars;
        this.sucrose = sucrose;
        this.glucose = glucose;
        this.fructose = fructose;
        this.lactose = lactose;
        this.maltose = maltose;
        this.maltodextrins = maltodextrins;
        this.starch = starch;
        this.polyols = polyols;
        this.fiber = fiber;
        this.proteins = proteins;
        this.salt = salt;
        this.sodium = sodium;
        this.alcohol = alcohol;
        this.vitaminA = vitaminA;
        this.vitaminD = vitaminD;
        this.vitaminE = vitaminE;
        this.vitaminK = vitaminK;
        this.vitaminC = vitaminC;
        this.vitaminB1 = vitaminB1;
        this.vitaminB2 = vitaminB2;
        this.vitaminPP = vitaminPP;
        this.vitaminB6 = vitaminB6;
        this.vitaminB9 = vitaminB9;
        this.vitaminB12 = vitaminB12;
        this.biotin = biotin;
        this.pantothenicAcid = pantothenicAcid;
        this.silica = silica;
        this.bicarbonate = bicarbonate;
        this.potassium = potassium;
        this.chloride = chloride;
        this.calcium = calcium;
        this.phosphorus = phosphorus;
        this.iron = iron;
        this.magnesium = magnesium;
        this.zinc = zinc;
        this.copper = copper;
        this.manganese = manganese;
        this.fluoride = fluoride;
        this.selenium = selenium;
        this.chromium = chromium;
        this.molybdenum = molybdenum;
        this.iodine = iodine;
        this.caffeine = caffeine;
        this.taurine = taurine;
        this.omega3Fat = omega3Fat;
        this.omega6Fat = omega6Fat;
        this.omega9Fat = omega9Fat;
        this.oleicAcid = oleicAcid;
        this.linoleicAcid = linoleicAcid;
        this.gammaLinolenicAcid = gammaLinolenicAcid;
        this.dihomoGammaLinolenicAcid = dihomoGammaLinolenicAcid;
        this.arachidonicAcid = arachidonicAcid;
        this.alphaLinolenicAcid = alphaLinolenicAcid;
        this.eicosapentaenoicAcid = eicosapentaenoicAcid;
        this.docosahexaenoicAcid = docosahexaenoicAcid;
        this.carbonFootprint = carbonFootprint;
    }

    @Override
    public String toString() {
        return "Energy: " + energy + "kcal, Fat: " + fat + "g, SatFat: " + saturatedFat + 
               "g, Carbs: " + carbohydrates + "g, Sugars: " + sugars + "g, Protein: " + proteins + "g, Salt: " + salt + "g";
    }
}
