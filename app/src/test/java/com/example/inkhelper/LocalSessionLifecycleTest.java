package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalSessionLifecycleTest {
    @Test
    public void senderPresentsDetailsAndReceiverConnectsWithMatchingDetails() throws Exception {
        // F-0005-S07, F-0005-S08, F-0005-S01, F-0005-S04
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();

            assertNotNull(details.addressWithPort());
            assertNotNull(details.code);
            assertEquals(SessionState.CONNECTING, server.state());
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));
            assertEquals(SessionState.CONNECTED, server.state());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void missingOrIncorrectSessionDetailsDoNotConnect() throws Exception {
        // F-0005-S09, F-0005-S02
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();

            assertEquals(SessionState.UNAVAILABLE, client.connect("", details.port, details.code, (sessionId, notification) -> true));
            assertEquals(SessionState.UNAVAILABLE, client.connect("127.0.0.1", details.port, "wrong", (sessionId, notification) -> true));
            assertTrue(server.state() == SessionState.CONNECTING || server.state() == SessionState.UNAVAILABLE);
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void secondCounterpartDoesNotReplaceActiveSession() throws Exception {
        // F-0005-S03
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient firstClient = new LocalSessionClient();
        LocalSessionClient secondClient = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();

            assertEquals(SessionState.CONNECTED, firstClient.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));
            String firstSessionId = server.sessionId();
            assertEquals(SessionState.UNAVAILABLE, secondClient.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));

            assertEquals(SessionState.CONNECTED, server.state());
            assertEquals(firstSessionId, server.sessionId());
        } finally {
            secondClient.stop();
            firstClient.stop();
            server.stop();
        }
    }

    @Test
    public void repeatedReceiverPairingWhileConnectedKeepsExistingSession() throws Exception {
        // F-0005-S12
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();

            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));
            String firstSessionId = server.sessionId();

            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));

            assertEquals(SessionState.CONNECTED, client.state());
            assertEquals(SessionState.CONNECTED, server.state());
            assertEquals(firstSessionId, server.sessionId());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void userDisconnectKeepsRoleActiveAndStopsSession() {
        // F-0005-S14
        InkRuntime runtime = new InkRuntime();
        runtime.selectRole(RuntimeRole.SENDER);
        runtime.startSenderSession();

        runtime.disconnectActiveSession();

        assertEquals(RuntimeRole.SENDER, runtime.activeRole());
        assertEquals(SessionState.DISCONNECTED, runtime.senderSessionState());
    }

    @Test
    public void interruptionAndRetryAreObservable() throws Exception {
        // F-0005-S05, F-0005-S06, F-0005-S10
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));

            client.stop();
            assertEquals(TransferStatus.UNCONFIRMED, server.transfer(new TransferNotification("App", 1000L, "Title", "Text")));
            assertEquals(SessionState.UNAVAILABLE, server.state());

            details = server.start();
            assertEquals(SessionState.CONNECTED, client.connect("127.0.0.1", details.port, details.code, (sessionId, notification) -> true));
            server.stop();
            assertEquals(SessionState.DISCONNECTED, server.state());
        } finally {
            client.stop();
            server.stop();
        }
    }
}
