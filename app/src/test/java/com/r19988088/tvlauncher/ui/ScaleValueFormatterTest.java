package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ScaleValueFormatterTest {
    @Test
    public void displaysPercentStorageAsPlainMultiplier() {
        assertEquals("0.5", ScaleValueFormatter.format(50));
        assertEquals("1", ScaleValueFormatter.format(100));
        assertEquals("1.1", ScaleValueFormatter.format(110));
        assertEquals("2", ScaleValueFormatter.format(200));
    }
}
