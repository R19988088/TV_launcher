package com.r19988088.tvlauncher.ui;

import java.math.BigDecimal;

public final class ScaleValueFormatter {
    private ScaleValueFormatter() {}

    public static String format(int percent) {
        return BigDecimal.valueOf(percent, 2).stripTrailingZeros().toPlainString();
    }
}
