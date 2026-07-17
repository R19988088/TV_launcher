package com.r19988088.tvlauncher.display;

import java.util.List;

public final class DisplayModeSelector {
    private static final float MAX_REFRESH_RATE = 120.01f;

    private DisplayModeSelector() {}

    public static Mode choose(Mode current, List<Mode> supportedModes) {
        Mode best = current;
        for (Mode candidate : supportedModes) {
            if (candidate.width() != current.width()
                    || candidate.height() != current.height()
                    || candidate.refreshRate() > MAX_REFRESH_RATE) {
                continue;
            }
            if (candidate.refreshRate() > best.refreshRate()) {
                best = candidate;
            }
        }
        return best;
    }

    public static final class Mode {
        private final int id;
        private final int width;
        private final int height;
        private final float refreshRate;

        public Mode(int id, int width, int height, float refreshRate) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.refreshRate = refreshRate;
        }

        public int id() {
            return id;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public float refreshRate() {
            return refreshRate;
        }
    }
}

