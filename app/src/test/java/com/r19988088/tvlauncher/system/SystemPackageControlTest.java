package com.r19988088.tvlauncher.system;

import static org.junit.Assert.assertArrayEquals;

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
}
