package com.r19988088.tvlauncher.wallpaper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WallpaperDecodeSizeTest {
    @Test
    public void keepsNativeResolutionAndOnlySamplesOversizedSources() {
        assertEquals(1, WallpaperDecodeSize.sampleSize(1920, 1080, 1920, 1080));
        assertEquals(1, WallpaperDecodeSize.sampleSize(3840, 2160, 3840, 2160));
        assertEquals(2, WallpaperDecodeSize.sampleSize(3840, 2160, 1920, 1080));
        assertEquals(4, WallpaperDecodeSize.sampleSize(7680, 4320, 1920, 1080));
    }
}
