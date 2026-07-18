package com.r19988088.tvlauncher.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AppRepositoryTest {
    @Test
    public void buildsPackageUriForSystemUninstaller() {
        assertEquals("package:com.example.tv", AppRepository.uninstallUriFor("com.example.tv"));
    }
}
