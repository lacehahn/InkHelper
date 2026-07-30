package com.example.inkhelper;

public final class SenderListenerSnapshot {
    public final boolean connected;
    public final String lastEvent;
    public final long lastEventAtEpochMillis;

    SenderListenerSnapshot(boolean connected, String lastEvent, long lastEventAtEpochMillis) {
        this.connected = connected;
        this.lastEvent = lastEvent == null || lastEvent.trim().isEmpty() ? "尚未收到监听器回调" : lastEvent.trim();
        this.lastEventAtEpochMillis = lastEventAtEpochMillis;
    }
}
