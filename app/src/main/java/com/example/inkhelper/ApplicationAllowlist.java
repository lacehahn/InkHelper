package com.example.inkhelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ApplicationAllowlist {
    private final Set<String> sourceApplications = new LinkedHashSet<>();

    public void add(String sourceApplication) {
        String normalized = normalize(sourceApplication);
        if (!normalized.isEmpty()) {
            sourceApplications.add(normalized);
        }
    }

    public void remove(String sourceApplication) {
        sourceApplications.remove(normalize(sourceApplication));
    }

    public boolean allows(String sourceApplication) {
        String normalized = normalize(sourceApplication);
        return sourceApplications.isEmpty() || sourceApplications.contains(normalized);
    }

    public List<String> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(sourceApplications));
    }

    public boolean isEmpty() {
        return sourceApplications.isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
