package com.r19988088.tvlauncher.wallpaper;

public final class WallpaperDecodeSize {
    private WallpaperDecodeSize() {}

    public static int sampleSize(
            int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int sample = 1;
        while (sourceWidth / (sample * 2) >= targetWidth
                && sourceHeight / (sample * 2) >= targetHeight) {
            sample *= 2;
        }
        return sample;
    }
}
