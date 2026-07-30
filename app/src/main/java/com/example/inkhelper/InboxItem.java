package com.example.inkhelper;

public final class InboxItem {
    public final long id;
    public final String sourceApplication;
    public final String capturedTimestamp;
    public final String title;
    public final String text;

    public InboxItem(String sourceApplication, String capturedTimestamp, String title, String text) {
        this(0L, sourceApplication, capturedTimestamp, title, text);
    }

    public InboxItem(long id, String sourceApplication, String capturedTimestamp, String title, String text) {
        this.id = id;
        this.sourceApplication = sourceApplication;
        this.capturedTimestamp = capturedTimestamp;
        this.title = title;
        this.text = text;
    }

    public boolean isValid() {
        return !sourceApplication.isEmpty() && !capturedTimestamp.isEmpty()
                && (!title.isEmpty() || !text.isEmpty());
    }
}
