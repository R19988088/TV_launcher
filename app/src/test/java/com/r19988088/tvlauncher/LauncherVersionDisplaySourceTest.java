package com.r19988088.tvlauncher;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public final class LauncherVersionDisplaySourceTest {
    @Test
    public void settingsKeepLocalVersionSeparateFromUpdateStatus() throws Exception {
        String layout = new String(Files.readAllBytes(
                new File("src/main/res/layout/activity_launcher.xml").toPath()),
                StandardCharsets.UTF_8);
        String activity = new String(Files.readAllBytes(
                new File("src/main/java/com/r19988088/tvlauncher/LauncherActivity.java").toPath()),
                StandardCharsets.UTF_8);

        assertTrue(layout.contains("@+id/local_version"));
        assertTrue(activity.contains("localVersion = findViewById(R.id.local_version)"));
        assertTrue(activity.contains("localVersion.setText(getString(R.string.current_version, currentVersion()))"));
    }
}
