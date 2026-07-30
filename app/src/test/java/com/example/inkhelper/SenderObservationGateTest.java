package com.example.inkhelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SenderObservationGateTest {
    private final SenderObservationGate gate = new SenderObservationGate();
    private final ObservedNotificationEvent event = new ObservedNotificationEvent("App", 1000L, "Title", "Text");

    @Test
    public void senderObservesOnlyWhenActiveAndAccessAvailable() {
        // F-0003-S01, F-0003-S04, F-0003-S07
        assertTrue(gate.canObserve(RuntimeRole.SENDER, true));
        assertSame(event, gate.observe(RuntimeRole.SENDER, true, event));
    }

    @Test
    public void senderDoesNotObserveWithoutAccessOrAfterRoleSwitch() {
        // F-0003-S02, F-0003-S03, F-0003-S05, F-0003-S06, F-0003-S08
        assertFalse(gate.canObserve(RuntimeRole.SENDER, false));
        assertNull(gate.observe(RuntimeRole.SENDER, false, event));
        assertFalse(gate.canObserve(RuntimeRole.RECEIVER, true));
        assertNull(gate.observe(RuntimeRole.RECEIVER, true, event));
    }

    @Test
    public void listenerDiagnosticsTrackConnectionAndRecentEvent() {
        // F-0003-S09
        InkRuntime runtime = new InkRuntime();

        SenderListenerSnapshot initial = runtime.senderListenerSnapshot();
        assertFalse(initial.connected);
        assertTrue(initial.lastEvent.length() > 0);

        runtime.recordSenderListenerEvent("监听服务已连接系统", true);
        SenderListenerSnapshot connected = runtime.senderListenerSnapshot();

        assertTrue(connected.connected);
        assertEquals("监听服务已连接系统", connected.lastEvent);
        assertTrue(connected.lastEventAtEpochMillis > 0L);
    }
}
