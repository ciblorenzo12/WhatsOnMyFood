package com.ciblorenzo.whatsonmyfood;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Entity(tableName = "cache_meta")
public class CacheMeta {

    @PrimaryKey
    @NonNull
    public String barcode;

    public long lastUpdated;

    /** Primary record provider, not the source of every field merged into the product. */
    public String sourceName;

    /** Original USDA basis; null for older records or a different primary provider. */
    public String usdaNutrientBasis;

    public CacheMeta(@NonNull String barcode, long lastUpdated) {
        this.barcode = barcode;
        this.lastUpdated = lastUpdated;
    }

    public List<ProductRepository.SourceStatus> withPrimarySourceStatuses(
            List<ProductRepository.SourceStatus> baseStatuses,
            boolean hasNutritionValues
    ) {
        LinkedHashSet<ProductRepository.SourceStatus> statuses = new LinkedHashSet<>();
        if (baseStatuses != null) statuses.addAll(baseStatuses);
        if ("FoodDataCentralClient".equals(sourceName)) {
            statuses.add(ProductRepository.SourceStatus.USDA_FOOD_DATA_CENTRAL);
            if (!"per100g".equals(usdaNutrientBasis) || !hasNutritionValues) {
                statuses.add(ProductRepository.SourceStatus.USDA_PER_100G_NUTRITION_UNAVAILABLE);
            }
        }
        return new ArrayList<>(statuses);
    }
}
