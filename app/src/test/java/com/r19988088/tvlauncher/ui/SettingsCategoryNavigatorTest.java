package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SettingsCategoryNavigatorTest {
    @Test
    public void backLeavesCategoryBeforeClosingSettings() {
        SettingsCategoryNavigator navigator = new SettingsCategoryNavigator();

        navigator.select(SettingsCategoryNavigator.DESKTOP);
        navigator.enter();

        assertTrue(navigator.isEntered());
        assertFalse(navigator.backShouldClose());
        assertFalse(navigator.isEntered());
        assertTrue(navigator.backShouldClose());
        assertEquals(SettingsCategoryNavigator.DESKTOP, navigator.selected());
    }
}
