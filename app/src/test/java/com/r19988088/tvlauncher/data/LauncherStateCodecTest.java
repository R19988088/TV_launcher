package com.r19988088.tvlauncher.data;

import static org.junit.Assert.assertEquals;

import com.r19988088.tvlauncher.model.LauncherSettings;
import java.util.Arrays;
import org.junit.Test;

public final class LauncherStateCodecTest {
    @Test
    public void roundTripPreservesOrderAndVisualSettings() {
        LauncherState state = new LauncherState(
                Arrays.asList("one/.Main", "two/.Tv"),
                new LauncherSettings(7, 110, 70),
                "content://wallpaper/current");

        LauncherState decoded = LauncherStateCodec.decode(LauncherStateCodec.encode(state));

        assertEquals(Arrays.asList("one/.Main", "two/.Tv"), decoded.componentIds());
        assertEquals(7, decoded.settings().columns());
        assertEquals(110, decoded.settings().cardScalePercent());
        assertEquals(70, decoded.settings().iconScalePercent());
        assertEquals("content://wallpaper/current", decoded.wallpaperUri());
    }

    @Test
    public void malformedJsonReturnsEmptyDefaultState() {
        LauncherState decoded = LauncherStateCodec.decode("{broken");

        assertEquals(0, decoded.componentIds().size());
        assertEquals(6, decoded.settings().columns());
        assertEquals("", decoded.wallpaperUri());
    }
}
