package com.r19988088.tvlauncher.display;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class DisplayModeSelectorTest {
    @Test
    public void choosesHighestRefreshAtCurrentResolutionUpTo120() {
        DisplayModeSelector.Mode current = new DisplayModeSelector.Mode(1, 3840, 2160, 60f);
        DisplayModeSelector.Mode chosen = DisplayModeSelector.choose(current, Arrays.asList(
                current,
                new DisplayModeSelector.Mode(2, 3840, 2160, 120f),
                new DisplayModeSelector.Mode(3, 3840, 2160, 144f),
                new DisplayModeSelector.Mode(4, 1920, 1080, 120f)));

        assertEquals(2, chosen.id());
    }

    @Test
    public void keepsCurrentModeWhenNoSameResolutionUpgradeExists() {
        DisplayModeSelector.Mode current = new DisplayModeSelector.Mode(1, 3840, 2160, 60f);
        DisplayModeSelector.Mode chosen = DisplayModeSelector.choose(current, Arrays.asList(
                current,
                new DisplayModeSelector.Mode(2, 1920, 1080, 120f)));

        assertEquals(1, chosen.id());
    }
}
