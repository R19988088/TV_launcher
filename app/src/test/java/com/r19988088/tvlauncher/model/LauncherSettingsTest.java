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
        LauncherSettings settings = new LauncherSettings(2, 240, 10, 9, 230);

        assertEquals(4, settings.columns());
        assertEquals(200, settings.cardScalePercent());
        assertEquals(40, settings.iconScalePercent());
        assertEquals(5, settings.topBlankRows());
        assertEquals(200, settings.spacingScalePercent());
    }

    @Test
    public void visualScalesCanReachHalfSize() {
        LauncherSettings settings = new LauncherSettings(6, 20, 60, 2, 40);

        assertEquals(50, settings.cardScalePercent());
        assertEquals(50, settings.spacingScalePercent());
    }
}
