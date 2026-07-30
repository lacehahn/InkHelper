package com.example.inkhelper;

public final class PairingScanGate {
    public boolean canScan(RuntimeRole activeRole) {
        return activeRole == RuntimeRole.RECEIVER;
    }
}
