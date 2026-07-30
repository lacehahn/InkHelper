package com.example.inkhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public final class SessionForegroundService extends Service {
    static final String CHANNEL_ID = "local_session_foreground";
    static final int NOTIFICATION_ID = 9001;
    private static final String EXTRA_ROLE = "role";
    private static final String PREFERENCES = "runtime_role";
    private static final String ROLE_KEY = "selected_role";

    public static void start(Context context, RuntimeRole role) {
        if (context == null || !supportsRole(role)) {
            return;
        }
        Intent intent = new Intent(context, SessionForegroundService.class);
        intent.putExtra(EXTRA_ROLE, role.name());
        try {
            ContextCompat.startForegroundService(context, intent);
        } catch (RuntimeException exception) {
            InkLog.w("event=session_foreground_service_launch_failed role=" + role.name(), exception);
        }
    }

    public static void stop(Context context) {
        if (context != null) {
            context.stopService(new Intent(context, SessionForegroundService.class));
        }
    }

    static boolean supportsRole(RuntimeRole role) {
        return role == RuntimeRole.SENDER || role == RuntimeRole.RECEIVER;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InkRuntime.get().initialize(getApplicationContext());
        ensureChannel();
        InkLog.d("event=session_foreground_service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RuntimeRole role = roleFromIntent(intent);
        if (!supportsRole(role)) {
            role = readRole();
        }
        InkLog.d("event=session_foreground_service_started role="
                + (role == null ? "null" : role.name()));
        if (!supportsRole(role)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        InkRuntime.get().restoreRole(role);
        try {
            startForeground(NOTIFICATION_ID, notification(role), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            InkLog.d("event=session_foreground_service_foreground role=" + role.name());
            return START_STICKY;
        } catch (RuntimeException exception) {
            InkLog.w("event=session_foreground_service_start_failed role=" + role.name(), exception);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        InkLog.d("event=session_foreground_service_destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification notification(RuntimeRole role) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.foreground_service_title))
                .setContentText(foregroundText(role))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(foregroundText(role)))
                .setContentIntent(contentIntent())
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        notification.flags = persistentNotificationFlags(notification.flags);
        return notification;
    }

    static int persistentNotificationFlags(int flags) {
        return flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
    }

    static boolean hasPersistentNotificationFlags(int flags) {
        return (flags & Notification.FLAG_ONGOING_EVENT) != 0
                && (flags & Notification.FLAG_NO_CLEAR) != 0;
    }

    private String foregroundText(RuntimeRole role) {
        if (role == RuntimeRole.SENDER) {
            return getString(R.string.foreground_service_sender_text);
        }
        return getString(R.string.foreground_service_receiver_text);
    }

    private void ensureChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_service_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.foreground_service_channel_description));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent contentIntent() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private RuntimeRole roleFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        return RuntimeRole.fromStoredValue(intent.getStringExtra(EXTRA_ROLE));
    }

    private RuntimeRole readRole() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        return RuntimeRole.fromStoredValue(preferences.getString(ROLE_KEY, null));
    }
}
