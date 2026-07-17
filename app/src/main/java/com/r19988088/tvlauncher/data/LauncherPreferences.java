package com.r19988088.tvlauncher.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class LauncherPreferences {
    private static final String FILE_NAME = "launcher_state";
    private static final String KEY_STATE = "state";

    private final SharedPreferences preferences;

    public LauncherPreferences(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public LauncherState load() {
        return LauncherStateCodec.decode(preferences.getString(KEY_STATE, ""));
    }

    public void save(LauncherState state) {
        preferences.edit().putString(KEY_STATE, LauncherStateCodec.encode(state)).apply();
    }
}
