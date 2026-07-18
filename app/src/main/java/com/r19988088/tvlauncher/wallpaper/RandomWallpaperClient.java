package com.r19988088.tvlauncher.wallpaper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class RandomWallpaperClient {
    public static final String DEFAULT_SOURCE = "https://4kwallpapers.com/search/?q=Abstract";
    private static final int MAX_HTML_BYTES = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 24 * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36 Safari/537.36";

    public File download(File directory, int physicalWidth) throws IOException {
        Random random = new Random();
        String firstPage = fetchText(DEFAULT_SOURCE);
        int page = 1 + random.nextInt(WallpaperSource.pageCount(firstPage));
        String searchPage = firstPage;
        if (page > 1) {
            try {
                searchPage = fetchText(WallpaperSource.searchPage("Abstract", page));
            } catch (IOException ignored) {}
        }
        List<String> details = WallpaperSource.detailPages(searchPage);
        if (details.isEmpty() && searchPage != firstPage) {
            details = WallpaperSource.detailPages(firstPage);
        }
        if (details.isEmpty()) throw new IOException("No wallpapers found");
        String detail = details.get(random.nextInt(details.size()));
        String image = WallpaperSource.desktopImage(detail, fetchText(detail), physicalWidth);
        if (image == null) throw new IOException("No desktop resolution found");
        return downloadImage(image, directory);
    }

    private String fetchText(String address) throws IOException {
        HttpURLConnection connection = open(address);
        try (InputStream input = connection.getInputStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output, MAX_HTML_BYTES);
            return output.toString(StandardCharsets.UTF_8.name());
        } finally {
            connection.disconnect();
        }
    }

    private File downloadImage(String address, File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create wallpaper directory");
        }
        File target = new File(directory, "random-wallpaper.image");
        File temporary = new File(directory, "random-wallpaper.download");
        HttpURLConnection connection = open(address);
        String contentType = connection.getContentType();
        if (!isWallpaperResponse(contentType)) {
            connection.disconnect();
            throw new IOException("Wallpaper response is not an image");
        }
        try (InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(temporary)) {
            copy(input, output, MAX_IMAGE_BYTES);
        } catch (IOException failure) {
            temporary.delete();
            throw failure;
        } finally {
            connection.disconnect();
        }
        if ((target.exists() && !target.delete()) || !temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Cannot save wallpaper");
        }
        return target;
    }

    static boolean isWallpaperResponse(String contentType) {
        if (contentType == null) return false;
        String normalized = contentType.toLowerCase(Locale.US);
        return !normalized.startsWith("text/")
                && !normalized.contains("html")
                && !normalized.contains("json")
                && !normalized.contains("xml");
    }

    private HttpURLConnection open(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("HTTP " + status);
        }
        return connection;
    }

    private static void copy(InputStream input, java.io.OutputStream output, int limit)
            throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IOException("Download too large");
            output.write(buffer, 0, read);
        }
    }
}
