package com.r19988088.tvlauncher.ui;

public final class GridMetrics {
    private final int horizontalPadding;
    private final int horizontalSpacing;
    private final int columnWidth;
    private final int cardWidth;
    private final int cardHeight;
    private final int topPadding;
    private final int verticalSpacing;

    private GridMetrics(
            int horizontalPadding,
            int horizontalSpacing,
            int columnWidth,
            int cardWidth,
            int cardHeight,
            int topPadding,
            int verticalSpacing) {
        this.horizontalPadding = horizontalPadding;
        this.horizontalSpacing = horizontalSpacing;
        this.columnWidth = columnWidth;
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.topPadding = topPadding;
        this.verticalSpacing = verticalSpacing;
    }

    public static GridMetrics calculate(
            int viewportWidth,
            int viewportHeight,
            int columns,
            int cardScalePercent,
            float density) {
        int safeColumns = Math.max(1, columns);
        int horizontalPadding = Math.round(viewportWidth * 0.04f);
        int horizontalSpacing = Math.round(viewportWidth * 0.025f);
        int available = viewportWidth
                - horizontalPadding * 2
                - horizontalSpacing * (safeColumns - 1);
        int columnWidth = Math.max(1, available / safeColumns);
        int cardWidth = Math.max(1, columnWidth * cardScalePercent / 100);
        return new GridMetrics(
                horizontalPadding,
                horizontalSpacing,
                columnWidth,
                cardWidth,
                cardWidth * 9 / 16,
                Math.round(viewportHeight / 15f),
                Math.round(viewportHeight / 54f));
    }

    public int horizontalPadding() { return horizontalPadding; }
    public int horizontalSpacing() { return horizontalSpacing; }
    public int columnWidth() { return columnWidth; }
    public int cardWidth() { return cardWidth; }
    public int cardHeight() { return cardHeight; }
    public int topPadding() { return topPadding; }
    public int verticalSpacing() { return verticalSpacing; }
}
