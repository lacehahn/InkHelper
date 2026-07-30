package com.example.inkhelper;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class BackgroundGuidanceTest {
    @Test
    public void backgroundGuidanceStringsAreDefined() {
        // F-0008-S06
        assertNotEquals(0, R.string.background_settings_label);
        assertNotEquals(0, R.string.background_settings_summary);
        assertNotEquals(0, R.string.open_battery_optimization_settings);
        assertNotEquals(0, R.string.battery_optimization_restricted);
        assertNotEquals(0, R.string.battery_optimization_unrestricted);
        assertNotEquals(0, R.string.foreground_service_title);
    }
}
