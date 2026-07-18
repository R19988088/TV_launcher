package com.r19988088.tvlauncher.wallpaper;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WallpaperSource {
    private static final Pattern ANCHOR = Pattern.compile("<a\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF = Pattern.compile(
            "\\bhref\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE = Pattern.compile(
            "\\bdata-page\\s*=\\s*[\\\"'](\\d+)[\\\"']", Pattern.CASE_INSENSITIVE);

    private WallpaperSource() {}

    public static List<String> detailPages(String html) {
        Set<String> links = new LinkedHashSet<>();
        Matcher anchors = ANCHOR.matcher(html);
        while (anchors.find()) {
            String tag = anchors.group();
            if (!tag.contains("wallpapers__canvas_image")) continue;
            Matcher href = HREF.matcher(tag);
            if (href.find()) links.add(href.group(1));
        }
        return new ArrayList<>(links);
    }

    public static int pageCount(String html) {
        int count = 1;
        Matcher pages = PAGE.matcher(html);
        while (pages.find()) count = Math.max(count, Integer.parseInt(pages.group(1)));
        return count;
    }

    public static String searchPage(String query, int page) {
        if (page <= 1) return RandomWallpaperClient.DEFAULT_SOURCE;
        return "https://4kwallpapers.com/search/" + query + "?page=" + page;
    }

    public static String desktopImage(String detailPage, String html, int physicalWidth) {
        String[] resolutions;
        if (physicalWidth >= 3840) {
            resolutions = new String[] {"3840x2160", "2560x1440", "1920x1080"};
        } else if (physicalWidth >= 2560) {
            resolutions = new String[] {"2560x1440", "1920x1080"};
        } else if (physicalWidth >= 1920) {
            resolutions = new String[] {"1920x1080"};
        } else {
            resolutions = new String[] {"1280x720", "1920x1080"};
        }
        String image = null;
        for (String resolution : resolutions) {
            image = imageForResolution(html, resolution);
            if (image != null) break;
        }
        return image == null ? null : URI.create(detailPage).resolve(image).toString();
    }

    private static String imageForResolution(String html, String resolution) {
        Matcher anchors = ANCHOR.matcher(html);
        while (anchors.find()) {
            Matcher href = HREF.matcher(anchors.group());
            if (href.find()) {
                String value = href.group(1);
                if (value.contains("/images/wallpapers/") && value.contains(resolution)) {
                    return value;
                }
            }
        }
        return null;
    }
}
