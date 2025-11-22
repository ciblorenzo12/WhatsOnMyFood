package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "nutriments")
public class Nutriments {

    @PrimaryKey
    @NonNull
    public String barcode;

    // Energy
    public Double energy;
    public Double energyKj;

    // Macros
    public Double proteins;
    public Double carbohydrates;
    public Double fat;
    public Double fiber;

    // Sugar Types
    public Double sugars;
    public Double addedSugars;
    public Double sucrose;
    public Double glucose;
    public Double fructose;
    public Double lactose;
    public Double maltose;
    public Double maltodextrins;
    public Double starch;
    public Double polyols;

    // Fats & Fatty Acids
    public Double saturatedFat;
    public Double monounsaturatedFat;
    public Double polyunsaturatedFat;
    public Double transFat;
    public Double cholesterol;
    public Double omega3Fat;
    public Double alphaLinolenicAcid;
    public Double eicosapentaenoicAcid;
    public Double docosahexaenoicAcid;
    public Double omega6Fat;
    public Double linoleicAcid;
    public Double arachidonicAcid;
    public Double gammaLinolenicAcid;
    public Double dihomoGammaLinolenicAcid;
    public Double omega9Fat;
    public Double oleicAcid;

    // Vitamins
    public Double vitaminA;
    public Double vitaminD;
    public Double vitaminE;
    public Double vitaminK;
    public Double vitaminC;
    public Double vitaminB1;
    public Double vitaminB2;
    public Double vitaminPP;
    public Double vitaminB6;
    public Double vitaminB9;
    public Double vitaminB12;
    public Double biotin;
    public Double pantothenicAcid;

    // Minerals & Trace Elements
    public Double silica;
    public Double bicarbonate;
    public Double potassium;
    public Double chloride;
    public Double calcium;
    public Double phosphorus;
    public Double iron;
    public Double magnesium;
    public Double zinc;
    public Double copper;
    public Double manganese;
    public Double fluoride;
    public Double selenium;
    public Double chromium;
    public Double molybdenum;
    public Double iodine;

    // Other
    public Double sodium;
    public Double alcohol;
    public Double caffeine;
    public Double taurine;
    public Double carbonFootprint;

    public Nutriments(@NonNull String barcode, Double energy, Double energyKj, Double proteins, Double carbohydrates, Double fat, Double fiber, Double sugars, Double addedSugars, Double sucrose, Double glucose, Double fructose, Double lactose, Double maltose, Double maltodextrins, Double starch, Double polyols, Double saturatedFat, Double monounsaturatedFat, Double polyunsaturatedFat, Double transFat, Double cholesterol, Double omega3Fat, Double alphaLinolenicAcid, Double eicosapentaenoicAcid, Double docosahexaenoicAcid, Double omega6Fat, Double linoleicAcid, Double arachidonicAcid, Double gammaLinolenicAcid, Double dihomoGammaLinolenicAcid, Double omega9Fat, Double oleicAcid, Double vitaminA, Double vitaminD, Double vitaminE, Double vitaminK, Double vitaminC, Double vitaminB1, Double vitaminB2, Double vitaminPP, Double vitaminB6, Double vitaminB9, Double vitaminB12, Double biotin, Double pantothenicAcid, Double silica, Double bicarbonate, Double potassium, Double chloride, Double calcium, Double phosphorus, Double iron, Double magnesium, Double zinc, Double copper, Double manganese, Double fluoride, Double selenium, Double chromium, Double molybdenum, Double iodine, Double sodium, Double alcohol, Double caffeine, Double taurine, Double carbonFootprint) {
        this.barcode = barcode;
        this.energy = energy;
        this.energyKj = energyKj;
        this.proteins = proteins;
        this.carbohydrates = carbohydrates;
        this.fat = fat;
        this.fiber = fiber;
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
        this.saturatedFat = saturatedFat;
        this.monounsaturatedFat = monounsaturatedFat;
        this.polyunsaturatedFat = polyunsaturatedFat;
        this.transFat = transFat;
        this.cholesterol = cholesterol;
        this.omega3Fat = omega3Fat;
        this.alphaLinolenicAcid = alphaLinolenicAcid;
        this.eicosapentaenoicAcid = eicosapentaenoicAcid;
        this.docosahexaenoicAcid = docosahexaenoicAcid;
        this.omega6Fat = omega6Fat;
        this.linoleicAcid = linoleicAcid;
        this.arachidonicAcid = arachidonicAcid;
        this.gammaLinolenicAcid = gammaLinolenicAcid;
        this.dihomoGammaLinolenicAcid = dihomoGammaLinolenicAcid;
        this.omega9Fat = omega9Fat;
        this.oleicAcid = oleicAcid;
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
        this.sodium = sodium;
        this.alcohol = alcohol;
        this.caffeine = caffeine;
        this.taurine = taurine;
        this.carbonFootprint = carbonFootprint;
    }
}
