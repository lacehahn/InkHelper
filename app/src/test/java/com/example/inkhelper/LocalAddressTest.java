package com.example.inkhelper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class LocalAddressTest {
    @Test
    public void wifiStyleAddressIsPreferredOverEmulatorNatAddress() {
        // F-0005-S15
        String selected = LocalAddress.chooseAddress(Arrays.asList(
                new LocalAddress.AddressCandidate("eth0", "10.0.2.15"),
                new LocalAddress.AddressCandidate("wlan0", "192.168.1.42")));

        assertEquals("192.168.1.42", selected);
    }

    @Test
    public void onlyAvailableIpv4AddressIsSelected() {
        // F-0005-S07
        String selected = LocalAddress.chooseAddress(Arrays.asList(
                new LocalAddress.AddressCandidate("eth0", "10.0.2.15")));

        assertEquals("10.0.2.15", selected);
    }
}
