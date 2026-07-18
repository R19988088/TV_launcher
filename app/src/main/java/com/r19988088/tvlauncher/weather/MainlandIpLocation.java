package com.r19988088.tvlauncher.weather;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainlandIpLocation {
    private static final Pattern RESPONSE = Pattern.compile(
            "IP[：:]\\s*([0-9a-fA-F:.]+).*来自于[：:]中国\\s+\\S+\\s+(\\S+)");

    private final String ip;
    private final String city;

    private MainlandIpLocation(String ip, String city) {
        this.ip = ip;
        this.city = city;
    }

    public static MainlandIpLocation parse(String response) {
        Matcher matcher = RESPONSE.matcher(response);
        if (!matcher.find()) throw new IllegalArgumentException("Unknown IP location response");
        return new MainlandIpLocation(matcher.group(1), matcher.group(2));
    }

    public String ip() {
        return ip;
    }

    public String city() {
        return city;
    }
}
