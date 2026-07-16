package com.ciblorenzo.whatsonmyfood;

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

    @Query("DELETE FROM ingredients WHERE barcode = :barcode")
    void deleteIngredientsForBarcode(String barcode);

    @Query("DELETE FROM nutriments WHERE barcode = :barcode")
    void deleteNutrimentsForBarcode(String barcode);

    @Transaction
    public default void insertProductWithDetails(ProductWithDetails productWithDetails) {
        if (productWithDetails.product == null || !productWithDetails.product.isValid()) {
            return;
        }
        insertProduct(productWithDetails.product);
        deleteIngredientsForBarcode(productWithDetails.product.barcode);
        if (productWithDetails.nutriments != null) {
            insertNutriments(productWithDetails.nutriments);
        } else {
            deleteNutrimentsForBarcode(productWithDetails.product.barcode);
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

    @Transaction
    @Query("SELECT p.* FROM products p INNER JOIN pantry ON p.barcode = pantry.barcode WHERE pantry.userId = :userId")
    List<ProductWithDetails> getPantryProductsWithDetails(String userId);

    @Query("UPDATE products SET aiInsight = :aiInsight WHERE barcode = :barcode")
    void updateAiInsight(String barcode, String aiInsight);

    @Query("UPDATE products SET healthScore = :healthScore WHERE barcode = :barcode")
    void updateHealthScore(String barcode, int healthScore);

    @Query("UPDATE products SET userIngredientRiskScore = :score WHERE barcode = :barcode")
    void updateUserIngredientRiskScore(String barcode, int score);
    
    // Cache metadata methods
    @Query("SELECT * FROM cache_meta WHERE barcode = :barcode")
    CacheMeta getCacheMeta(String barcode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCacheMeta(CacheMeta cacheMeta);

    @Query("DELETE FROM cache_meta")
    void clearCacheMetadata();

    @Query("UPDATE products SET aiInsight = NULL")
    void clearCachedAiInsights();

    @Query("DELETE FROM ingredients WHERE barcode NOT IN (SELECT barcode FROM pantry) AND barcode NOT IN (SELECT barcode FROM products WHERE isFavorite = 1)")
    void deleteUnretainedCachedIngredients();

    @Query("DELETE FROM nutriments WHERE barcode NOT IN (SELECT barcode FROM pantry) AND barcode NOT IN (SELECT barcode FROM products WHERE isFavorite = 1)")
    void deleteUnretainedCachedNutriments();

    @Query("DELETE FROM products WHERE barcode NOT IN (SELECT barcode FROM pantry) AND isFavorite = 0")
    void deleteUnretainedCachedProducts();

    /** Clears downloaded product and AI cache while preserving pantry items and favorites. */
    @Transaction
    default void clearCachedProductData() {
        clearCachedAiInsights();
        clearCacheMetadata();
        deleteUnretainedCachedIngredients();
        deleteUnretainedCachedNutriments();
        deleteUnretainedCachedProducts();
    }
}
