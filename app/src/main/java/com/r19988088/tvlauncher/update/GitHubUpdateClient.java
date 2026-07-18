package com.r19988088.tvlauncher.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GitHubUpdateClient {
    private static final String LATEST_RELEASE =
            "https://api.github.com/repos/R19988088/TV_launcher/releases/latest";
    private static final int MAX_JSON_BYTES = 1024 * 1024;
    private static final int MAX_APK_BYTES = 32 * 1024 * 1024;

    public GitHubRelease latest() throws IOException {
        HttpURLConnection connection = open(LATEST_RELEASE, "application/vnd.github+json");
        try (InputStream input = connection.getInputStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output, MAX_JSON_BYTES);
            try {
                return GitHubRelease.parse(output.toString(StandardCharsets.UTF_8.name()));
            } catch (org.json.JSONException malformed) {
                throw new IOException("Malformed GitHub release", malformed);
            }
        } finally {
            connection.disconnect();
        }
    }

    public File download(GitHubRelease release, File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create update directory");
        }
        File target = new File(directory, "update.apk");
        File temporary = new File(directory, "update.download");
        HttpURLConnection connection = open(release.apkUrl(), "application/vnd.android.package-archive");
        try (InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(temporary)) {
            copy(input, output, MAX_APK_BYTES);
        } catch (IOException failure) {
            temporary.delete();
            throw failure;
        } finally {
            connection.disconnect();
        }
        if ((target.exists() && !target.delete()) || !temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Cannot store update");
        }
        return target;
    }

    private static HttpURLConnection open(String address, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("User-Agent", "TVLauncher-Android");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("GitHub HTTP " + status);
        }
        return connection;
    }

    private static void copy(InputStream input, java.io.OutputStream output, int limit)
            throws IOException {
        byte[] buffer = new byte[32 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IOException("Update response too large");
            output.write(buffer, 0, read);
        }
    }
}
