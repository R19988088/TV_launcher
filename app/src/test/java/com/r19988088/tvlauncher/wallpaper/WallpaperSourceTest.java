package com.r19988088.tvlauncher.wallpaper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class WallpaperSourceTest {
    @Test
    public void extractsWallpaperDetailPagesFromSearchHtml() {
        String html = "<a class=\"wallpapers__canvas_image\" "
                + "href=\"https://4kwallpapers.com/abstract/one-1.html\">one</a>"
                + "<a href=\"https://4kwallpapers.com/abstract/two-2.html\" "
                + "class=\"wallpapers__canvas_image other\">two</a>";

        assertEquals(
                Arrays.asList(
                        "https://4kwallpapers.com/abstract/one-1.html",
                        "https://4kwallpapers.com/abstract/two-2.html"),
                WallpaperSource.detailPages(html));
    }

    @Test
    public void readsLastSearchPageAndBuildsSitePageAddress() {
        String html = "<p class=\"pages\"><strong data-page=\"1\">1</strong>"
                + "<strong data-page=\"2\">2</strong>"
                + "<strong data-page=\"57\">57</strong></p>";

        assertEquals(57, WallpaperSource.pageCount(html));
        assertEquals(
                "https://4kwallpapers.com/search/Abstract?page=2",
                WallpaperSource.searchPage("Abstract", 2));
        assertEquals(RandomWallpaperClient.DEFAULT_SOURCE,
                WallpaperSource.searchPage("Abstract", 1));
    }

    @Test
    public void selectsDownloadThatMatchesPhysicalDisplayWidth() {
        String html = "<a href=\"/images/wallpapers/abstract-3840x2160-1.jpg\">4K</a>"
                + "<a href=\"/images/wallpapers/abstract-2560x1440-1.jpg\">2K</a>"
                + "<a href=\"/images/wallpapers/abstract-1280x720-1.jpg\">HD</a>"
                + "<a href=\"/images/wallpapers/abstract-1920x1080-1.jpg\">Full HD</a>";

        assertEquals(
                "https://4kwallpapers.com/images/wallpapers/abstract-1280x720-1.jpg",
                WallpaperSource.desktopImage(
                        "https://4kwallpapers.com/abstract/one-1.html", html, 1280));
        assertEquals(
                "https://4kwallpapers.com/images/wallpapers/abstract-1920x1080-1.jpg",
                WallpaperSource.desktopImage(
                        "https://4kwallpapers.com/abstract/one-1.html", html, 1920));
        assertEquals(
                "https://4kwallpapers.com/images/wallpapers/abstract-2560x1440-1.jpg",
                WallpaperSource.desktopImage(
                        "https://4kwallpapers.com/abstract/one-1.html", html, 2560));
        assertEquals(
                "https://4kwallpapers.com/images/wallpapers/abstract-3840x2160-1.jpg",
                WallpaperSource.desktopImage(
                        "https://4kwallpapers.com/abstract/one-1.html", html, 3840));
    }

    @Test
    public void acceptsSitesGenericBinaryImageContentType() {
        assertEquals(true, RandomWallpaperClient.isWallpaperResponse("binary/data"));
        assertEquals(true, RandomWallpaperClient.isWallpaperResponse("image/jpeg"));
        assertEquals(false, RandomWallpaperClient.isWallpaperResponse("text/html"));
    }
}
