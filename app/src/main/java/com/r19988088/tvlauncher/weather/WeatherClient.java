package com.r19988088.tvlauncher.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;

public final class WeatherClient {
    public String fetch() throws IOException {
        try {
            JSONObject location = request("https://ipwho.is/");
            if (!location.optBoolean("success", false)) {
                throw new IOException("IP location failed");
            }
            double latitude = location.getDouble("latitude");
            double longitude = location.getDouble("longitude");
            String city = location.optString("city", "");
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code&timezone=auto",
                    latitude, longitude);
            JSONObject current = request(url).getJSONObject("current");
            int temperature = (int) Math.round(current.getDouble("temperature_2m"));
            String weather = WeatherDescription.fromCode(current.getInt("weather_code"));
            return city.isEmpty()
                    ? weather + "  " + temperature + "°"
                    : city + "  " + weather + "  " + temperature + "°";
        } catch (org.json.JSONException malformed) {
            throw new IOException("Malformed weather response", malformed);
        }
    }

    private static JSONObject request(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status);
            }
            StringBuilder json = new StringBuilder(512);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            return new JSONObject(json.toString());
        } catch (org.json.JSONException malformed) {
            throw new IOException("Malformed weather response", malformed);
        } finally {
            connection.disconnect();
        }
    }
}
