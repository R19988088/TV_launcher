package com.r19988088.tvlauncher.ui;

public final class CardVisualState {
    private final float scale;
    private final float translationYDp;
    private final float elevationDp;
    private final float labelAlpha;

    private CardVisualState(
            float scale,
            float translationYDp,
            float elevationDp,
            float labelAlpha) {
        this.scale = scale;
        this.translationYDp = translationYDp;
        this.elevationDp = elevationDp;
        this.labelAlpha = labelAlpha;
    }

    public static CardVisualState focused() {
        return new CardVisualState(1.12f, -6f, 24f, 1f);
    }

    public static CardVisualState unfocused() {
        return new CardVisualState(1f, 0f, 1f, 0f);
    }

    public float scale() {
        return scale;
    }

    public float translationYDp() {
        return translationYDp;
    }

    public float elevationDp() {
        return elevationDp;
    }

    public float labelAlpha() {
        return labelAlpha;
    }
}
