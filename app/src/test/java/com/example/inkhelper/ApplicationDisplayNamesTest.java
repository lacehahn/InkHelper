package com.example.inkhelper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApplicationDisplayNamesTest {
    @Test
    public void knownPackageNamesUseReadableDisplayNames() {
        // F-0002-S15, F-0006-S17
        assertEquals("微信", ApplicationDisplayNames.displayName("com.tencent.mm"));
        assertEquals("微信", ApplicationDisplayNames.displayName("微信"));
        assertEquals("QQ", ApplicationDisplayNames.displayName("com.tencent.mobileqq"));
    }

    @Test
    public void unknownPackageNamesPassThrough() {
        // F-0002-S15, F-0006-S17
        assertEquals("com.example.app", ApplicationDisplayNames.displayName(" com.example.app "));
        assertEquals("", ApplicationDisplayNames.displayName(null));
    }
}
