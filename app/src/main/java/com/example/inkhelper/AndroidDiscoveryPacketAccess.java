package com.example.inkhelper;

import android.content.Context;
import android.net.wifi.WifiManager;

public final class AndroidDiscoveryPacketAccess {
    private static WifiManager.MulticastLock multicastLock;
    private static int acquireCount;

    private AndroidDiscoveryPacketAccess() {
    }

    public static synchronized void initialize(Context context) {
        if (context == null || multicastLock != null) {
            return;
        }
        Object service = context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!(service instanceof WifiManager)) {
            InkLog.d("event=discovery_packet_access_unavailable reason=no_wifi_manager");
            return;
        }
        multicastLock = ((WifiManager) service).createMulticastLock("InkHelperDiscovery");
        multicastLock.setReferenceCounted(false);
        InkLog.d("event=discovery_packet_access_ready");
    }

    public static synchronized void acquire(String owner) {
        if (multicastLock == null) {
            return;
        }
        acquireCount++;
        try {
            if (!multicastLock.isHeld()) {
                multicastLock.acquire();
            }
            InkLog.d("event=discovery_packet_access_acquired owner=" + safeOwner(owner)
                    + " count=" + acquireCount);
        } catch (RuntimeException exception) {
            acquireCount = Math.max(0, acquireCount - 1);
            InkLog.w("event=discovery_packet_access_acquire_failed owner=" + safeOwner(owner), exception);
        }
    }

    public static synchronized void release(String owner) {
        if (multicastLock == null || acquireCount <= 0) {
            return;
        }
        acquireCount--;
        try {
            if (acquireCount == 0 && multicastLock.isHeld()) {
                multicastLock.release();
            }
            InkLog.d("event=discovery_packet_access_released owner=" + safeOwner(owner)
                    + " count=" + acquireCount);
        } catch (RuntimeException exception) {
            InkLog.w("event=discovery_packet_access_release_failed owner=" + safeOwner(owner), exception);
        }
    }

    private static String safeOwner(String owner) {
        return owner == null ? "unknown" : owner.replace(' ', '_');
    }
}
