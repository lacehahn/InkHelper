package com.example.inkhelper;

public interface ReceiverNotificationPresenter {
    boolean show(TransferNotification notification);

    static ReceiverNotificationPresenter noop() {
        return notification -> false;
    }
}
