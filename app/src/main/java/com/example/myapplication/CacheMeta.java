package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cache_meta")
public class CacheMeta {

    @PrimaryKey
    @NonNull
    public String barcode;

    public long lastUpdated;

    public CacheMeta(@NonNull String barcode, long lastUpdated) {
        this.barcode = barcode;
        this.lastUpdated = lastUpdated;
    }
}
