package com.ciblorenzo.whatsonmyfood.recall;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.ciblorenzo.whatsonmyfood.Product;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;

public final class RecallNotifications {
    private static final String CHANNEL = "pantry_recalls";
    private RecallNotifications() {}
    public static void createChannel(Context context) {
        context.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, context.getString(R.string.recall_notification_channel), NotificationManager.IMPORTANCE_DEFAULT));
    }
    public static void clear(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (android.service.notification.StatusBarNotification item : manager.getActiveNotifications()) {
            if (CHANNEL.equals(item.getNotification().getChannelId())) manager.cancel(item.getTag(), item.getId());
        }
    }
    public static boolean post(Context context, String userId, Product product, FoodRecallRecord recall) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return false;
        NotificationChannel channel = context.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);
        if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        ProductWithDetails details = new ProductWithDetails(); details.product = product;
        Intent intent = FoodRecallNavigation.createIntent(context, details, FoodRecallNavigation.EntryPoint.SAVED_PRODUCT);
        intent.putExtra("recall_owner", userId);
        String tag = userId + ":" + product.barcode + ":" + recall.recallNumber;
        intent.setData(android.net.Uri.parse("whatsonmyfood://recall/" + android.net.Uri.encode(tag)));
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.notify(tag, 0, new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_recall_notification)
                    .setContentTitle(context.getString(R.string.recall_notification_title))
                    .setContentText(context.getString(R.string.recall_notification_body, product.productName))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(
                            context.getString(R.string.recall_notification_body, product.productName)))
                    .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build());
            return true;
        } catch (SecurityException denied) { return false; }
    }
}
