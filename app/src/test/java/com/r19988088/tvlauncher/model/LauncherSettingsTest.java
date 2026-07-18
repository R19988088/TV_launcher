package com.r19988088.tvlauncher.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class LauncherSettingsTest {
    @Test
    public void defaultsMatchSixColumnLargeCardDesign() {
        LauncherSettings settings = LauncherSettings.defaults();

        assertEquals(6, settings.columns());
        assertEquals(100, settings.cardScalePercent());
        assertEquals(60, settings.iconScalePercent());
        assertEquals(2, settings.topBlankRows());
        assertEquals(100, settings.spacingScalePercent());
    }

    @Test
    public void valuesAreClampedToSupportedRanges() {
        LauncherSettings settings = new LauncherSettings(2, 140, 10, 9, 180);

        assertEquals(4, settings.columns());
        assertEquals(120, settings.cardScalePercent());
        assertEquals(40, settings.iconScalePercent());
        assertEquals(5, settings.topBlankRows());
        assertEquals(150, settings.spacingScalePercent());
    }
}
