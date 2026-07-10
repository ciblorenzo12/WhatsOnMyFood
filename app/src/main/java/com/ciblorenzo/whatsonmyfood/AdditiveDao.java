package com.ciblorenzo.whatsonmyfood;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AdditiveDao {
    @Query("SELECT * FROM additives")
    List<AdditiveEntry> getAllAdditives();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAdditive(AdditiveEntry entry);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<AdditiveEntry> entries);

    @Query("SELECT * FROM additives WHERE name = :name LIMIT 1")
    AdditiveEntry getAdditiveByName(String name);

    @Query("DELETE FROM additives WHERE name IS NULL OR category IS NULL OR `function` IS NULL " +
            "OR LOWER(TRIM(name)) IN ('null', 'none', 'unknown', 'n/a') " +
            "OR LOWER(TRIM(category)) IN ('null', 'none', 'unknown', 'n/a') " +
            "OR LOWER(TRIM(`function`)) IN ('null', 'none', 'unknown', 'n/a')")
    void deleteInvalidEntries();
}
