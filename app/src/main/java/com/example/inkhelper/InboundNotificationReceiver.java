package com.example.inkhelper;

public interface InboundNotificationReceiver {
    boolean receive(String sessionId, TransferNotification notification);
}
