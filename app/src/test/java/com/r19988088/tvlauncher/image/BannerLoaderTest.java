package com.r19988088.tvlauncher.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BannerLoaderTest {
    @Test
    public void onlyPositiveDimensionsCanBeRendered() {
        assertFalse(BannerLoader.hasRenderableSize(0, 148));
        assertFalse(BannerLoader.hasRenderableSize(264, 0));
        assertFalse(BannerLoader.hasRenderableSize(-1, 148));
        assertTrue(BannerLoader.hasRenderableSize(264, 148));
    }
}
