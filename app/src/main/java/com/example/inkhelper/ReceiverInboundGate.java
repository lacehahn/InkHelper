package com.example.inkhelper;

public final class ReceiverInboundGate {
    public boolean accepts(
            RuntimeRole activeRole,
            SessionState sessionState,
            String recognizedSessionId,
            String inboundSessionId,
            TransferNotification notification) {
        return activeRole == RuntimeRole.RECEIVER
                && sessionState == SessionState.CONNECTED
                && recognizedSessionId != null
                && recognizedSessionId.equals(inboundSessionId)
                && notification != null
                && notification.isValid();
    }
}
