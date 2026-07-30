package com.example.inkhelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ApplicationDisplayNames {
    private static final Map<String, String> KNOWN_NAMES = knownNames();

    private ApplicationDisplayNames() {
    }

    public static String displayName(String sourceApplication) {
        String normalized = normalize(sourceApplication);
        if (normalized.isEmpty()) {
            return "";
        }
        String knownName = KNOWN_NAMES.get(normalized.toLowerCase(Locale.ROOT));
        return knownName == null ? normalized : knownName;
    }

    private static String normalize(String sourceApplication) {
        return sourceApplication == null ? "" : sourceApplication.trim();
    }

    private static Map<String, String> knownNames() {
        Map<String, String> names = new HashMap<>();
        names.put("com.tencent.mm", "微信");
        names.put("wechat", "微信");
        names.put("weixin", "微信");
        names.put("微信", "微信");
        names.put("com.tencent.mobileqq", "QQ");
        names.put("com.tencent.tim", "TIM");
        return Collections.unmodifiableMap(names);
    }
}
