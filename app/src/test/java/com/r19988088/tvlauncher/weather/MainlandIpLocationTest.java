package com.r19988088.tvlauncher.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class MainlandIpLocationTest {
    @Test
    public void extractsMainlandIpCity() {
        MainlandIpLocation location = MainlandIpLocation.parse(
                "当前 IP：171.213.190.154  来自于：中国 四川 成都  电信");

        assertEquals("171.213.190.154", location.ip());
        assertEquals("成都", location.city());
    }
}
