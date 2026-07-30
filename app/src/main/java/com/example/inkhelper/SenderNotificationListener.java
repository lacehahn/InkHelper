package com.example.inkhelper;

import android.content.ComponentName;
import android.content.Context;
import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class SenderNotificationListener extends NotificationListenerService {
    static boolean canObserve(Context context) {
        if (context == null) {
            return false;
        }
        String enabled = android.provider.Settings.Secure.getString(
                context.getContentResolver(), "enabled_notification_listeners");
        if (enabled == null || enabled.trim().isEmpty()) {
            return false;
        }
        ComponentName expected = componentName(context);
        for (String item : enabled.split(":")) {
            ComponentName actual = ComponentName.unflattenFromString(item);
            if (expected.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    static String accessDiagnostic(Context context) {
        boolean enabled = canObserve(context);
        return "component=" + componentName(context).flattenToString() + " access=" + enabled;
    }

    static boolean requestListenerRebind(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            InkLog.d("event=sender_listener_rebind_skipped supported=false");
            return false;
        }
        try {
            requestRebind(componentName(context));
            InkRuntime.get().recordSenderListenerEvent("已请求系统重新连接监听服务", false);
            InkLog.d("event=sender_listener_rebind_requested " + accessDiagnostic(context));
            return true;
        } catch (RuntimeException exception) {
            InkLog.w("event=sender_listener_rebind_failed " + accessDiagnostic(context), exception);
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        InkRuntime.get().initialize(getApplicationContext());
        InkRuntime.get().restoreRole(readStoredRole());
        InkRuntime.get().recordSenderListenerEvent("监听服务已创建", false);
        InkLog.d("event=sender_listener_created " + accessDiagnostic(this));
    }

    @Override
    public void onDestroy() {
        InkRuntime.get().recordSenderListenerEvent("监听服务已销毁", false);
        InkLog.d("event=sender_listener_destroyed");
        super.onDestroy();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        InkRuntime.get().initialize(getApplicationContext());
        InkRuntime.get().restoreRole(readStoredRole());
        InkRuntime.get().recordSenderListenerEvent("监听服务已连接系统", true);
        InkLog.d("event=sender_listener_connected " + accessDiagnostic(this));
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        InkRuntime.get().recordSenderListenerEvent("监听服务已断开系统连接", false);
        InkLog.d("event=sender_listener_disconnected " + accessDiagnostic(this));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        RuntimeRole role = readStoredRole();
        boolean settingsAccess = canObserve(this);
        String source = notification == null ? "null" : notification.getPackageName();
        InkRuntime.get().recordSenderListenerEvent("收到通知回调：" + source, true);
        InkLog.d("event=sender_listener_notification_posted source=" + source
                + " role=" + (role == null ? "null" : role.name())
                + " callbackAccess=true"
                + " settingsAccess=" + settingsAccess
                + " postTime=" + (notification == null ? 0L : notification.getPostTime())
                + " hasTitle=" + hasExtra(notification, Notification.EXTRA_TITLE)
                + " hasText=" + hasExtra(notification, Notification.EXTRA_TEXT)
                + " hasBigText=" + hasExtra(notification, Notification.EXTRA_BIG_TEXT)
                + " hasTextLines=" + hasTextLines(notification));
        if (notification == null) {
            InkRuntime.get().recordSenderListenerEvent("通知回调为空，已忽略", true);
            return;
        }
        if (role != RuntimeRole.SENDER) {
            InkRuntime.get().recordSenderListenerEvent("通知已忽略：当前不是发送端", true);
            InkLog.d("event=sender_listener_notification_ignored reason=role role="
                    + (role == null ? "null" : role.name()));
            return;
        }
        ObservedNotificationEvent observedEvent = toObservedEvent(notification);
        new Thread(() -> {
            InkLog.d("event=sender_listener_notification_transfer_thread_start source="
                    + observedEvent.sourceApplication);
            InkRuntime.get().restoreRole(RuntimeRole.SENDER);
            InkRuntime.get().handleObservedEvent(role, true, observedEvent);
        }, "InkHelperSenderNotificationTransfer").start();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        InkLog.d("event=sender_listener_notification_removed source="
                + (notification == null ? "null" : notification.getPackageName()));
    }

    private ObservedNotificationEvent toObservedEvent(StatusBarNotification notification) {
        CharSequence title = firstText(
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE),
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        CharSequence text = firstText(
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT),
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                notification.getNotification().extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                joinTextLines(notification),
                notification.getNotification().tickerText);
        long postTime = notification.getPostTime() > 0 ? notification.getPostTime() : System.currentTimeMillis();
        return new ObservedNotificationEvent(
                notification.getPackageName(),
                postTime,
                title == null ? "" : title.toString(),
                text == null ? "" : text.toString());
    }

    private RuntimeRole readStoredRole() {
        return RuntimeRole.fromStoredValue(getSharedPreferences("runtime_role", MODE_PRIVATE)
                .getString("selected_role", null));
    }

    private static ComponentName componentName(Context context) {
        return new ComponentName(context, SenderNotificationListener.class);
    }

    private static boolean hasExtra(StatusBarNotification notification, String key) {
        if (notification == null || notification.getNotification() == null
                || notification.getNotification().extras == null) {
            return false;
        }
        CharSequence value = notification.getNotification().extras.getCharSequence(key);
        return value != null && value.toString().trim().length() > 0;
    }

    private static boolean hasTextLines(StatusBarNotification notification) {
        return joinTextLines(notification).trim().length() > 0;
    }

    private static String joinTextLines(StatusBarNotification notification) {
        if (notification == null || notification.getNotification() == null
                || notification.getNotification().extras == null) {
            return "";
        }
        CharSequence[] lines = notification.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines == null || lines.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (CharSequence line : lines) {
            if (line == null || line.toString().trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.toString().trim());
        }
        return builder.toString();
    }

    private static CharSequence firstText(CharSequence... values) {
        if (values == null) {
            return "";
        }
        for (CharSequence value : values) {
            if (value != null && value.toString().trim().length() > 0) {
                return value;
            }
        }
        return "";
    }
}
