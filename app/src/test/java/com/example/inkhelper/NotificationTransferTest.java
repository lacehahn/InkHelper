package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NotificationTransferTest {
    @Test
    public void eligibleNotificationIsConfirmedOnlyAfterReceiverAcceptsIt() throws Exception {
        // F-0006-S01, F-0006-S03
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        ReceiverInbox inbox = new ReceiverInbox();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> inbox.accept(notification)));

            assertEquals(TransferStatus.CONFIRMED_RECEIVED,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));
            assertEquals(1, inbox.snapshot().size());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void acceptedInboundNotificationInvokesSystemNotificationPresenter() throws Exception {
        // F-0008-S02
        LocalSessionServer server = new LocalSessionServer();
        InkRuntime receiverRuntime = new InkRuntime();
        AtomicInteger notifications = new AtomicInteger();
        receiverRuntime.setReceiverNotificationPresenter(notification -> {
            notifications.incrementAndGet();
            return true;
        });
        try {
            receiverRuntime.selectRole(RuntimeRole.RECEIVER);
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, receiverRuntime.connectReceiver("127.0.0.1:" + details.port, details.code));

            assertEquals(TransferStatus.CONFIRMED_RECEIVED,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));

            assertEquals(1, receiverRuntime.inboxItems().size());
            assertEquals(1, notifications.get());
        } finally {
            receiverRuntime.disconnectActiveSession();
            server.stop();
        }
    }

    @Test
    public void unavailableSystemNotificationDoesNotRejectInboundNotification() throws Exception {
        // F-0008-S03
        LocalSessionServer server = new LocalSessionServer();
        InkRuntime receiverRuntime = new InkRuntime();
        receiverRuntime.setReceiverNotificationPresenter(notification -> false);
        try {
            receiverRuntime.selectRole(RuntimeRole.RECEIVER);
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, receiverRuntime.connectReceiver("127.0.0.1:" + details.port, details.code));

            assertEquals(TransferStatus.CONFIRMED_RECEIVED,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));

            assertEquals(1, receiverRuntime.inboxItems().size());
        } finally {
            receiverRuntime.disconnectActiveSession();
            server.stop();
        }
    }

    @Test
    public void transferIsNotSentWithoutConnectedSessionAndLaterTransferCanProceed() throws Exception {
        // F-0006-S02, F-0006-S06
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        ReceiverInbox inbox = new ReceiverInbox();
        try {
            assertEquals(TransferStatus.NOT_SENT,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));

            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> inbox.accept(notification)));

            assertEquals(TransferStatus.CONFIRMED_RECEIVED,
                    server.transfer(new TransferNotification("App", 1001L, "Later", "Text")));
            assertEquals(1, inbox.snapshot().size());
            assertEquals("Later", inbox.snapshot().get(0).title);
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void idleConnectedSessionCanReceiveLaterTransfer() throws Exception {
        // F-0005-S13, F-0006-S01
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        ReceiverInbox inbox = new ReceiverInbox();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> inbox.accept(notification)));

            TimeUnit.MILLISECONDS.sleep(10500L);

            assertEquals(SessionState.CONNECTED, client.state());
            assertEquals(TransferStatus.CONFIRMED_RECEIVED,
                    server.transfer(new TransferNotification("App", 1000L, "After idle", "Text")));
            assertEquals(1, inbox.snapshot().size());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void interruptedTransferIsUnconfirmedAndDoesNotAddPartialInboxItem() throws Exception {
        // F-0006-S05, F-0006-S10
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        ReceiverInbox inbox = new ReceiverInbox();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> inbox.accept(notification)));

            client.stop();

            assertEquals(TransferStatus.UNCONFIRMED,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));
            assertTrue(inbox.snapshot().isEmpty());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void receiverRejectionProducesRejectedTransferStatus() throws Exception {
        // F-0006-S04
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> false));

            assertEquals(TransferStatus.REJECTED,
                    server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void invalidTransferDoesNotDropConnectedSession() throws Exception {
        // F-0006-S02
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code,
                    (sessionId, notification) -> true));

            assertEquals(TransferStatus.NOT_SENT, server.transfer(null));

            assertEquals(SessionState.CONNECTED, server.state());
            assertEquals(SessionState.CONNECTED, client.state());
        } finally {
            client.stop();
            server.stop();
        }
    }
}
