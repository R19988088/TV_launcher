package com.r19988088.tvlauncher.ui;

public final class SettingsCategoryNavigator {
    public static final int APPS = 0;
    public static final int DESKTOP = 1;
    public static final int WALLPAPER = 2;

    private int selected = APPS;
    private boolean entered;

    public void reset() {
        selected = APPS;
        entered = false;
    }

    public void select(int category) {
        selected = category >= APPS && category <= WALLPAPER ? category : APPS;
    }

    public void enter() {
        entered = true;
    }

    public boolean backShouldClose() {
        if (!entered) return true;
        entered = false;
        return false;
    }

    public int selected() {
        return selected;
    }

    public boolean isEntered() {
        return entered;
    }
}
