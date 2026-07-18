package com.r19988088.tvlauncher.data;

import com.r19988088.tvlauncher.model.LauncherSettings;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class LauncherStateCodec {
    private LauncherStateCodec() {}

    public static String encode(LauncherState state) {
        try {
            JSONObject object = new JSONObject();
            object.put("components", new JSONArray(state.componentIds()));
            object.put("columns", state.settings().columns());
            object.put("cardScale", state.settings().cardScalePercent());
            object.put("iconScale", state.settings().iconScalePercent());
            object.put("topBlankRows", state.settings().topBlankRows());
            object.put("spacingScale", state.settings().spacingScalePercent());
            object.put("wallpaper", state.wallpaperUri());
            return object.toString();
        } catch (JSONException impossible) {
            return "{}";
        }
    }

    public static LauncherState decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return LauncherState.defaults();
        }
        try {
            JSONObject object = new JSONObject(encoded);
            JSONArray componentsJson = object.optJSONArray("components");
            List<String> components = new ArrayList<>();
            if (componentsJson != null) {
                for (int index = 0; index < componentsJson.length(); index++) {
                    String component = componentsJson.optString(index, "");
                    if (!component.isEmpty()) {
                        components.add(component);
                    }
                }
            }
            LauncherSettings settings = new LauncherSettings(
                    object.optInt("columns", LauncherSettings.DEFAULT_COLUMNS),
                    object.optInt("cardScale", LauncherSettings.DEFAULT_CARD_SCALE_PERCENT),
                    object.optInt("iconScale", LauncherSettings.DEFAULT_ICON_SCALE_PERCENT),
                    object.optInt("topBlankRows", LauncherSettings.DEFAULT_TOP_BLANK_ROWS),
                    object.optInt("spacingScale", LauncherSettings.DEFAULT_SPACING_SCALE_PERCENT));
            return new LauncherState(
                    components, settings, object.optString("wallpaper", ""));
        } catch (JSONException malformed) {
            return LauncherState.defaults();
        }
    }
}
