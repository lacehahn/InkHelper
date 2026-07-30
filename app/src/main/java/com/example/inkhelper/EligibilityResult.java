package com.example.inkhelper;

public final class EligibilityResult {
    public final boolean accepted;
    public final TransferNotification notification;

    private EligibilityResult(boolean accepted, TransferNotification notification) {
        this.accepted = accepted;
        this.notification = notification;
    }

    public static EligibilityResult accepted(TransferNotification notification) {
        return new EligibilityResult(true, notification);
    }

    public static EligibilityResult rejected() {
        return new EligibilityResult(false, null);
    }
}
