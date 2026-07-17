package com.r19988088.tvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.r19988088.tvlauncher.data.AppRepository;
import com.r19988088.tvlauncher.data.LauncherPreferences;
import com.r19988088.tvlauncher.data.LauncherState;
import com.r19988088.tvlauncher.display.DisplayModeController;
import com.r19988088.tvlauncher.image.BannerLoader;
import com.r19988088.tvlauncher.model.AppEntry;
import com.r19988088.tvlauncher.model.ReorderSession;
import com.r19988088.tvlauncher.ui.AppGridAdapter;
import com.r19988088.tvlauncher.ui.LauncherGridView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LauncherActivity extends Activity implements AppGridAdapter.Listener {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService repositoryExecutor = Executors.newSingleThreadExecutor();

    private LauncherGridView gridView;
    private TextView emptyPrompt;
    private ImageView wallpaperView;
    private LauncherPreferences preferences;
    private AppRepository appRepository;
    private BannerLoader bannerLoader;
    private AppGridAdapter adapter;
    private LauncherState state = LauncherState.defaults();
    private List<AppEntry> entries = new ArrayList<>();
    private ReorderSession reorderSession;
    private int loadGeneration;
    private Bitmap customWallpaper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        enterImmersiveMode();
        DisplayModeController.requestBestMode(getWindow());

        wallpaperView = findViewById(R.id.wallpaper);
        gridView = findViewById(R.id.app_grid);
        emptyPrompt = findViewById(R.id.empty_prompt);
        preferences = new LauncherPreferences(this);
        appRepository = new AppRepository(this);
        bannerLoader = new BannerLoader(this);
        adapter = new AppGridAdapter(this, bannerLoader, this);
        gridView.setAdapter(adapter);
        gridView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View view,
                    int left,
                    int top,
                    int right,
                    int bottom,
                    int oldLeft,
                    int oldTop,
                    int oldRight,
                    int oldBottom) {
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    configureGrid();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        state = preferences.load();
        configureGrid();
        loadWallpaper();
        refreshDesktop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openSettings();
            return true;
        }
        if (reorderSession != null && handleMoveKey(keyCode)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (reorderSession != null) {
            cancelMove();
        }
    }

    @Override
    public void onLaunch(AppEntry entry) {
        if (reorderSession != null) {
            commitMove();
            return;
        }
        try {
            startActivity(appRepository.launchIntent(entry));
        } catch (ActivityNotFoundException | SecurityException failure) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            refreshDesktop();
        }
    }

    @Override
    public boolean onLongPress(final AppEntry entry) {
        if (reorderSession != null) {
            return true;
        }
        new AlertDialog.Builder(this)
                .setTitle(entry.label())
                .setItems(R.array.app_actions, (dialog, which) -> {
                    if (which == 0) {
                        beginMove(entry);
                    } else {
                        hide(entry);
                    }
                })
                .show();
        return true;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        bannerLoader.trimMemory(level);
    }

    @Override
    protected void onDestroy() {
        loadGeneration++;
        repositoryExecutor.shutdownNow();
        bannerLoader.close();
        if (customWallpaper != null) {
            customWallpaper.recycle();
        }
        super.onDestroy();
    }

    private void refreshDesktop() {
        final int generation = ++loadGeneration;
        final List<String> requested = state.componentIds();
        if (requested.isEmpty()) {
            applyEntries(Collections.<AppEntry>emptyList());
            return;
        }
        repositoryExecutor.execute(() -> {
            final List<AppEntry> resolved = appRepository.resolveOrdered(requested);
            mainHandler.post(() -> {
                if (generation != loadGeneration || isFinishing()) {
                    return;
                }
                if (resolved.size() != requested.size()) {
                    state = new LauncherState(componentIds(resolved), state.settings(), state.wallpaperUri());
                    preferences.save(state);
                }
                applyEntries(resolved);
            });
        });
    }

    private void applyEntries(List<AppEntry> resolved) {
        entries = new ArrayList<>(resolved);
        adapter.replace(entries);
        boolean empty = entries.isEmpty();
        emptyPrompt.setVisibility(empty ? View.VISIBLE : View.GONE);
        gridView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            gridView.post(() -> {
                gridView.setSelection(0);
                View first = gridView.getChildAt(0);
                if (first != null) {
                    first.requestFocus();
                } else {
                    gridView.requestFocus();
                }
            });
        }
    }

    private void configureGrid() {
        if (gridView == null || gridView.getWidth() <= 0 || gridView.getHeight() <= 0) {
            return;
        }
        int columns = state.settings().columns();
        int spacing = dp(8);
        int horizontalPadding = dp(54);
        gridView.setNumColumns(columns);
        gridView.setHorizontalSpacing(spacing);
        gridView.setVerticalSpacing(dp(8));
        gridView.setPadding(
                horizontalPadding,
                Math.round(gridView.getHeight() * 0.27f),
                horizontalPadding,
                dp(48));
        int available = gridView.getWidth() - horizontalPadding * 2 - spacing * (columns - 1);
        int cellWidth = Math.max(dp(120), available / columns);
        int baseWidth = Math.max(dp(110), cellWidth - dp(34));
        int cardWidth = baseWidth * state.settings().cardScalePercent() / 100;
        int cardHeight = cardWidth * 9 / 16;
        adapter.setCardMetrics(
                cardWidth, cardHeight, state.settings().iconScalePercent());
    }

    private boolean handleMoveKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            commitMove();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelMove();
            return true;
        }
        int selected = reorderSession.selectedIndex();
        int columns = state.settings().columns();
        int target = selected;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && selected % columns > 0) {
            target--;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                && selected % columns < columns - 1
                && selected + 1 < entries.size()) {
            target++;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && selected >= columns) {
            target -= columns;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && selected + columns < entries.size()) {
            target += columns;
        }
        if (target == selected) {
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
        }
        reorderSession.swapWith(target);
        Collections.swap(entries, selected, target);
        adapter.replace(entries);
        final int focusTarget = target;
        gridView.post(() -> {
            gridView.setSelection(focusTarget);
            int childIndex = focusTarget - gridView.getFirstVisiblePosition();
            View child = gridView.getChildAt(childIndex);
            if (child != null) {
                child.requestFocus();
            }
        });
        return true;
    }

    private void beginMove(AppEntry entry) {
        int index = indexOf(entry.componentId());
        if (index < 0) {
            return;
        }
        reorderSession = new ReorderSession(componentIds(entries), index);
        Toast.makeText(this, R.string.move_hint, Toast.LENGTH_SHORT).show();
    }

    private void commitMove() {
        if (reorderSession == null) {
            return;
        }
        state = new LauncherState(reorderSession.commit(), state.settings(), state.wallpaperUri());
        preferences.save(state);
        reorderSession = null;
        Toast.makeText(this, R.string.move_saved, Toast.LENGTH_SHORT).show();
    }

    private void cancelMove() {
        List<String> original = reorderSession.cancel();
        reorderSession = null;
        entries = orderEntries(entries, original);
        adapter.replace(entries);
        Toast.makeText(this, R.string.move_cancelled, Toast.LENGTH_SHORT).show();
    }

    private void hide(AppEntry entry) {
        List<String> ids = new ArrayList<>(state.componentIds());
        ids.remove(entry.componentId());
        state = new LauncherState(ids, state.settings(), state.wallpaperUri());
        preferences.save(state);
        for (int index = entries.size() - 1; index >= 0; index--) {
            if (entries.get(index).componentId().equals(entry.componentId())) {
                entries.remove(index);
            }
        }
        applyEntries(entries);
    }

    private void openSettings() {
        startActivity(new Intent().setClassName(
                getPackageName(), getPackageName() + ".SettingsActivity"));
    }

    private void loadWallpaper() {
        String uriText = state.wallpaperUri();
        if (uriText.isEmpty()) {
            replaceCustomWallpaper(null);
            wallpaperView.setImageResource(R.drawable.default_wallpaper);
            return;
        }
        final int generation = loadGeneration;
        final Uri uri = Uri.parse(uriText);
        repositoryExecutor.execute(() -> {
            final Bitmap decoded = decodeWallpaper(uri);
            mainHandler.post(() -> {
                if (generation != loadGeneration || isFinishing()) {
                    if (decoded != null) {
                        decoded.recycle();
                    }
                    return;
                }
                if (decoded == null) {
                    wallpaperView.setImageResource(R.drawable.default_wallpaper);
                } else {
                    replaceCustomWallpaper(decoded);
                    wallpaperView.setImageBitmap(decoded);
                }
            });
        });
    }

    private Bitmap decodeWallpaper(Uri uri) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
        int sample = 1;
        while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1080) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    private void replaceCustomWallpaper(Bitmap replacement) {
        if (customWallpaper != null && customWallpaper != replacement) {
            customWallpaper.recycle();
        }
        customWallpaper = replacement;
    }

    private int indexOf(String componentId) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).componentId().equals(componentId)) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> componentIds(List<AppEntry> source) {
        List<String> ids = new ArrayList<>(source.size());
        for (AppEntry entry : source) {
            ids.add(entry.componentId());
        }
        return ids;
    }

    private static List<AppEntry> orderEntries(List<AppEntry> source, List<String> ids) {
        Map<String, AppEntry> byId = new HashMap<>();
        for (AppEntry entry : source) {
            byId.put(entry.componentId(), entry);
        }
        List<AppEntry> ordered = new ArrayList<>();
        for (String id : ids) {
            AppEntry entry = byId.get(id);
            if (entry != null) {
                ordered.add(entry);
            }
        }
        return ordered;
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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
