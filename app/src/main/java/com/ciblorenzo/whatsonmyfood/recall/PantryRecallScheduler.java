package com.ciblorenzo.whatsonmyfood.recall;

import android.content.Context;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public final class PantryRecallScheduler {
    private PantryRecallScheduler() {}
    static String tag(String userId) { return "pantry-recalls-" + userId; }
    public static void start(Context context, String userId) {
        PeriodicWorkRequest daily = new PeriodicWorkRequest.Builder(PantryRecallWorker.class, 24, TimeUnit.HOURS)
                .setInputData(new Data.Builder().putString("user", userId).build())
                .addTag(tag(userId)).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(tag(userId) + "-daily",
                ExistingPeriodicWorkPolicy.KEEP, daily);
        sweep(context, userId);
    }
    public static void sweep(Context context, String userId) {
        // Append so a save during a running sweep cannot be lost. Sweeps perform no network IO.
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PantryRecallWorker.class)
                .setInputData(new Data.Builder().putString("user", userId).build())
                .addTag(tag(userId)).build();
        WorkManager.getInstance(context).enqueueUniqueWork(tag(userId) + "-sweep",
                ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
    public static void item(Context context, String userId, String barcode) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PantryRecallWorker.class)
                .setInputData(new Data.Builder().putString("user", userId).putString("barcode", barcode).build())
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .addTag(tag(userId)).build();
        WorkManager.getInstance(context).enqueueUniqueWork(tag(userId) + "-item-" + barcode,
                ExistingWorkPolicy.KEEP, request);
    }
    public static void cancel(Context context, String userId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(userId));
    }
}
