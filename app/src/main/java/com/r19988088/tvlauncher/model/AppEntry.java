package com.r19988088.tvlauncher.model;

import android.content.ComponentName;

public final class AppEntry {
    private final ComponentName componentName;
    private final String label;
    private final long lastUpdateTime;

    public AppEntry(ComponentName componentName, String label, long lastUpdateTime) {
        this.componentName = componentName;
        this.label = label;
        this.lastUpdateTime = lastUpdateTime;
    }

    public ComponentName componentName() {
        return componentName;
    }

    public String componentId() {
        return componentName.flattenToString();
    }

    public String label() {
        return label;
    }

    public long lastUpdateTime() {
        return lastUpdateTime;
    }
}

