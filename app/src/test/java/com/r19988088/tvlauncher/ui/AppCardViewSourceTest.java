package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public final class AppCardViewSourceTest {
    @Test
    public void focusAnimationsUseHardwareLayersToPreserveVisualQuality() throws Exception {
        String source = new String(
                Files.readAllBytes(new File("src/main/java/com/r19988088/tvlauncher/ui/AppCardView.java").toPath()),
                StandardCharsets.UTF_8);

        int layerCount = source.split("\\.withLayer\\(\\)", -1).length - 1;

        assertTrue("Image and label focus transitions must retain their hardware layers",
                layerCount >= 2);
    }
}
