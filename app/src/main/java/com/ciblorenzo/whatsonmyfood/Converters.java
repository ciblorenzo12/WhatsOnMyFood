package com.ciblorenzo.whatsonmyfood;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static String fromHealthStatus(AdditiveEntry.HealthStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static AdditiveEntry.HealthStatus toHealthStatus(String status) {
        return status == null ? null : AdditiveEntry.HealthStatus.valueOf(status);
    }
}
