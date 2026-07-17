package com.r19988088.tvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.r19988088.tvlauncher.data.AppRepository;
import com.r19988088.tvlauncher.data.LauncherPreferences;
import com.r19988088.tvlauncher.data.LauncherState;
import com.r19988088.tvlauncher.model.AppEntry;
import com.r19988088.tvlauncher.model.LauncherSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsActivity extends Activity {
    private static final int REQUEST_WALLPAPER = 20;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<String> selectedIds = new ArrayList<>();

    private LauncherPreferences preferences;
    private LauncherState state;
    private List<AppEntry> discovered = new ArrayList<>();
    private ListView appList;
    private ProgressBar loading;
    private TextView columnsValue;
    private TextView cardValue;
    private TextView iconValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        enterImmersiveMode();
        preferences = new LauncherPreferences(this);
        state = preferences.load();
        selectedIds.addAll(state.componentIds());

        appList = findViewById(R.id.app_list);
        loading = findViewById(R.id.app_loading);
        columnsValue = findViewById(R.id.columns_value);
        cardValue = findViewById(R.id.card_value);
        iconValue = findViewById(R.id.icon_value);

        findViewById(R.id.columns_minus).setOnClickListener(view -> changeColumns(-1));
        findViewById(R.id.columns_plus).setOnClickListener(view -> changeColumns(1));
        findViewById(R.id.card_minus).setOnClickListener(view -> changeCardScale(-5));
        findViewById(R.id.card_plus).setOnClickListener(view -> changeCardScale(5));
        findViewById(R.id.icon_minus).setOnClickListener(view -> changeIconScale(-5));
        findViewById(R.id.icon_plus).setOnClickListener(view -> changeIconScale(5));
        findViewById(R.id.wallpaper_button).setOnClickListener(view -> chooseWallpaper());
        appList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        appList.setOnItemClickListener((parent, view, position, id) -> toggleApp(position));
        updateValues();
        loadApps();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP
                && (event.getKeyCode() == KeyEvent.KEYCODE_MENU
                || event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS)) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_WALLPAPER || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            state = new LauncherState(selectedIds, state.settings(), uri.toString());
            preferences.save(state);
            Toast.makeText(this, R.string.wallpaper_saved, Toast.LENGTH_SHORT).show();
        } catch (SecurityException failure) {
            Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void loadApps() {
        loading.setVisibility(View.VISIBLE);
        final AppRepository repository = new AppRepository(this);
        executor.execute(() -> {
            final List<AppEntry> entries = repository.discoverLaunchableApps();
            mainHandler.post(() -> {
                if (isFinishing()) {
                    return;
                }
                discovered = entries;
                List<String> labels = new ArrayList<>(entries.size());
                for (AppEntry entry : entries) {
                    labels.add(entry.label());
                }
                appList.setAdapter(new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_multiple_choice, labels));
                for (int index = 0; index < entries.size(); index++) {
                    appList.setItemChecked(index, selectedIds.contains(entries.get(index).componentId()));
                }
                loading.setVisibility(View.GONE);
                if (!entries.isEmpty()) {
                    appList.requestFocus();
                }
            });
        });
    }

    private void toggleApp(int position) {
        if (position < 0 || position >= discovered.size()) {
            return;
        }
        String componentId = discovered.get(position).componentId();
        if (selectedIds.contains(componentId)) {
            selectedIds.remove(componentId);
            appList.setItemChecked(position, false);
        } else {
            selectedIds.add(componentId);
            appList.setItemChecked(position, true);
        }
        saveCurrentState();
    }

    private void changeColumns(int delta) {
        LauncherSettings current = state.settings();
        updateSettings(new LauncherSettings(
                current.columns() + delta,
                current.cardScalePercent(),
                current.iconScalePercent()));
    }

    private void changeCardScale(int delta) {
        LauncherSettings current = state.settings();
        updateSettings(new LauncherSettings(
                current.columns(),
                current.cardScalePercent() + delta,
                current.iconScalePercent()));
    }

    private void changeIconScale(int delta) {
        LauncherSettings current = state.settings();
        updateSettings(new LauncherSettings(
                current.columns(),
                current.cardScalePercent(),
                current.iconScalePercent() + delta));
    }

    private void updateSettings(LauncherSettings settings) {
        state = new LauncherState(selectedIds, settings, state.wallpaperUri());
        preferences.save(state);
        updateValues();
    }

    private void saveCurrentState() {
        state = new LauncherState(selectedIds, state.settings(), state.wallpaperUri());
        preferences.save(state);
    }

    private void updateValues() {
        LauncherSettings settings = state.settings();
        columnsValue.setText(String.valueOf(settings.columns()));
        cardValue.setText(getString(R.string.percent_value, settings.cardScalePercent()));
        iconValue.setText(getString(R.string.percent_value, settings.iconScalePercent()));
    }

    private void chooseWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_WALLPAPER);
        } catch (android.content.ActivityNotFoundException failure) {
            Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
