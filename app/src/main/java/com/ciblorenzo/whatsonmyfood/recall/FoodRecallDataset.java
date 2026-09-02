package com.ciblorenzo.whatsonmyfood.recall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One bounded response from the trusted recall source. */
public final class FoodRecallDataset {
    public final List<FoodRecallRecord> records;
    public final String sourceUpdatedAt;

    public FoodRecallDataset(List<FoodRecallRecord> records, String sourceUpdatedAt) {
        List<FoodRecallRecord> safeRecords = records == null
                ? Collections.emptyList()
                : new ArrayList<>(records);
        this.records = Collections.unmodifiableList(safeRecords);
        this.sourceUpdatedAt = sourceUpdatedAt == null ? "" : sourceUpdatedAt.trim();
    }
}
