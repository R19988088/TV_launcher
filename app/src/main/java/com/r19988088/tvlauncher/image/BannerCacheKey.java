package com.r19988088.tvlauncher.image;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class BannerCacheKey {
    private final String componentId;
    private final long lastUpdateTime;
    private final int width;
    private final int height;
    private final int iconScalePercent;

    public BannerCacheKey(
            String componentId,
            long lastUpdateTime,
            int width,
            int height,
            int iconScalePercent) {
        this.componentId = componentId;
        this.lastUpdateTime = lastUpdateTime;
        this.width = width;
        this.height = height;
        this.iconScalePercent = iconScalePercent;
    }

    public String fileName() {
        String value = componentId + ':' + lastUpdateTime + ':' + width + ':' + height + ':'
                + iconScalePercent;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BannerCacheKey)) {
            return false;
        }
        BannerCacheKey key = (BannerCacheKey) other;
        return lastUpdateTime == key.lastUpdateTime
                && width == key.width
                && height == key.height
                && iconScalePercent == key.iconScalePercent
                && componentId.equals(key.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentId, lastUpdateTime, width, height, iconScalePercent);
    }
}

