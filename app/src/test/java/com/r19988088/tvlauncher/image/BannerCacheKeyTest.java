package com.r19988088.tvlauncher.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public final class BannerCacheKeyTest {
    @Test
    public void keyChangesForAppUpdateAndVisualSize() {
        BannerCacheKey base = new BannerCacheKey("pkg/.Main", 10L, 264, 148, 60);

        assertNotEquals(base, new BannerCacheKey("pkg/.Main", 11L, 264, 148, 60));
        assertNotEquals(base, new BannerCacheKey("pkg/.Main", 10L, 300, 168, 60));
        assertNotEquals(base, new BannerCacheKey("pkg/.Main", 10L, 264, 148, 70));
        assertEquals(64, base.fileName().length());
        assertEquals(
                "318ad8c7aad1278aa07309e9c534966de0e25911243d6c8fa80e0993bf864ed1",
                base.fileName());
    }
}
