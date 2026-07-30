package com.example.inkhelper;

import java.util.Base64;

public final class PairingDiscoveryMessage {
    static final String DISCOVER = "INKHELPER_DISCOVER_V1";
    private static final String CANDIDATE_PREFIX = "INKHELPER_SENDER_V1 ";

    private PairingDiscoveryMessage() {
    }

    public static String encodeCandidate(PairingCandidate candidate) {
        if (candidate == null || !candidate.isValid()) {
            return "";
        }
        return CANDIDATE_PREFIX
                + encode(candidate.senderAddress)
                + " "
                + candidate.sessionPort
                + " "
                + encode(candidate.sessionCode);
    }

    public static PairingCandidate decodeCandidate(String message) {
        if (message == null || !message.startsWith(CANDIDATE_PREFIX)) {
            return null;
        }
        String[] parts = message.substring(CANDIDATE_PREFIX.length()).split(" ", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            PairingCandidate candidate = new PairingCandidate(
                    decode(parts[0]),
                    Integer.parseInt(parts[1]),
                    decode(parts[2]));
            return candidate.isValid() ? candidate : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
