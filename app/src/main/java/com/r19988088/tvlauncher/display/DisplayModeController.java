package com.r19988088.tvlauncher.display;

import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

public final class DisplayModeController {
    private DisplayModeController() {}

    public static void requestBestMode(Window window) {
        try {
            requestBestModeInternal(window);
        } catch (RuntimeException ignored) {
            // Broken vendor display services must not prevent the HOME activity from starting.
        }
    }

    private static void requestBestModeInternal(Window window) {
        Display display = window.getWindowManager().getDefaultDisplay();
        Display.Mode current = display.getMode();
        Display.Mode[] supported = display.getSupportedModes();
        List<DisplayModeSelector.Mode> modes = new ArrayList<>(supported.length);
        for (Display.Mode mode : supported) {
            modes.add(new DisplayModeSelector.Mode(
                    mode.getModeId(),
                    mode.getPhysicalWidth(),
                    mode.getPhysicalHeight(),
                    mode.getRefreshRate()));
        }
        DisplayModeSelector.Mode selected = DisplayModeSelector.choose(
                new DisplayModeSelector.Mode(
                        current.getModeId(),
                        current.getPhysicalWidth(),
                        current.getPhysicalHeight(),
                        current.getRefreshRate()),
                modes);
        if (selected.id() == current.getModeId()) {
            return;
        }
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.preferredDisplayModeId = selected.id();
        window.setAttributes(attributes);
    }
}
