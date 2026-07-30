package com.example.inkhelper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class ReceiverSystemNotifier implements ReceiverNotificationPresenter {
    static final String CHANNEL_ID = "receiver_messages";
    private static final int FIRST_NOTIFICATION_ID = 8000;
    private final Context context;
    private int nextNotificationId = FIRST_NOTIFICATION_ID;

    public ReceiverSystemNotifier(Context context) {
        this.context = context.getApplicationContext();
        ensureChannel();
    }

    @Override
    public synchronized boolean show(TransferNotification notification) {
        if (notification == null || !notification.isValid()) {
            InkLog.d("event=receiver_system_notification_skipped invalid=true");
            return false;
        }
        if (!hasNotificationPermission(context)) {
            InkLog.d("event=receiver_system_notification_skipped permission=false");
            return false;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(displayTitle(notification))
                .setContentText(displayText(notification))
                .setSubText(notification.sourceApplication)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText(notification)))
                .setContentIntent(contentIntent())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        try {
            NotificationManagerCompat.from(context).notify(nextNotificationId++, builder.build());
            InkLog.d("event=receiver_system_notification_posted source=" + notification.sourceApplication);
            return true;
        } catch (SecurityException exception) {
            InkLog.w("event=receiver_system_notification_failed", exception);
            return false;
        }
    }

    static boolean hasNotificationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static String displayTitle(TransferNotification notification) {
        String title = clean(notification.title);
        return title.isEmpty() ? notification.sourceApplication : title;
    }

    static String displayText(TransferNotification notification) {
        String text = clean(notification.text);
        if (!text.isEmpty()) {
            return text;
        }
        String title = clean(notification.title);
        return title.isEmpty() ? notification.sourceApplication : title;
    }

    static String bigText(TransferNotification notification) {
        String title = clean(notification.title);
        String text = clean(notification.text);
        if (!title.isEmpty() && !text.isEmpty()) {
            return notification.sourceApplication + "\n" + title + "\n" + text;
        }
        if (!title.isEmpty()) {
            return notification.sourceApplication + "\n" + title;
        }
        return notification.sourceApplication + "\n" + text;
    }

    private void ensureChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.receiver_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.receiver_notification_channel_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent contentIntent() {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
