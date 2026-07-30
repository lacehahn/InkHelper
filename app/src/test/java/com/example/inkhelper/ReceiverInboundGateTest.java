package com.example.inkhelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReceiverInboundGateTest {
    private final ReceiverInboundGate gate = new ReceiverInboundGate();
    private final TransferNotification notification = new TransferNotification("App", 1000L, "Title", "Text");

    @Test
    public void validInboundNotificationIsAcceptedOnlyForActiveConnectedRecognizedSession() {
        // F-0006-S03
        assertTrue(gate.accepts(RuntimeRole.RECEIVER, SessionState.CONNECTED, "session", "session", notification));
    }

    @Test
    public void inboundNotificationIsRejectedWhenReceiverIsInactiveDisconnectedWrongSessionOrMalformed() {
        // F-0006-S07, F-0006-S08, F-0006-S09, F-0006-S04
        assertFalse(gate.accepts(RuntimeRole.SENDER, SessionState.CONNECTED, "session", "session", notification));
        assertFalse(gate.accepts(RuntimeRole.RECEIVER, SessionState.DISCONNECTED, "session", "session", notification));
        assertFalse(gate.accepts(RuntimeRole.RECEIVER, SessionState.CONNECTED, "session", "other", notification));
        assertFalse(gate.accepts(RuntimeRole.RECEIVER, SessionState.CONNECTED, "session", "session",
                new TransferNotification("App", 1000L, "", "")));
    }
}
