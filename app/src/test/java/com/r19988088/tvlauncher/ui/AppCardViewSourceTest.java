package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public final class AppCardViewSourceTest {
    @Test
    public void focusAnimationsDoNotForceHardwareLayers() throws Exception {
        String source = new String(
                Files.readAllBytes(new File("src/main/java/com/r19988088/tvlauncher/ui/AppCardView.java").toPath()),
                StandardCharsets.UTF_8);

        assertFalse("Focus transitions must not allocate offscreen hardware layers",
                source.contains(".withLayer()"));
    }
}
