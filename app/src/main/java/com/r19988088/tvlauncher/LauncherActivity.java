package com.r19988088.tvlauncher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.r19988088.tvlauncher.data.AppRepository;
import com.r19988088.tvlauncher.data.LauncherPreferences;
import com.r19988088.tvlauncher.data.LauncherState;
import com.r19988088.tvlauncher.display.DisplayModeController;
import com.r19988088.tvlauncher.image.BannerLoader;
import com.r19988088.tvlauncher.model.AppEntry;
import com.r19988088.tvlauncher.model.LauncherSettings;
import com.r19988088.tvlauncher.model.ReorderSession;
import com.r19988088.tvlauncher.ui.AppCardView;
import com.r19988088.tvlauncher.ui.AppGridAdapter;
import com.r19988088.tvlauncher.ui.GridFocusNavigator;
import com.r19988088.tvlauncher.ui.GridMetrics;
import com.r19988088.tvlauncher.ui.LauncherGridLayout;
import com.r19988088.tvlauncher.weather.WeatherClient;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressLint("GestureBackNavigation")
public final class LauncherActivity extends Activity implements AppGridAdapter.Listener {
    private static final long REORDER_ANIMATION_DURATION_MS = 160L;
    private static final int REQUEST_WALLPAPER = 20;
    private static final int FIXED_ICON_SCALE_PERCENT = 60;
    private static final long WEATHER_REFRESH_MS = 60L * 60L * 1000L;
    private static final long WEATHER_RETRY_MS = 5L * 60L * 1000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService repositoryExecutor = Executors.newSingleThreadExecutor();
    private final ThreadPoolExecutor weatherExecutor = new ThreadPoolExecutor(
            0, 1, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1),
            new ThreadPoolExecutor.DiscardPolicy());
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private LauncherGridLayout gridView;
    private TextView emptyPrompt;
    private ImageView wallpaperView;
    private View settingsPanel;
    private ListView settingsAppList;
    private ProgressBar settingsAppLoading;
    private TextView columnsValue;
    private TextView cardValue;
    private TextView spacingValue;
    private TextView topRowsValue;
    private TextView clockView;
    private TextView weatherView;
    private LauncherPreferences preferences;
    private AppRepository appRepository;
    private BannerLoader bannerLoader;
    private AppGridAdapter adapter;
    private LauncherState state = LauncherState.defaults();
    private List<AppEntry> entries = new ArrayList<>();
    private List<AppEntry> discoveredApps = new ArrayList<>();
    private int activePosition = -1;
    private boolean centerLongPressed;
    private ReorderSession reorderSession;
    private boolean moveAnimationRunning;
    private int moveAnimationGeneration;
    private int loadGeneration;
    private int wallpaperLoadGeneration;
    private boolean weatherRequestRunning;
    private boolean activityResumed;
    private long nextWeatherRefreshAt;
    private Bitmap customWallpaper;
    private boolean packageReceiverRegistered;
    private final Runnable clockUpdater = new Runnable() {
        @Override
        public void run() {
            if (clockView == null) return;
            clockView.setText(clockFormat.format(new Date()));
            long delay = 60_000L - System.currentTimeMillis() % 60_000L;
            mainHandler.postDelayed(this, delay);
        }
    };
    private final Runnable weatherUpdater = new Runnable() {
        @Override
        public void run() {
            refreshWeather();
        }
    };
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            discardMoveSession();
            refreshDesktop();
        }
    };
    private final Runnable centerLongPress = new Runnable() {
        @Override
        public void run() {
            AppEntry active = activeEntry();
            if (active != null) {
                centerLongPressed = true;
                onLongPress(active);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        enterImmersiveMode();
        DisplayModeController.requestBestMode(getWindow());

        wallpaperView = findViewById(R.id.wallpaper);
        gridView = findViewById(R.id.app_grid);
        emptyPrompt = findViewById(R.id.empty_prompt);
        settingsPanel = findViewById(R.id.settings_panel);
        settingsAppList = findViewById(R.id.settings_app_list);
        settingsAppLoading = findViewById(R.id.settings_app_loading);
        columnsValue = findViewById(R.id.columns_value);
        cardValue = findViewById(R.id.card_value);
        spacingValue = findViewById(R.id.spacing_value);
        topRowsValue = findViewById(R.id.top_rows_value);
        clockView = findViewById(R.id.clock);
        weatherView = findViewById(R.id.weather);
        preferences = new LauncherPreferences(this);
        appRepository = new AppRepository(this);
        bannerLoader = new BannerLoader(this);
        adapter = new AppGridAdapter(this, bannerLoader, this);
        setupSettingsPanel();
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
                    if (!entries.isEmpty()) {
                        rebuildGrid();
                        activatePosition(Math.max(0, activePosition), false);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        enterImmersiveMode();
        discardMoveSession();
        state = preferences.load();
        configureGrid();
        loadWallpaper();
        refreshDesktop();
        startClock();
        refreshWeather();
    }

    @Override
    protected void onPause() {
        activityResumed = false;
        mainHandler.removeCallbacks(clockUpdater);
        mainHandler.removeCallbacks(weatherUpdater);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, filter);
        }
        packageReceiverRegistered = true;
    }

    @Override
    protected void onStop() {
        if (packageReceiverRegistered) {
            unregisterReceiver(packageReceiver);
            packageReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (isSettingsOpen()) {
            if (event.getAction() == KeyEvent.ACTION_UP
                    && (keyCode == KeyEvent.KEYCODE_MENU
                            || keyCode == KeyEvent.KEYCODE_SETTINGS)) {
                closeSettings();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if (reorderSession != null && isMoveCommandKey(keyCode)) {
            return event.getAction() != KeyEvent.ACTION_UP || handleMoveKey(keyCode);
        }
        if (isNavigationKey(keyCode)) {
            return event.getAction() != KeyEvent.ACTION_DOWN || handleNavigationKey(keyCode);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            return handleCenterKey(event);
        }
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openSettings();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (isSettingsOpen()) {
            closeSettings();
        } else if (reorderSession != null) {
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
        wallpaperLoadGeneration++;
        mainHandler.removeCallbacks(centerLongPress);
        mainHandler.removeCallbacks(weatherUpdater);
        repositoryExecutor.shutdownNow();
        weatherExecutor.shutdownNow();
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
        String focusedComponentId = focusedComponentId();
        entries = new ArrayList<>(resolved);
        configureGrid();
        boolean empty = entries.isEmpty();
        int restoredIndex = empty ? -1 : indexOf(focusedComponentId);
        activePosition = empty ? -1 : (restoredIndex >= 0 ? restoredIndex : 0);
        adapter.setActivePosition(activePosition);
        adapter.replace(entries);
        rebuildGrid();
        emptyPrompt.setVisibility(empty ? View.VISIBLE : View.GONE);
        gridView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            activatePosition(activePosition, false);
        }
    }

    private void configureGrid() {
        if (gridView == null || gridView.getWidth() <= 0 || gridView.getHeight() <= 0) {
            return;
        }
        int columns = state.settings().columns();
        GridMetrics metrics = GridMetrics.calculate(
                gridView.getWidth(),
                gridView.getHeight(),
                columns,
                entries.size(),
                state.settings().cardScalePercent(),
                state.settings().topBlankRows(),
                state.settings().spacingScalePercent(),
                getResources().getDisplayMetrics().density);
        gridView.setMetrics(metrics);
        adapter.setCardMetrics(
                metrics.columnWidth(),
                metrics.cardWidth(),
                metrics.cardHeight(),
                FIXED_ICON_SCALE_PERCENT);
    }

    private boolean handleNavigationKey(int keyCode) {
        if (entries.isEmpty()) {
            return true;
        }
        int columns = Math.max(1, Math.min(state.settings().columns(), entries.size()));
        int current = activePosition;
        if (current < 0 || current >= entries.size()) {
            current = 0;
        }
        int direction;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            direction = GridFocusNavigator.LEFT;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            direction = GridFocusNavigator.RIGHT;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            direction = GridFocusNavigator.UP;
        } else {
            direction = GridFocusNavigator.DOWN;
        }
        activatePosition(
                GridFocusNavigator.move(current, entries.size(), columns, direction), true);
        return true;
    }

    private static boolean isNavigationKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
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
        if (moveAnimationRunning
                && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == KeyEvent.KEYCODE_DPAD_UP
                        || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
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
        View selectedView = visibleCardAt(selected);
        View targetView = visibleCardAt(target);
        reorderSession.swapWith(target);
        Collections.swap(entries, selected, target);
        adapter.swap(selected, target);
        if (!animateSwap(selected, target, selectedView, targetView)) {
            adapter.replace(entries);
            rebuildGrid();
            focusPosition(target);
        }
        return true;
    }

    private static boolean isMoveCommandKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
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
        stopMoveAnimation();
        List<String> original = reorderSession.cancel();
        reorderSession = null;
        entries = orderEntries(entries, original);
        adapter.replace(entries);
        rebuildGrid();
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
        discardMoveSession();
        updateSettingsValues();
        settingsPanel.setVisibility(View.VISIBLE);
        settingsPanel.setAlpha(0f);
        settingsPanel.setTranslationX(dp(36));
        settingsPanel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180L)
                .start();
        findViewById(R.id.columns_minus).requestFocus();
        loadSettingsApps();
    }

    private void closeSettings() {
        settingsPanel.animate()
                .alpha(0f)
                .translationX(dp(36))
                .setDuration(140L)
                .withEndAction(() -> {
                    settingsPanel.setVisibility(View.GONE);
                    gridView.requestFocus();
                    activatePosition(Math.max(0, activePosition), false);
                })
                .start();
    }

    private boolean isSettingsOpen() {
        return settingsPanel != null && settingsPanel.getVisibility() == View.VISIBLE;
    }

    private void startClock() {
        mainHandler.removeCallbacks(clockUpdater);
        clockUpdater.run();
    }

    private void refreshWeather() {
        mainHandler.removeCallbacks(weatherUpdater);
        long remaining = nextWeatherRefreshAt - System.currentTimeMillis();
        if (remaining > 0L) {
            mainHandler.postDelayed(weatherUpdater, remaining);
            return;
        }
        if (weatherRequestRunning) return;
        weatherRequestRunning = true;
        nextWeatherRefreshAt = System.currentTimeMillis() + WEATHER_RETRY_MS;
        weatherExecutor.execute(() -> {
            String result = null;
            try {
                result = new WeatherClient().fetch();
            } catch (IOException ignored) {
                // Weather is optional and must never delay launcher startup.
            }
            final String weather = result;
            mainHandler.post(() -> {
                weatherRequestRunning = false;
                if (isFinishing() || isDestroyed()) return;
                if (weather != null) {
                    weatherView.setText(weather);
                    weatherView.setVisibility(View.VISIBLE);
                    nextWeatherRefreshAt = System.currentTimeMillis() + WEATHER_REFRESH_MS;
                }
                if (activityResumed) {
                    mainHandler.postDelayed(weatherUpdater,
                            Math.max(1L, nextWeatherRefreshAt - System.currentTimeMillis()));
                }
            });
        });
    }

    private boolean handleCenterKey(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                centerLongPressed = false;
                mainHandler.postDelayed(centerLongPress, ViewConfiguration.getLongPressTimeout());
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            mainHandler.removeCallbacks(centerLongPress);
            if (!centerLongPressed) {
                AppEntry active = activeEntry();
                if (active != null) {
                    onLaunch(active);
                }
            }
            centerLongPressed = false;
            return true;
        }
        return true;
    }

    private AppEntry activeEntry() {
        return activePosition >= 0 && activePosition < entries.size()
                ? entries.get(activePosition)
                : null;
    }

    private void setupSettingsPanel() {
        findViewById(R.id.columns_minus).setOnClickListener(view -> changeColumns(-1));
        findViewById(R.id.columns_plus).setOnClickListener(view -> changeColumns(1));
        findViewById(R.id.card_minus).setOnClickListener(view -> changeCardScale(-5));
        findViewById(R.id.card_plus).setOnClickListener(view -> changeCardScale(5));
        findViewById(R.id.spacing_minus).setOnClickListener(view -> changeSpacing(-10));
        findViewById(R.id.spacing_plus).setOnClickListener(view -> changeSpacing(10));
        findViewById(R.id.top_rows_minus).setOnClickListener(view -> changeTopRows(-1));
        findViewById(R.id.top_rows_plus).setOnClickListener(view -> changeTopRows(1));
        findViewById(R.id.wallpaper_button).setOnClickListener(view -> chooseWallpaper());
        settingsAppList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        settingsAppList.setOnItemClickListener((parent, view, position, id) -> toggleSettingsApp(position));
    }

    private void changeColumns(int delta) {
        LauncherSettings current = state.settings();
        applySettings(new LauncherSettings(
                current.columns() + delta,
                current.cardScalePercent(),
                current.iconScalePercent(),
                current.topBlankRows(),
                current.spacingScalePercent()));
    }

    private void changeCardScale(int delta) {
        LauncherSettings current = state.settings();
        applySettings(new LauncherSettings(
                current.columns(),
                current.cardScalePercent() + delta,
                current.iconScalePercent(),
                current.topBlankRows(),
                current.spacingScalePercent()));
    }

    private void changeSpacing(int delta) {
        LauncherSettings current = state.settings();
        applySettings(new LauncherSettings(
                current.columns(),
                current.cardScalePercent(),
                current.iconScalePercent(),
                current.topBlankRows(),
                current.spacingScalePercent() + delta));
    }

    private void changeTopRows(int delta) {
        LauncherSettings current = state.settings();
        applySettings(new LauncherSettings(
                current.columns(),
                current.cardScalePercent(),
                current.iconScalePercent(),
                current.topBlankRows() + delta,
                current.spacingScalePercent()));
    }

    private void applySettings(LauncherSettings settings) {
        state = new LauncherState(state.componentIds(), settings, state.wallpaperUri());
        preferences.save(state);
        configureGrid();
        adapter.setActivePosition(activePosition);
        rebuildGrid();
        updateSettingsValues();
    }

    private void updateSettingsValues() {
        LauncherSettings settings = state.settings();
        columnsValue.setText(String.valueOf(settings.columns()));
        cardValue.setText(getString(R.string.percent_value, settings.cardScalePercent()));
        spacingValue.setText(getString(R.string.percent_value, settings.spacingScalePercent()));
        topRowsValue.setText(getString(R.string.rows_value, settings.topBlankRows()));
    }

    private void loadSettingsApps() {
        settingsAppLoading.setVisibility(View.VISIBLE);
        repositoryExecutor.execute(() -> {
            List<AppEntry> discovered = appRepository.discoverLaunchableApps();
            mainHandler.post(() -> {
                if (!isSettingsOpen() || isFinishing()) {
                    return;
                }
                discoveredApps = discovered;
                List<String> labels = new ArrayList<>(discovered.size());
                for (AppEntry entry : discovered) {
                    labels.add(entry.label());
                }
                settingsAppList.setAdapter(new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_multiple_choice, labels));
                for (int index = 0; index < discovered.size(); index++) {
                    settingsAppList.setItemChecked(
                            index, state.componentIds().contains(discovered.get(index).componentId()));
                }
                settingsAppLoading.setVisibility(View.GONE);
            });
        });
    }

    private void toggleSettingsApp(int position) {
        if (position < 0 || position >= discoveredApps.size()) {
            return;
        }
        List<String> ids = new ArrayList<>(state.componentIds());
        String componentId = discoveredApps.get(position).componentId();
        if (ids.remove(componentId)) {
            settingsAppList.setItemChecked(position, false);
        } else {
            ids.add(componentId);
            settingsAppList.setItemChecked(position, true);
        }
        state = new LauncherState(ids, state.settings(), state.wallpaperUri());
        preferences.save(state);
        refreshDesktop();
    }

    private void chooseWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_WALLPAPER);
        } catch (ActivityNotFoundException failure) {
            Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
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
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            state = new LauncherState(state.componentIds(), state.settings(), uri.toString());
            preferences.save(state);
            loadWallpaper();
            Toast.makeText(this, R.string.wallpaper_saved, Toast.LENGTH_SHORT).show();
        } catch (SecurityException failure) {
            Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean animateSwap(int from, int to, View fromView, View toView) {
        if (!(fromView instanceof AppCardView) || !(toView instanceof AppCardView)) {
            return false;
        }
        AppCardView fromCard = (AppCardView) fromView;
        AppCardView toCard = (AppCardView) toView;
        float deltaX = toCard.getLeft() - fromCard.getLeft();
        float deltaY = toCard.getTop() - fromCard.getTop();
        final int generation = ++moveAnimationGeneration;
        moveAnimationRunning = true;
        fromCard.animate().cancel();
        toCard.animate().cancel();
        fromCard.setTranslationX(0f);
        fromCard.setTranslationY(0f);
        toCard.setTranslationX(0f);
        toCard.setTranslationY(0f);
        fromCard.animate()
                .translationX(deltaX)
                .translationY(deltaY)
                .setDuration(REORDER_ANIMATION_DURATION_MS)
                .withLayer()
                .start();
        toCard.animate()
                .translationX(-deltaX)
                .translationY(-deltaY)
                .setDuration(REORDER_ANIMATION_DURATION_MS)
                .withLayer()
                .withEndAction(() -> {
                    if (generation != moveAnimationGeneration) {
                        return;
                    }
                    moveAnimationRunning = false;
                    fromCard.setTranslationX(0f);
                    fromCard.setTranslationY(0f);
                    toCard.setTranslationX(0f);
                    toCard.setTranslationY(0f);
                    adapter.bindView(from, fromCard);
                    adapter.bindView(to, toCard);
                    activatePosition(to, false);
                })
                .start();
        return true;
    }

    private View visibleCardAt(int position) {
        return position >= 0 && position < gridView.getChildCount()
                ? gridView.getChildAt(position)
                : null;
    }

    private void focusPosition(int position) {
        activatePosition(position, false);
    }

    private void activatePosition(int position, boolean animate) {
        if (position < 0 || position >= entries.size()) {
            return;
        }
        View previous = visibleCardAt(activePosition);
        if (previous instanceof AppCardView && activePosition != position) {
            ((AppCardView) previous).setActive(false, animate);
        }
        activePosition = position;
        adapter.setActivePosition(position);
        gridView.setActivePosition(position);
        if (!isSettingsOpen()) {
            gridView.requestFocus();
        }
        View visible = visibleCardAt(position);
        if (visible instanceof AppCardView) {
            ((AppCardView) visible).setActive(true, animate);
            return;
        }
        gridView.post(() -> {
            View child = visibleCardAt(position);
            if (child instanceof AppCardView && position == activePosition) {
                ((AppCardView) child).setActive(true, animate);
                gridView.ensurePositionVisible(position);
            }
        });
        gridView.ensurePositionVisible(position);
    }

    private void rebuildGrid() {
        gridView.removeAllViews();
        for (int position = 0; position < entries.size(); position++) {
            gridView.addView(adapter.getView(position, null, gridView));
        }
        gridView.setActivePosition(activePosition);
        gridView.requestLayout();
    }

    private String focusedComponentId() {
        int position = activePosition;
        if (position >= 0 && position < entries.size()) {
            return entries.get(position).componentId();
        }
        return "";
    }

    private void discardMoveSession() {
        stopMoveAnimation();
        reorderSession = null;
    }

    private void stopMoveAnimation() {
        moveAnimationRunning = false;
        moveAnimationGeneration++;
        if (gridView == null) {
            return;
        }
        for (int index = 0; index < gridView.getChildCount(); index++) {
            View child = gridView.getChildAt(index);
            child.animate().cancel();
            child.setTranslationX(0f);
            child.setTranslationY(0f);
        }
    }

    private void loadWallpaper() {
        final int generation = ++wallpaperLoadGeneration;
        String uriText = state.wallpaperUri();
        final Uri uri = uriText.isEmpty() ? null : Uri.parse(uriText);
        repositoryExecutor.execute(() -> {
            Bitmap candidate = uri == null ? null : decodeWallpaper(uri);
            if (candidate == null) {
                candidate = decodeDefaultWallpaper();
            }
            final Bitmap decoded = softenWallpaper(candidate);
            mainHandler.post(() -> {
                if (generation != wallpaperLoadGeneration || isFinishing()) {
                    if (decoded != null) {
                        decoded.recycle();
                    }
                    return;
                }
                if (decoded == null) {
                    wallpaperView.setImageDrawable(null);
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
            if (input == null) {
                return null;
            }
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
        int sample = 1;
        while (bounds.outWidth / sample > 320 || bounds.outHeight / sample > 320) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                return null;
            }
            return BitmapFactory.decodeStream(input, null, options);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    private Bitmap decodeDefaultWallpaper() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(getResources(), R.drawable.default_wallpaper, options);
    }

    private static Bitmap softenWallpaper(Bitmap source) {
        if (source == null) {
            return null;
        }
        int longest = Math.max(source.getWidth(), source.getHeight());
        if (longest <= 160) {
            return source;
        }
        float scale = 160f / longest;
        Bitmap softened = Bitmap.createScaledBitmap(
                source,
                Math.max(1, Math.round(source.getWidth() * scale)),
                Math.max(1, Math.round(source.getHeight() * scale)),
                true);
        if (softened != source) {
            source.recycle();
        }
        return softened;
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
