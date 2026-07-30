package com.example.inkhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuntimeRoleTest {
    @Test
    public void parsesOnlyKnownStoredRoles() {
        // F-0001-S01, F-0001-S04, F-0001-S05
        assertNull(RuntimeRole.fromStoredValue(null));
        assertEquals(RuntimeRole.SENDER, RuntimeRole.fromStoredValue("SENDER"));
        assertEquals(RuntimeRole.RECEIVER, RuntimeRole.fromStoredValue("RECEIVER"));
        assertNull(RuntimeRole.fromStoredValue("unknown"));
    }

    @Test
    public void selectsAndSwitchesOneActiveRuntimeRole() {
        // F-0001-S02, F-0001-S03, F-0001-S06, F-0001-S07
        RuntimeRoleState state = new RuntimeRoleState();

        assertTrue(state.select(RuntimeRole.SENDER));
        assertEquals(RuntimeRole.SENDER, state.activeRole());
        assertTrue(state.select(RuntimeRole.RECEIVER));
        assertEquals(RuntimeRole.RECEIVER, state.activeRole());
        assertTrue(state.select(RuntimeRole.SENDER));
        assertEquals(RuntimeRole.SENDER, state.activeRole());
    }

    @Test
    public void repeatedSelectionPreservesActiveRole() {
        // F-0001-S08, F-0001-S09
        RuntimeRoleState state = new RuntimeRoleState();

        assertTrue(state.select(RuntimeRole.SENDER));
        assertFalse(state.select(RuntimeRole.SENDER));
        assertEquals(RuntimeRole.SENDER, state.activeRole());
        assertTrue(state.select(RuntimeRole.RECEIVER));
        assertFalse(state.select(RuntimeRole.RECEIVER));
        assertEquals(RuntimeRole.RECEIVER, state.activeRole());
    }

    @Test
    public void restoresPreviouslySelectedRoleOnlyWhenNoRoleIsActive() {
        // F-0001-S04, F-0001-S05
        RuntimeRoleState state = new RuntimeRoleState();

        state.restore(RuntimeRole.RECEIVER);
        assertEquals(RuntimeRole.RECEIVER, state.activeRole());
        state.restore(RuntimeRole.SENDER);
        assertEquals(RuntimeRole.RECEIVER, state.activeRole());
    }
}
