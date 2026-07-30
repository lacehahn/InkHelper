package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NotificationEligibilityTest {
    private final NotificationEligibility eligibility = new NotificationEligibility();

    @Test
    public void eligibleNotificationIsAcceptedAndPreparedForTransfer() {
        // F-0004-S01, F-0004-S04, F-0004-S07
        ObservedNotificationEvent event = new ObservedNotificationEvent("App", 1000L, "Title", "");

        EligibilityResult result = eligibility.evaluate(event, new ApplicationAllowlist());

        assertTrue(result.accepted);
        assertEquals("App", result.notification.sourceApplication);
        assertEquals(1000L, result.notification.capturedAtEpochMillis);
        assertEquals("Title", result.notification.title);
        assertEquals("", result.notification.text);
        assertNotSame(event, result.notification);
    }

    @Test
    public void ineligibleNotificationIsRejectedWithoutTransferRepresentation() {
        // F-0004-S02, F-0004-S05
        assertRejected(new ObservedNotificationEvent("", 1000L, "Title", "Text"));
        assertRejected(new ObservedNotificationEvent("App", null, "Title", "Text"));
        assertRejected(new ObservedNotificationEvent("App", 0L, "Title", "Text"));
        assertRejected(new ObservedNotificationEvent("App", 1000L, "", ""));
    }

    @Test
    public void rejectedNotificationPreservesPreviouslyAcceptedWork() {
        // F-0004-S03
        TransferNotification accepted = eligibility.evaluate(
                new ObservedNotificationEvent("App", 1000L, "Title", ""),
                new ApplicationAllowlist()).notification;

        EligibilityResult rejected = eligibility.evaluate(
                new ObservedNotificationEvent("App", 1001L, "", ""),
                new ApplicationAllowlist());

        assertFalse(rejected.accepted);
        assertNull(rejected.notification);
        assertEquals("Title", accepted.title);
    }

    @Test
    public void retainedSourceListDoesNotControlEligibility() {
        // F-0004-S06, F-0004-S08, F-0004-S09
        ApplicationAllowlist allowlist = new ApplicationAllowlist();
        TransferNotification acceptedBeforeAllowlist = eligibility.evaluate(
                new ObservedNotificationEvent("App", 1000L, "Title", ""),
                allowlist).notification;

        allowlist.add("Other");
        assertTrue(eligibility.evaluate(
                new ObservedNotificationEvent("App", 1001L, "Title", ""),
                allowlist).accepted);
        assertTrue(eligibility.evaluate(
                new ObservedNotificationEvent("Other", 1002L, "Title", ""),
                allowlist).accepted);

        allowlist.remove("Other");
        assertTrue(eligibility.evaluate(
                new ObservedNotificationEvent("Other", 1003L, "Title", ""),
                allowlist).accepted);
        assertEquals("App", acceptedBeforeAllowlist.sourceApplication);
    }

    private void assertRejected(ObservedNotificationEvent event) {
        assertRejected(event, new ApplicationAllowlist());
    }

    private void assertRejected(ObservedNotificationEvent event, ApplicationAllowlist allowlist) {
        EligibilityResult result = eligibility.evaluate(event, allowlist);
        assertFalse(result.accepted);
        assertNull(result.notification);
    }
}
