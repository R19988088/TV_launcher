package com.r19988088.tvlauncher.system;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SystemPackageControlTest {
    @Test
    public void buildsExplicitPerUserPackageCommands() {
        assertArrayEquals(
                new String[] {"pm", "disable-user", "--user", "0", "com.mitv.tvhome"},
                SystemPackageControl.commandFor("com.mitv.tvhome", true));
        assertArrayEquals(
                new String[] {"pm", "enable", "--user", "0", "com.mitv.tvhome"},
                SystemPackageControl.commandFor("com.mitv.tvhome", false));
    }

    @Test
    public void buildsLocalAdbShellCommands() {
        assertEquals(
                "pm disable-user --user 0 com.mitv.tvhome",
                SystemPackageControl.localAdbCommandFor("com.mitv.tvhome", true));
        assertEquals(
                "pm enable --user 0 com.mitv.tvhome",
                SystemPackageControl.localAdbCommandFor("com.mitv.tvhome", false));
    }

    @Test
    public void buildsSilentPerUserUninstallCommand() {
        assertEquals(
                "pm uninstall --user 0 com.example.tv",
                SystemPackageControl.localAdbUninstallCommandFor("com.example.tv"));
    }
}
