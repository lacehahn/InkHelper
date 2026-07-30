package com.example.inkhelper;

public final class SenderObservationGate {
    public boolean canObserve(RuntimeRole activeRole, boolean notificationAccessAvailable) {
        return activeRole == RuntimeRole.SENDER && notificationAccessAvailable;
    }

    public ObservedNotificationEvent observe(
            RuntimeRole activeRole,
            boolean notificationAccessAvailable,
            ObservedNotificationEvent event) {
        return canObserve(activeRole, notificationAccessAvailable) ? event : null;
    }
}
