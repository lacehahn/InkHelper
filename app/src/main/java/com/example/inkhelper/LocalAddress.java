package com.example.inkhelper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LocalAddress {
    private LocalAddress() {
    }

    public static String discover() {
        List<AddressCandidate> candidates = new ArrayList<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        candidates.add(new AddressCandidate(networkInterface.getName(), address.getHostAddress()));
                    }
                }
            }
        } catch (SocketException ignored) {
            return "127.0.0.1";
        }
        String selected = chooseAddress(candidates);
        if (!"127.0.0.1".equals(selected)) {
            InkLog.d("event=local_address_selected address=" + selected
                    + " candidates=" + candidates.size());
        }
        return selected;
    }

    static String chooseAddress(List<AddressCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "127.0.0.1";
        }
        AddressCandidate best = null;
        int bestScore = Integer.MAX_VALUE;
        for (AddressCandidate candidate : candidates) {
            int score = score(candidate, candidates.size() > 1);
            if (best == null || score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best == null ? "127.0.0.1" : best.address;
    }

    private static int score(AddressCandidate candidate, boolean hasAlternatives) {
        int score = 100;
        String name = candidate.interfaceName == null ? "" : candidate.interfaceName.toLowerCase(java.util.Locale.US);
        if (name.startsWith("wlan") || name.contains("wifi") || name.startsWith("ap") || name.startsWith("p2p")) {
            score -= 60;
        } else if (!name.startsWith("eth")) {
            score -= 30;
        }
        if (hasAlternatives && "10.0.2.15".equals(candidate.address)) {
            score += 80;
        }
        return score;
    }

    static final class AddressCandidate {
        final String interfaceName;
        final String address;

        AddressCandidate(String interfaceName, String address) {
            this.interfaceName = interfaceName;
            this.address = address;
        }
    }
}
