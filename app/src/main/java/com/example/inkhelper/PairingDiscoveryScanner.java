package com.example.inkhelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PairingDiscoveryScanner {
    private static final int BUFFER_SIZE = 512;
    private static final int DEFAULT_TIMEOUT_MILLIS = 1200;
    private static final int MAX_SUBNET_PROBES = 34;
    private final InetAddress targetAddress;

    public PairingDiscoveryScanner() {
        this(null);
    }

    PairingDiscoveryScanner(InetAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    public List<PairingCandidate> scan() {
        return scan(DEFAULT_TIMEOUT_MILLIS);
    }

    public List<PairingCandidate> scan(int timeoutMillis) {
        Set<PairingCandidate> candidates = new LinkedHashSet<>();
        int safeTimeoutMillis = Math.max(1, timeoutMillis);
        InkLog.d("event=pairing_scan_start target=" + targetLabel() + " timeoutMillis=" + safeTimeoutMillis);
        AndroidDiscoveryPacketAccess.acquire("receiver_scan");
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] request = PairingDiscoveryMessage.DISCOVER.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            List<InetAddress> targets = scanAddresses();
            InkLog.d("event=pairing_scan_targets count=" + targets.size()
                    + " targets=" + targetSummary(targets));
            for (InetAddress address : targets) {
                InkLog.d("event=pairing_scan_request_sent target=" + address.getHostAddress()
                        + " port=" + PairingDiscoveryResponder.DISCOVERY_PORT);
                socket.send(new DatagramPacket(request, request.length, address, PairingDiscoveryResponder.DISCOVERY_PORT));
            }
            long deadline = System.currentTimeMillis() + safeTimeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                try {
                    int remainingMillis = (int) Math.max(1, deadline - System.currentTimeMillis());
                    socket.setSoTimeout(remainingMillis);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    socket.receive(response);
                    String message = new String(response.getData(), response.getOffset(), response.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                    PairingCandidate candidate = PairingDiscoveryMessage.decodeCandidate(message);
                    if (candidate != null) {
                        PairingCandidate reachableCandidate = new PairingCandidate(
                                response.getAddress().getHostAddress(),
                                candidate.sessionPort,
                                candidate.sessionCode);
                        InkLog.d("event=pairing_scan_candidate sourceAddress=" + response.getAddress().getHostAddress()
                                + " advertisedAddress=" + candidate.senderAddress
                                + " selectedAddress=" + reachableCandidate.senderAddress
                                + " sessionPort=" + reachableCandidate.sessionPort
                                + " codeLength=" + reachableCandidate.sessionCode.length());
                        candidates.add(reachableCandidate);
                    } else {
                        InkLog.d("event=pairing_scan_ignored_response sourceAddress="
                                + response.getAddress().getHostAddress()
                                + " bytes=" + response.getLength());
                    }
                } catch (SocketTimeoutException exception) {
                    InkLog.d("event=pairing_scan_timeout candidates=" + candidates.size());
                    break;
                }
            }
        } catch (IOException exception) {
            InkLog.w("event=pairing_scan_failed candidates=0", exception);
            return new ArrayList<>();
        } finally {
            AndroidDiscoveryPacketAccess.release("receiver_scan");
        }
        InkLog.d("event=pairing_scan_complete candidates=" + candidates.size());
        return new ArrayList<>(candidates);
    }

    private List<InetAddress> scanAddresses() throws IOException {
        if (targetAddress != null) {
            return Collections.singletonList(targetAddress);
        }
        LinkedHashSet<InetAddress> targets = new LinkedHashSet<>();
        targets.add(InetAddress.getByName("255.255.255.255"));
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (!(address instanceof Inet4Address)
                            || address.isLoopbackAddress()
                            || (!address.isSiteLocalAddress() && !address.isLinkLocalAddress())) {
                        continue;
                    }
                    InkLog.d("event=pairing_scan_interface_address address=" + address.getHostAddress()
                            + " prefixLength=" + interfaceAddress.getNetworkPrefixLength());
                    InetAddress advertisedBroadcast = interfaceAddress.getBroadcast();
                    if (advertisedBroadcast instanceof Inet4Address) {
                        targets.add(advertisedBroadcast);
                    }
                    InetAddress calculatedBroadcast = directedBroadcastAddress(
                            address,
                            interfaceAddress.getNetworkPrefixLength());
                    if (calculatedBroadcast != null) {
                        targets.add(calculatedBroadcast);
                    }
                    targets.addAll(subnetProbeAddresses(address, interfaceAddress.getNetworkPrefixLength()));
                }
            }
        } catch (IOException exception) {
            InkLog.w("event=pairing_scan_target_build_failed", exception);
        }
        return new ArrayList<>(targets);
    }

    private String targetLabel() {
        return targetAddress == null ? "local-network" : targetAddress.getHostAddress();
    }

    private static String targetSummary(List<InetAddress> targets) {
        int limit = Math.min(targets.size(), 24);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(targets.get(index).getHostAddress());
        }
        if (targets.size() > limit) {
            builder.append(",...");
        }
        return builder.toString();
    }

    static InetAddress directedBroadcastAddress(InetAddress address, short prefixLength) throws IOException {
        if (!(address instanceof Inet4Address) || prefixLength < 0 || prefixLength > 30) {
            return null;
        }
        int value = ipv4ToInt(address);
        int mask = prefixLength == 0 ? 0 : -1 << (32 - prefixLength);
        int broadcast = (value & mask) | ~mask;
        if (broadcast == value) {
            return null;
        }
        return intToIpv4(broadcast);
    }

    static List<InetAddress> subnetProbeAddresses(InetAddress address, short prefixLength) throws IOException {
        if (!(address instanceof Inet4Address)) {
            return Collections.emptyList();
        }
        int effectivePrefixLength = prefixLength >= 24 && prefixLength <= 30 ? prefixLength : 24;
        int value = ipv4ToInt(address);
        int mask = -1 << (32 - effectivePrefixLength);
        int network = value & mask;
        int broadcast = network | ~mask;
        List<InetAddress> probes = new ArrayList<>();
        int firstHost = network + 1;
        int lastHost = broadcast - 1;
        int hostCount = lastHost - firstHost + 1;
        if (hostCount <= MAX_SUBNET_PROBES) {
            addProbeRange(probes, firstHost, lastHost, value);
            return probes;
        }
        addProbeRange(probes, Math.max(firstHost, value - 16), Math.min(lastHost, value + 16), value);
        addProbe(probes, firstHost, value);
        addProbe(probes, lastHost, value);
        return probes;
    }

    private static void addProbeRange(List<InetAddress> probes, int first, int last, int self) throws IOException {
        for (int candidate = first; candidate <= last; candidate++) {
            addProbe(probes, candidate, self);
        }
    }

    private static void addProbe(List<InetAddress> probes, int candidate, int self) throws IOException {
        if (candidate != self && !containsAddress(probes, candidate)) {
            probes.add(intToIpv4(candidate));
        }
    }

    private static boolean containsAddress(List<InetAddress> probes, int candidate) {
        byte[] candidateBytes = intToBytes(candidate);
        for (InetAddress probe : probes) {
            if (java.util.Arrays.equals(probe.getAddress(), candidateBytes)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private static InetAddress intToIpv4(int value) throws IOException {
        return InetAddress.getByAddress(intToBytes(value));
    }

    private static int ipv4ToInt(InetAddress address) {
        byte[] bytes = address.getAddress();
        return ((bytes[0] & 0xff) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8)
                | (bytes[3] & 0xff);
    }

}
