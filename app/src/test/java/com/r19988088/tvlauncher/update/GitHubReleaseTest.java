package com.r19988088.tvlauncher.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GitHubReleaseTest {
    @Test
    public void selectsApkAssetFromLatestRelease() throws Exception {
        String json = "{\"tag_name\":\"v0.10\",\"assets\":["
                + "{\"name\":\"source.zip\",\"browser_download_url\":\"https://x/source.zip\"},"
                + "{\"name\":\"TVLauncher-0.10.apk\","
                + "\"browser_download_url\":\"https://x/TVLauncher-0.10.apk\"}]}";

        GitHubRelease release = GitHubRelease.parse(json);

        assertEquals("0.10", release.version());
        assertEquals("https://x/TVLauncher-0.10.apk", release.apkUrl());
    }

    @Test
    public void comparesNumericVersionComponents() {
        assertTrue(GitHubRelease.isNewer("0.10", "0.9.1"));
        assertTrue(GitHubRelease.isNewer("1.0", "0.10"));
        assertFalse(GitHubRelease.isNewer("0.9.1", "0.9.1"));
        assertFalse(GitHubRelease.isNewer("0.9", "0.9.1"));
    }
}
