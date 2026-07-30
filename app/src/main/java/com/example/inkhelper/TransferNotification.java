package com.example.inkhelper;

import java.util.Objects;

public final class TransferNotification {
    public final String sourceApplication;
    public final long capturedAtEpochMillis;
    public final String title;
    public final String text;

    public TransferNotification(String sourceApplication, long capturedAtEpochMillis, String title, String text) {
        this.sourceApplication = normalize(sourceApplication);
        this.capturedAtEpochMillis = capturedAtEpochMillis;
        this.title = normalize(title);
        this.text = normalize(text);
    }

    public boolean isValid() {
        return !sourceApplication.trim().isEmpty()
                && capturedAtEpochMillis > 0
                && hasReadableContent();
    }

    public boolean hasReadableContent() {
        return !title.trim().isEmpty() || !text.trim().isEmpty();
    }

    public InboxItem toInboxItem(String capturedTimestamp) {
        return new InboxItem(sourceApplication, capturedTimestamp, title, text);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransferNotification)) {
            return false;
        }
        TransferNotification that = (TransferNotification) other;
        return capturedAtEpochMillis == that.capturedAtEpochMillis
                && sourceApplication.equals(that.sourceApplication)
                && title.equals(that.title)
                && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceApplication, capturedAtEpochMillis, title, text);
    }
}
