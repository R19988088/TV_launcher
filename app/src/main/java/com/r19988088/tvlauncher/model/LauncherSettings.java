package com.r19988088.tvlauncher.model;

public final class LauncherSettings {
    public static final int DEFAULT_COLUMNS = 6;
    public static final int DEFAULT_CARD_SCALE_PERCENT = 100;
    public static final int DEFAULT_ICON_SCALE_PERCENT = 60;

    private final int columns;
    private final int cardScalePercent;
    private final int iconScalePercent;

    public LauncherSettings(int columns, int cardScalePercent, int iconScalePercent) {
        this.columns = clamp(columns, 4, 8);
        this.cardScalePercent = clamp(cardScalePercent, 80, 120);
        this.iconScalePercent = clamp(iconScalePercent, 40, 80);
    }

    public static LauncherSettings defaults() {
        return new LauncherSettings(
                DEFAULT_COLUMNS,
                DEFAULT_CARD_SCALE_PERCENT,
                DEFAULT_ICON_SCALE_PERCENT);
    }

    public int columns() {
        return columns;
    }

    public int cardScalePercent() {
        return cardScalePercent;
    }

    public int iconScalePercent() {
        return iconScalePercent;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
