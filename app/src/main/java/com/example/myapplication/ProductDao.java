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
        insertProduct(productWithDetails.product);
        if (productWithDetails.nutriments != null) {
            insertNutriments(productWithDetails.nutriments);
        }
        if (productWithDetails.ingredients != null && !productWithDetails.ingredients.isEmpty()) {
            insertAllIngredients(productWithDetails.ingredients);
        }
    }

    // Pantry methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPantry(Pantry pantry);

    @Query("SELECT * FROM pantry WHERE barcode = :barcode")
    Pantry findPantryItemByBarcode(String barcode);

    @Query("DELETE FROM pantry WHERE barcode = :barcode")
    void deletePantryProduct(String barcode);
    
    @Query("SELECT p.* FROM products p INNER JOIN pantry ON p.barcode = pantry.barcode")
    List<Product> getPantryProducts();
    
    // Cache metadata methods
    @Query("SELECT * FROM cache_meta WHERE barcode = :barcode")
    CacheMeta getCacheMeta(String barcode);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCacheMeta(CacheMeta cacheMeta);
}
