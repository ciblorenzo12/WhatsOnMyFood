package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ProductDao {

    @Transaction
    @Query("SELECT * FROM products WHERE barcode = :barcode")
    ProductWithDetails getProductWithDetails(String barcode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProduct(Product product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNutriments(Nutriments nutriments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllIngredients(List<Ingredient> ingredients);

    @Transaction
    public default void insertProductWithDetails(ProductWithDetails productWithDetails) {
        if (productWithDetails.product == null || !productWithDetails.product.isValid()) {
            return;
        }
        insertProduct(productWithDetails.product);
        if (productWithDetails.nutriments != null) {
            insertNutriments(productWithDetails.nutriments);
        }
        if (productWithDetails.ingredients != null && !productWithDetails.ingredients.isEmpty()) {
            insertAllIngredients(productWithDetails.ingredients);
        }
    }

    // Favorite method
    @Query("UPDATE products SET isFavorite = :isFavorite WHERE barcode = :barcode")
    void setFavorite(String barcode, boolean isFavorite);

    // Pantry methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPantry(Pantry pantry);

    @Query("SELECT * FROM pantry WHERE barcode = :barcode AND userId = :userId")
    Pantry findPantryItemByBarcode(String barcode, String userId);

    @Query("DELETE FROM pantry WHERE barcode = :barcode AND userId = :userId")
    void deletePantryProduct(String barcode, String userId);
    
    @Query("SELECT p.* FROM products p INNER JOIN pantry ON p.barcode = pantry.barcode WHERE pantry.userId = :userId")
    List<Product> getPantryProducts(String userId);

    @Query("UPDATE products SET aiInsight = :aiInsight WHERE barcode = :barcode")
    void updateAiInsight(String barcode, String aiInsight);

    @Query("UPDATE products SET healthScore = :healthScore WHERE barcode = :barcode")
    void updateHealthScore(String barcode, int healthScore);
    
    // Cache metadata methods
    @Query("SELECT * FROM cache_meta WHERE barcode = :barcode")
    CacheMeta getCacheMeta(String barcode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCacheMeta(CacheMeta cacheMeta);
}
