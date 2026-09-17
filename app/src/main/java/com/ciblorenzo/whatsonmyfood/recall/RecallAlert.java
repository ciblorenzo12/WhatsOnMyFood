package com.ciblorenzo.whatsonmyfood.recall;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "recall_alerts", primaryKeys = {"userId", "barcode", "recallId"})
public class RecallAlert {
    @NonNull public String userId = "";
    @NonNull public String barcode = "";
    @NonNull public String recallId = "";
    public long notifiedAt;
}
