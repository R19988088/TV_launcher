package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GridFocusNavigatorTest {
    @Test
    public void horizontalMovementNeverLeavesARealItemOrCrossesRows() {
        assertEquals(0, GridFocusNavigator.move(0, 8, 6, GridFocusNavigator.LEFT));
        assertEquals(0, GridFocusNavigator.move(1, 8, 6, GridFocusNavigator.LEFT));
        assertEquals(5, GridFocusNavigator.move(5, 8, 6, GridFocusNavigator.RIGHT));
        assertEquals(6, GridFocusNavigator.move(6, 8, 6, GridFocusNavigator.LEFT));
        assertEquals(7, GridFocusNavigator.move(7, 8, 6, GridFocusNavigator.RIGHT));
    }

    @Test
    public void verticalMovementNeverTargetsAnEmptyCell() {
        assertEquals(6, GridFocusNavigator.move(0, 8, 6, GridFocusNavigator.DOWN));
        assertEquals(2, GridFocusNavigator.move(2, 8, 6, GridFocusNavigator.DOWN));
        assertEquals(1, GridFocusNavigator.move(7, 8, 6, GridFocusNavigator.UP));
        assertEquals(1, GridFocusNavigator.move(1, 8, 6, GridFocusNavigator.UP));
    }
}
