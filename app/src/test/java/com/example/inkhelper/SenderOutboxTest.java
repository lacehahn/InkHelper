package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SenderOutboxTest {
    @Test
    public void recordsObservedTransferAttemptAndFinalStatus() {
        // F-0006-S02, F-0006-S14
        InkRuntime runtime = new InkRuntime();
        runtime.selectRole(RuntimeRole.SENDER);

        TransferStatus status = runtime.handleObservedEvent(
                RuntimeRole.SENDER,
                true,
                new ObservedNotificationEvent("App", 1000L, "Title", "Text"));

        List<SenderOutboxItem> items = runtime.senderOutboxItems();
        assertEquals(TransferStatus.NOT_SENT, status);
        assertEquals(1, items.size());
        assertEquals("App", items.get(0).sourceApplication);
        assertEquals("Title", items.get(0).title);
        assertEquals("Text", items.get(0).text);
        assertEquals(TransferStatus.NOT_SENT, items.get(0).status);
        assertEquals("未发送", items.get(0).stage);
    }

    @Test
    public void recordsEligibilityRejectionInOutbox() {
        // F-0004-S05, F-0006-S14
        InkRuntime runtime = new InkRuntime();
        runtime.selectRole(RuntimeRole.SENDER);

        TransferStatus status = runtime.handleObservedEvent(
                RuntimeRole.SENDER,
                true,
                new ObservedNotificationEvent("App", 1000L, "", ""));

        List<SenderOutboxItem> items = runtime.senderOutboxItems();
        assertEquals(TransferStatus.NOT_SENT, status);
        assertEquals(1, items.size());
        assertEquals("App", items.get(0).sourceApplication);
        assertEquals(TransferStatus.NOT_SENT, items.get(0).status);
        assertEquals("资格检查拒绝", items.get(0).stage);
    }

    @Test
    public void senderOutboxDeletesOneItemAndCanBeCleared() {
        // F-0006-S15, F-0006-S16
        InkRuntime runtime = new InkRuntime();
        runtime.selectRole(RuntimeRole.SENDER);
        runtime.handleObservedEvent(RuntimeRole.SENDER, true,
                new ObservedNotificationEvent("App", 1000L, "First", "Text"));
        runtime.handleObservedEvent(RuntimeRole.SENDER, true,
                new ObservedNotificationEvent("App", 1001L, "Second", "Text"));

        long latestId = runtime.senderOutboxItems().get(0).id;
        assertTrue(runtime.removeSenderOutboxItem(latestId));

        assertEquals(1, runtime.senderOutboxItems().size());
        assertEquals("First", runtime.senderOutboxItems().get(0).title);
        assertEquals(SessionState.DISCONNECTED, runtime.senderSessionState());

        assertTrue(runtime.clearSenderOutbox());
        assertTrue(runtime.senderOutboxItems().isEmpty());
        assertEquals(SessionState.DISCONNECTED, runtime.senderSessionState());
    }
}
