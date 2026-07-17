package com.r19988088.tvlauncher.ui;

public final class GridMetrics {
    private final int horizontalPadding;
    private final int horizontalSpacing;
    private final int columnWidth;
    private final int cardWidth;
    private final int cardHeight;
    private final int topPadding;
    private final int verticalSpacing;
    private final int displayColumns;

    private GridMetrics(
            int horizontalPadding,
            int horizontalSpacing,
            int columnWidth,
            int cardWidth,
            int cardHeight,
            int topPadding,
            int verticalSpacing,
            int displayColumns) {
        this.horizontalPadding = horizontalPadding;
        this.horizontalSpacing = horizontalSpacing;
        this.columnWidth = columnWidth;
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.topPadding = topPadding;
        this.verticalSpacing = verticalSpacing;
        this.displayColumns = displayColumns;
    }

    public static GridMetrics calculate(
            int viewportWidth,
            int viewportHeight,
            int configuredColumns,
            int itemCount,
            int cardScalePercent,
            float density) {
        int safeColumns = Math.max(1, configuredColumns);
        int displayColumns = Math.max(1, Math.min(safeColumns, itemCount));
        int targetPadding = Math.round(viewportWidth * 0.04f);
        int horizontalSpacing = Math.round(viewportWidth * 0.025f);
        int available = viewportWidth
                - targetPadding * 2
                - horizontalSpacing * (safeColumns - 1);
        int columnWidth = Math.max(1, available / safeColumns);
        int cardWidth = Math.max(1, columnWidth * cardScalePercent / 100);
        int groupWidth = columnWidth * displayColumns
                + horizontalSpacing * (displayColumns - 1);
        int horizontalPadding = Math.max(0, (viewportWidth - groupWidth) / 2);
        return new GridMetrics(
                horizontalPadding,
                horizontalSpacing,
                columnWidth,
                cardWidth,
                cardWidth * 9 / 16,
                Math.round(viewportHeight / 15f),
                Math.round(viewportHeight / 54f),
                displayColumns);
    }

    public int horizontalPadding() { return horizontalPadding; }
    public int horizontalSpacing() { return horizontalSpacing; }
    public int columnWidth() { return columnWidth; }
    public int cardWidth() { return cardWidth; }
    public int cardHeight() { return cardHeight; }
    public int topPadding() { return topPadding; }
    public int verticalSpacing() { return verticalSpacing; }
    public int displayColumns() { return displayColumns; }
}
