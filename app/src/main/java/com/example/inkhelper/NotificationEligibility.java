package com.example.inkhelper;

public final class NotificationEligibility {
    public EligibilityResult evaluate(ObservedNotificationEvent event, ApplicationAllowlist allowlist) {
        if (event == null || event.capturedAtEpochMillis == null) {
            return EligibilityResult.rejected();
        }
        TransferNotification notification = new TransferNotification(
                event.sourceApplication,
                event.capturedAtEpochMillis,
                event.title,
                event.text);
        if (!notification.isValid()) {
            return EligibilityResult.rejected();
        }
        return EligibilityResult.accepted(notification);
    }
}
