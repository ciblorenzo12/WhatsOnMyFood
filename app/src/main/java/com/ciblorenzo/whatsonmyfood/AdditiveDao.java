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
}
