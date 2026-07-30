package com.example.inkhelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class InboxItemTest {
    @Test public void acceptsTransferRepresentation() {
        assertTrue(new InboxItem("App", "Now", "Title", "").isValid());
    }
    @Test public void rejectsMalformedRepresentation() {
        assertFalse(new InboxItem("", "Now", "Title", "Text").isValid());
        assertFalse(new InboxItem("App", "", "Title", "Text").isValid());
        assertFalse(new InboxItem("App", "Now", "", "").isValid());
    }

    @Test
    public void receiverInboxPresentsAcceptedNotificationsAndEmptyState() {
        // F-0002-S03, F-0002-S04, F-0002-S06
        ReceiverInbox inbox = new ReceiverInbox();

        assertTrue(inbox.isEmpty());
        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "Title", "Text")));

        assertFalse(inbox.isEmpty());
        assertEquals(1, inbox.snapshot().size());
        InboxItem item = inbox.snapshot().get(0);
        assertEquals("App", item.sourceApplication);
        assertFalse(item.capturedTimestamp.isEmpty());
        assertEquals("Title", item.title);
        assertEquals("Text", item.text);
    }

    @Test
    public void malformedNotificationDoesNotChangeInbox() {
        // F-0002-S05, F-0006-S04
        ReceiverInbox inbox = new ReceiverInbox();
        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "Title", "")));

        assertFalse(inbox.accept(new TransferNotification("", 1000L, "Title", "Text")));
        assertFalse(inbox.accept(new TransferNotification("App", 0L, "Title", "Text")));
        assertFalse(inbox.accept(new TransferNotification("App", 1000L, "", "")));
        assertEquals(1, inbox.snapshot().size());
    }

    @Test
    public void repeatedAcceptedEventsRemainDistinct() {
        // F-0006-S11
        ReceiverInbox inbox = new ReceiverInbox();

        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "Title", "Text")));
        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "Title", "Text")));

        assertEquals(2, inbox.snapshot().size());
    }

    @Test
    public void representativeLocalNotificationDoesNotCreateConnectedSession() {
        // F-0002-S07
        ReceiverInbox inbox = new ReceiverInbox();
        LocalSessionClient receiverSession = new LocalSessionClient();

        assertTrue(inbox.accept(new TransferNotification("Representative App", 1000L, "Sample title", "Sample text")));

        assertEquals(1, inbox.snapshot().size());
        assertEquals(SessionState.DISCONNECTED, receiverSession.state());
    }

    @Test
    public void roleSwitchDoesNotDeleteExistingReceiverInboxItems() {
        // F-0002-S08
        RuntimeRoleState roles = new RuntimeRoleState();
        ReceiverInbox inbox = new ReceiverInbox();
        roles.select(RuntimeRole.RECEIVER);
        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "Title", "Text")));

        roles.select(RuntimeRole.SENDER);

        assertEquals(RuntimeRole.SENDER, roles.activeRole());
        assertEquals(1, inbox.snapshot().size());
    }

    @Test
    public void inboxChangeListenersRunAfterAcceptedNotification() {
        // F-0002-S10
        InkRuntime runtime = new InkRuntime();
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        runtime.addInboxChangedListener(listener);
        try {
            runtime.selectRole(RuntimeRole.RECEIVER);

            assertTrue(runtime.addRepresentativeNotification());

            assertEquals(1, changes.get());
            assertEquals(1, runtime.inboxItems().size());
        } finally {
            runtime.removeInboxChangedListener(listener);
        }
    }

    @Test
    public void receiverInboxDeletesOneItemAndCanBeCleared() {
        // F-0002-S13, F-0002-S14
        ReceiverInbox inbox = new ReceiverInbox();
        assertTrue(inbox.accept(new TransferNotification("App", 1000L, "First", "Text")));
        assertTrue(inbox.accept(new TransferNotification("App", 1001L, "Second", "Text")));

        long firstId = inbox.snapshot().get(0).id;
        assertTrue(inbox.remove(firstId));

        assertEquals(1, inbox.snapshot().size());
        assertEquals("Second", inbox.snapshot().get(0).title);

        assertTrue(inbox.clear());
        assertTrue(inbox.isEmpty());
    }

    @Test
    public void runtimeInboxDeletionDoesNotChangeReceiverSessionState() {
        // F-0002-S13, F-0002-S14
        InkRuntime runtime = new InkRuntime();
        runtime.selectRole(RuntimeRole.RECEIVER);
        assertTrue(runtime.addRepresentativeNotification());
        long itemId = runtime.inboxItems().get(0).id;

        assertTrue(runtime.removeInboxItem(itemId));

        assertEquals(SessionState.DISCONNECTED, runtime.receiverSessionState());
        assertTrue(runtime.inboxItems().isEmpty());

        assertTrue(runtime.addRepresentativeNotification());
        assertTrue(runtime.clearInbox());
        assertEquals(SessionState.DISCONNECTED, runtime.receiverSessionState());
        assertTrue(runtime.inboxItems().isEmpty());
    }

    @Test
    public void systemNotificationContentUsesTransferredMessageFields() {
        // F-0008-S02
        TransferNotification notification = new TransferNotification("Chat App", 1000L, "Alice", "Hello from Sender");

        assertEquals("Alice", ReceiverSystemNotifier.displayTitle(notification));
        assertEquals("Hello from Sender", ReceiverSystemNotifier.displayText(notification));
        assertTrue(ReceiverSystemNotifier.bigText(notification).contains("Chat App"));
        assertTrue(ReceiverSystemNotifier.bigText(notification).contains("Alice"));
        assertTrue(ReceiverSystemNotifier.bigText(notification).contains("Hello from Sender"));
    }
}
