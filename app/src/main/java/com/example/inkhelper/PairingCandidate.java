package com.example.inkhelper;

import java.util.Objects;

public final class PairingCandidate {
    public final String senderAddress;
    public final int sessionPort;
    public final String sessionCode;

    public PairingCandidate(String senderAddress, int sessionPort, String sessionCode) {
        this.senderAddress = senderAddress == null ? "" : senderAddress.trim();
        this.sessionPort = sessionPort;
        this.sessionCode = sessionCode == null ? "" : sessionCode.trim();
    }

    public boolean isValid() {
        return !senderAddress.isEmpty() && sessionPort > 0 && !sessionCode.isEmpty();
    }

    public String addressWithPort() {
        return senderAddress + ":" + sessionPort;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PairingCandidate)) {
            return false;
        }
        PairingCandidate that = (PairingCandidate) other;
        return sessionPort == that.sessionPort
                && senderAddress.equals(that.senderAddress)
                && sessionCode.equals(that.sessionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderAddress, sessionPort, sessionCode);
    }
}
