package com.r19988088.tvlauncher.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WeatherDescriptionTest {
    @Test
    public void mapsOpenMeteoCodesToCompactChineseText() {
        assertEquals("晴", WeatherDescription.fromCode(0));
        assertEquals("多云", WeatherDescription.fromCode(3));
        assertEquals("小雨", WeatherDescription.fromCode(61));
        assertEquals("雷雨", WeatherDescription.fromCode(95));
        assertEquals("天气", WeatherDescription.fromCode(999));
    }
}
