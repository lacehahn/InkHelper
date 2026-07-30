package com.example.inkhelper;

public final class LocalSessionDetails {
    public final String address;
    public final int port;
    public final String code;

    public LocalSessionDetails(String address, int port, String code) {
        this.address = address;
        this.port = port;
        this.code = code;
    }

    public String addressWithPort() {
        return address + ":" + port;
    }
}
