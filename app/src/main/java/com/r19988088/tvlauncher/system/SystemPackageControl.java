package com.r19988088.tvlauncher.system;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.IOException;
import rikka.shizuku.Shizuku;

public final class SystemPackageControl {
    private final PackageManager packageManager;
    private final LocalAdbShell localAdbShell;

    public SystemPackageControl(Context context) {
        packageManager = context.getPackageManager();
        localAdbShell = new LocalAdbShell(context.getNoBackupFilesDir());
    }

    public boolean isDisabled(String packageName) {
        int state = packageManager.getApplicationEnabledSetting(packageName);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
    }

    public boolean setDisabledViaLocalAdb(String packageName, boolean disabled) {
        try {
            localAdbShell.execute(localAdbCommandFor(packageName, disabled));
            return isDisabled(packageName) == disabled;
        } catch (Exception failure) {
            return false;
        }
    }

    public boolean uninstallViaLocalAdb(String packageName) {
        try {
            localAdbShell.execute(localAdbUninstallCommandFor(packageName));
            return !isInstalled(packageName);
        } catch (Exception failure) {
            return false;
        }
    }

    public boolean setDisabledViaShizuku(String packageName, boolean disabled)
            throws IOException, InterruptedException {
        return executeViaShizuku(commandFor(packageName, disabled));
    }

    public boolean uninstallViaShizuku(String packageName)
            throws IOException, InterruptedException {
        return executeViaShizuku(
                new String[] {"pm", "uninstall", "--user", "0", packageName})
                && !isInstalled(packageName);
    }

    private boolean executeViaShizuku(String[] command) throws IOException, InterruptedException {
        Process process = Shizuku.newProcess(command, null, null);
        try {
            return process.waitFor() == 0;
        } finally {
            process.destroy();
        }
    }

    static String[] commandFor(String packageName, boolean disabled) {
        return disabled
                ? new String[] {"pm", "disable-user", "--user", "0", packageName}
                : new String[] {"pm", "enable", "--user", "0", packageName};
    }

    static String localAdbCommandFor(String packageName, boolean disabled) {
        return String.join(" ", commandFor(packageName, disabled));
    }

    static String localAdbUninstallCommandFor(String packageName) {
        return "pm uninstall --user 0 " + packageName;
    }

    private boolean isInstalled(String packageName) {
        try {
            packageManager.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException removed) {
            return false;
        }
    }
}
