package com.r19988088.tvlauncher.data;

import com.r19988088.tvlauncher.model.LauncherSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LauncherState {
    private final List<String> componentIds;
    private final LauncherSettings settings;
    private final String wallpaperUri;

    public LauncherState(
            List<String> componentIds,
            LauncherSettings settings,
            String wallpaperUri) {
        this.componentIds = Collections.unmodifiableList(new ArrayList<>(componentIds));
        this.settings = settings;
        this.wallpaperUri = wallpaperUri == null ? "" : wallpaperUri;
    }

    public static LauncherState defaults() {
        return new LauncherState(
                Collections.<String>emptyList(), LauncherSettings.defaults(), "");
    }

    public List<String> componentIds() {
        return componentIds;
    }

    public LauncherSettings settings() {
        return settings;
    }

    public String wallpaperUri() {
        return wallpaperUri;
    }
}

