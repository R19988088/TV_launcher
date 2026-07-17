package com.r19988088.tvlauncher.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.r19988088.tvlauncher.model.AppEntry;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AppRepository {
    private final PackageManager packageManager;
    private final String ownPackageName;

    public AppRepository(Context context) {
        packageManager = context.getPackageManager();
        ownPackageName = context.getPackageName();
    }

    public List<AppEntry> discoverLaunchableApps() {
        Map<String, AppEntry> unique = new LinkedHashMap<>();
        collect(Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        collect(Intent.CATEGORY_LAUNCHER, unique);
        List<AppEntry> entries = new ArrayList<>(unique.values());
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(entries, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry left, AppEntry right) {
                return collator.compare(left.label(), right.label());
            }
        });
        return entries;
    }

    public List<AppEntry> resolveOrdered(List<String> componentIds) {
        List<AppEntry> ordered = new ArrayList<>();
        for (String componentId : componentIds) {
            ComponentName component = ComponentName.unflattenFromString(componentId);
            if (component == null || ownPackageName.equals(component.getPackageName())) {
                continue;
            }
            try {
                ActivityInfo activity = packageManager.getActivityInfo(component, 0);
                if (!activity.enabled
                        || activity.applicationInfo == null
                        || !activity.applicationInfo.enabled
                        || !activity.exported) {
                    continue;
                }
                CharSequence loadedLabel = activity.loadLabel(packageManager);
                String label = loadedLabel == null
                        ? activity.packageName
                        : loadedLabel.toString();
                ordered.add(new AppEntry(
                        component, label, lastUpdateTime(component.getPackageName())));
            } catch (PackageManager.NameNotFoundException ignored) {
                // Removed components are omitted and cleaned from preferences by the caller.
            }
        }
        return ordered;
    }

    public Intent launchIntent(AppEntry entry) {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(entry.componentName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    private void collect(String category, Map<String, AppEntry> destination) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ResolveInfo> resolved = packageManager.queryIntentActivities(query, 0);
        for (ResolveInfo info : resolved) {
            ActivityInfo activity = info.activityInfo;
            if (activity == null
                    || !activity.enabled
                    || !activity.applicationInfo.enabled
                    || !activity.exported
                    || ownPackageName.equals(activity.packageName)) {
                continue;
            }
            ComponentName component = new ComponentName(activity.packageName, activity.name);
            String componentId = component.flattenToString();
            if (destination.containsKey(componentId)) {
                continue;
            }
            CharSequence loadedLabel = info.loadLabel(packageManager);
            String label = loadedLabel == null ? activity.packageName : loadedLabel.toString();
            destination.put(componentId, new AppEntry(
                    component, label, lastUpdateTime(activity.packageName)));
        }
    }

    private long lastUpdateTime(String packageName) {
        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            return info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0L;
        }
    }
}
