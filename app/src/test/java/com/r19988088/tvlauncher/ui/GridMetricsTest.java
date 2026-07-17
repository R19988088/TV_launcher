package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GridMetricsTest {
    @Test
    public void sixColumnsFillA1080pScreenWithoutHiddenRows() {
        GridMetrics metrics = GridMetrics.calculate(1920, 1080, 6, 100, 2f);

        assertEquals(77, metrics.horizontalPadding());
        assertEquals(48, metrics.horizontalSpacing());
        assertEquals(254, metrics.columnWidth());
        assertEquals(254, metrics.cardWidth());
        assertEquals(142, metrics.cardHeight());
        assertEquals(72, metrics.topPadding());
        assertEquals(20, metrics.verticalSpacing());
    }

    @Test
    public void cardScaleDoesNotMoveFocusTracks() {
        GridMetrics normal = GridMetrics.calculate(1920, 1080, 6, 100, 2f);
        GridMetrics compact = GridMetrics.calculate(1920, 1080, 6, 80, 2f);

        assertEquals(normal.columnWidth(), compact.columnWidth());
        assertEquals(203, compact.cardWidth());
    }
}
