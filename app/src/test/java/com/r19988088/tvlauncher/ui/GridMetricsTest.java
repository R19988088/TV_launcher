package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GridMetricsTest {
    @Test
    public void sixColumnsFillA1080pScreenWithoutHiddenRows() {
        GridMetrics metrics = GridMetrics.calculate(1920, 1080, 6, 6, 100, 0, 2f);

        assertEquals(78, metrics.horizontalPadding());
        assertEquals(48, metrics.horizontalSpacing());
        assertEquals(254, metrics.columnWidth());
        assertEquals(254, metrics.cardWidth());
        assertEquals(142, metrics.cardHeight());
        assertEquals(0, metrics.topPadding());
        assertEquals(20, metrics.verticalSpacing());
        assertEquals(6, metrics.displayColumns());
    }

    @Test
    public void cardScaleDoesNotMoveFocusTracks() {
        GridMetrics normal = GridMetrics.calculate(1920, 1080, 6, 6, 100, 0, 2f);
        GridMetrics compact = GridMetrics.calculate(1920, 1080, 6, 6, 80, 0, 2f);

        assertEquals(normal.columnWidth(), compact.columnWidth());
        assertEquals(203, compact.cardWidth());
    }

    @Test
    public void fiveColumnsKeepEqualScreenMargins() {
        GridMetrics metrics = GridMetrics.calculate(1920, 1080, 5, 5, 100, 0, 2f);

        int usedWidth = metrics.columnWidth() * 5 + metrics.horizontalSpacing() * 4;
        assertEquals((1920 - usedWidth) / 2, metrics.horizontalPadding());
        assertEquals(5, metrics.displayColumns());
    }

    @Test
    public void sparseRowsKeepConfiguredCardSizeAndCenterRealItems() {
        GridMetrics full = GridMetrics.calculate(1920, 1080, 6, 6, 100, 0, 2f);
        GridMetrics sparse = GridMetrics.calculate(1920, 1080, 6, 3, 100, 0, 2f);

        assertEquals(3, sparse.displayColumns());
        assertEquals(full.columnWidth(), sparse.columnWidth());
        assertEquals(531, sparse.horizontalPadding());
        assertEquals(
                sparse.horizontalPadding(),
                (1920 - sparse.columnWidth() * 3 - sparse.horizontalSpacing() * 2) / 2);
    }

    @Test
    public void topBlankRowsUseFullItemHeightAndStayOnScreen() {
        GridMetrics twoRows = GridMetrics.calculate(1920, 1080, 6, 6, 100, 2, 2f);
        GridMetrics fiveRows = GridMetrics.calculate(1920, 1080, 6, 6, 100, 5, 2f);

        assertEquals(492, twoRows.topPadding());
        assertEquals(834, fiveRows.topPadding());
    }

    @Test
    public void spacingScaleChangesBothAxesWithoutBreakingCentering() {
        GridMetrics metrics = GridMetrics.calculate(1920, 1080, 6, 3, 100, 0, 150, 2f);

        assertEquals(72, metrics.horizontalSpacing());
        assertEquals(30, metrics.verticalSpacing());
        assertEquals(
                (1920 - metrics.columnWidth() * 3 - metrics.horizontalSpacing() * 2) / 2,
                metrics.horizontalPadding());
    }
}
