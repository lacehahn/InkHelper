package com.example.inkhelper;

public final class RuntimeRoleState {
    private RuntimeRole activeRole;

    public RuntimeRole activeRole() {
        return activeRole;
    }

    public void restore(RuntimeRole role) {
        if (activeRole == null && role != null) {
            activeRole = role;
        }
    }

    public boolean select(RuntimeRole role) {
        if (role == null) {
            return false;
        }
        boolean changed = activeRole != role;
        activeRole = role;
        return changed;
    }
}
