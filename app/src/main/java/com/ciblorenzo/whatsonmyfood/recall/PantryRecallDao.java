package com.ciblorenzo.whatsonmyfood.recall;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface PantryRecallDao {
    @Query("SELECT * FROM pantry_recall_status WHERE userId = :userId AND barcode = :barcode")
    PantryRecallStatus get(String userId, String barcode);

    @Query("SELECT * FROM pantry_recall_status WHERE userId = :userId AND barcode = :barcode")
    LiveData<PantryRecallStatus> observe(String userId, String barcode);

    @Query("SELECT s.* FROM pantry_recall_status s INNER JOIN pantry p ON s.userId = p.userId AND s.barcode = p.barcode WHERE s.userId = :userId")
    LiveData<List<PantryRecallStatus>> observePantry(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(PantryRecallStatus status);

    @Query("SELECT COUNT(*) FROM recall_alerts WHERE userId = :userId AND barcode = :barcode AND recallId = :recallId")
    int alertCount(String userId, String barcode, String recallId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void saveAlert(RecallAlert alert);
}
