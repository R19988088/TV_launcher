package com.r19988088.tvlauncher.update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class GitHubRelease {
    private final String version;
    private final String apkUrl;

    private GitHubRelease(String version, String apkUrl) {
        this.version = version;
        this.apkUrl = apkUrl;
    }

    public static GitHubRelease parse(String json) throws JSONException {
        JSONObject release = new JSONObject(json);
        String tag = release.getString("tag_name");
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        JSONArray assets = release.getJSONArray("assets");
        for (int index = 0; index < assets.length(); index++) {
            JSONObject asset = assets.getJSONObject(index);
            if (asset.getString("name").toLowerCase().endsWith(".apk")) {
                return new GitHubRelease(version, asset.getString("browser_download_url"));
            }
        }
        throw new JSONException("Release has no APK asset");
    }

    public String version() {
        return version;
    }

    public String apkUrl() {
        return apkUrl;
    }

    public static boolean isNewer(String candidate, String current) {
        String[] left = candidate.split("\\.");
        String[] right = current.split("\\.");
        int length = Math.max(left.length, right.length);
        for (int index = 0; index < length; index++) {
            int leftPart = index < left.length ? number(left[index]) : 0;
            int rightPart = index < right.length ? number(right[index]) : 0;
            if (leftPart != rightPart) return leftPart > rightPart;
        }
        return false;
    }

    private static int number(String part) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) end++;
        return end == 0 ? 0 : Integer.parseInt(part.substring(0, end));
    }
}
