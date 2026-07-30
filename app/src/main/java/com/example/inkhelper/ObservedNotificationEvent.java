package com.example.inkhelper;

public final class ObservedNotificationEvent {
    public final String sourceApplication;
    public final Long capturedAtEpochMillis;
    public final String title;
    public final String text;

    public ObservedNotificationEvent(String sourceApplication, Long capturedAtEpochMillis, String title, String text) {
        this.sourceApplication = sourceApplication == null ? "" : sourceApplication;
        this.capturedAtEpochMillis = capturedAtEpochMillis;
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
    }
}
