package com.example.inkhelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public final class ReceiverBackgroundService extends Service {
    private static final String PREFERENCES = "runtime_role";
    private static final String ROLE_KEY = "selected_role";

    public static void start(Context context) {
        context.startService(new Intent(context, ReceiverBackgroundService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ReceiverBackgroundService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InkRuntime.get().initialize(getApplicationContext());
        InkLog.d("event=receiver_background_service_created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RuntimeRole role = readRole();
        InkLog.d("event=receiver_background_service_started role="
                + (role == null ? "null" : role.name()));
        if (role == RuntimeRole.RECEIVER) {
            InkRuntime.get().restoreRole(RuntimeRole.RECEIVER);
            return START_STICKY;
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        InkLog.d("event=receiver_background_service_destroyed");
        super.onDestroy();
    }

    private RuntimeRole readRole() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        return RuntimeRole.fromStoredValue(preferences.getString(ROLE_KEY, null));
    }
}
