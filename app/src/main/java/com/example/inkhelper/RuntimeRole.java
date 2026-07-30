package com.example.inkhelper;

public enum RuntimeRole {
    SENDER,
    RECEIVER;

    public static RuntimeRole fromStoredValue(String value) {
        for (RuntimeRole role : values()) {
            if (role.name().equals(value)) {
                return role;
            }
        }
        return null;
    }
}
