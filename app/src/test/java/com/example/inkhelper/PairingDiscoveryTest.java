package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.List;

import org.junit.Test;

public class PairingDiscoveryTest {
    @Test
    public void discoveryMessageRoundTripsOnlyValidCandidates() {
        // F-0007-S01
        PairingCandidate candidate = new PairingCandidate("127.0.0.1", 12345, "654321");

        PairingCandidate decoded = PairingDiscoveryMessage.decodeCandidate(
                PairingDiscoveryMessage.encodeCandidate(candidate));

        assertEquals(candidate, decoded);
        assertNull(PairingDiscoveryMessage.decodeCandidate("not inkhelper"));
        assertEquals("", PairingDiscoveryMessage.encodeCandidate(new PairingCandidate("", 0, "")));
    }

    @Test
    public void activeSenderWithSessionDetailsIsDiscoverable() throws Exception {
        // F-0007-S01, F-0007-S04
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            LocalSessionDetails details = server.start();

            List<PairingCandidate> candidates = loopbackScanner().scan(500);

            assertFalse(candidates.isEmpty());
            PairingCandidate candidate = candidates.get(0);
            assertEquals(details.port, candidate.sessionPort);
            assertEquals(details.code, candidate.sessionCode);
            assertEquals("127.0.0.1", candidate.senderAddress);
            assertEquals(SessionState.DISCONNECTED, client.state());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void discoveredCandidateUsesReachableResponseAddress() throws Exception {
        // F-0007-S01, F-0007-S04
        PairingDiscoveryResponder responder = new PairingDiscoveryResponder();
        try {
            responder.start(new PairingCandidate("192.0.2.1", 12345, "654321"));

            List<PairingCandidate> candidates = loopbackScanner().scan(500);

            assertFalse(candidates.isEmpty());
            assertEquals("127.0.0.1", candidates.get(0).senderAddress);
            assertEquals(12345, candidates.get(0).sessionPort);
            assertEquals("654321", candidates.get(0).sessionCode);
        } finally {
            responder.stop();
        }
    }

    @Test
    public void senderWithoutCurrentSessionDetailsIsNotDiscoverable() throws Exception {
        // F-0007-S02
        assertTrue(loopbackScanner().scan(150).isEmpty());
    }

    @Test
    public void inactiveSenderIsNotDiscoverable() throws Exception {
        // F-0007-S03
        LocalSessionServer server = new LocalSessionServer();
        server.start();
        server.stop();

        assertTrue(loopbackScanner().scan(150).isEmpty());
    }

    @Test
    public void inactiveReceiverDoesNotScan() {
        // F-0007-S06
        PairingScanGate gate = new PairingScanGate();

        assertFalse(gate.canScan(RuntimeRole.SENDER));
        assertTrue(gate.canScan(RuntimeRole.RECEIVER));
    }

    @Test
    public void scanTargetCalculationIncludesDirectedBroadcastAddress() throws Exception {
        // F-0007-S10
        InetAddress address = InetAddress.getByName("192.168.12.34");

        InetAddress broadcast = PairingDiscoveryScanner.directedBroadcastAddress(address, (short) 24);

        assertNotNull(broadcast);
        assertEquals("192.168.12.255", broadcast.getHostAddress());
    }

    @Test
    public void subnetProbeCalculationUsesBoundedSameSubnetTargets() throws Exception {
        // F-0007-S10
        InetAddress address = InetAddress.getByName("192.168.12.34");

        List<InetAddress> probes = PairingDiscoveryScanner.subnetProbeAddresses(address, (short) 30);

        assertEquals(1, probes.size());
        assertEquals("192.168.12.33", probes.get(0).getHostAddress());
    }

    @Test
    public void subnetProbeCalculationDoesNotScanEntireLargeSubnet() throws Exception {
        // F-0007-S10
        InetAddress address = InetAddress.getByName("192.168.12.34");

        List<InetAddress> probes = PairingDiscoveryScanner.subnetProbeAddresses(address, (short) 24);

        assertTrue(probes.size() <= 34);
        assertTrue(containsHost(probes, "192.168.12.33"));
        assertTrue(containsHost(probes, "192.168.12.35"));
        assertTrue(containsHost(probes, "192.168.12.1"));
        assertTrue(containsHost(probes, "192.168.12.254"));
    }

    @Test
    public void receiverPairsWithDiscoveredSenderCandidate() throws Exception {
        // F-0007-S07
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            server.start();
            PairingCandidate candidate = firstCandidate();

            assertEquals(SessionState.CONNECTED, client.connect(
                    candidate.senderAddress,
                    candidate.sessionPort,
                    candidate.sessionCode,
                    (sessionId, notification) -> true));
            assertEquals(SessionState.CONNECTED, server.state());
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void expiredDiscoveredCandidateIsRejected() throws Exception {
        // F-0007-S08
        LocalSessionServer server = new LocalSessionServer();
        LocalSessionClient client = new LocalSessionClient();
        try {
            server.start();
            PairingCandidate candidate = firstCandidate();
            server.stop();

            assertEquals(SessionState.UNAVAILABLE, client.connect(
                    candidate.senderAddress,
                    candidate.sessionPort,
                    candidate.sessionCode,
                    (sessionId, notification) -> true));
        } finally {
            client.stop();
            server.stop();
        }
    }

    @Test
    public void scanWithoutCandidatesPreservesDisconnectedState() throws Exception {
        // F-0007-S05
        LocalSessionClient client = new LocalSessionClient();

        assertTrue(loopbackScanner().scan(150).isEmpty());
        assertEquals(SessionState.DISCONNECTED, client.state());
    }

    private PairingCandidate firstCandidate() throws Exception {
        List<PairingCandidate> candidates = loopbackScanner().scan(500);
        assertFalse(candidates.isEmpty());
        PairingCandidate candidate = candidates.get(0);
        assertNotNull(candidate);
        return candidate;
    }

    private PairingDiscoveryScanner loopbackScanner() throws Exception {
        return new PairingDiscoveryScanner(InetAddress.getByName("127.0.0.1"));
    }

    private boolean containsHost(List<InetAddress> addresses, String host) {
        for (InetAddress address : addresses) {
            if (host.equals(address.getHostAddress())) {
                return true;
            }
        }
        return false;
    }
}
