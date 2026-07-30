package com.example.inkhelper;

import android.app.Notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SessionForegroundServiceTest {
    @Test
    public void foregroundServiceSupportsOnlySessionRoles() {
        // F-0009-S01, F-0009-S02, F-0009-S04, F-0009-S05
        assertTrue(SessionForegroundService.supportsRole(RuntimeRole.SENDER));
        assertTrue(SessionForegroundService.supportsRole(RuntimeRole.RECEIVER));
        assertFalse(SessionForegroundService.supportsRole(null));
    }

    @Test
    public void foregroundServiceCopyResourcesAreDefined() {
        // F-0009-S03, F-0009-S06
        assertNotEquals(0, R.string.foreground_service_channel_name);
        assertNotEquals(0, R.string.foreground_service_channel_description);
        assertNotEquals(0, R.string.foreground_service_title);
        assertNotEquals(0, R.string.foreground_service_sender_text);
        assertNotEquals(0, R.string.foreground_service_receiver_text);
    }

    @Test
    public void foregroundServiceNotificationRequestsNonClearableOngoingFlags() {
        // F-0009-S07
        int flags = SessionForegroundService.persistentNotificationFlags(0);

        assertTrue((flags & Notification.FLAG_ONGOING_EVENT) != 0);
        assertTrue((flags & Notification.FLAG_NO_CLEAR) != 0);
        assertTrue(SessionForegroundService.hasPersistentNotificationFlags(flags));
    }
}
