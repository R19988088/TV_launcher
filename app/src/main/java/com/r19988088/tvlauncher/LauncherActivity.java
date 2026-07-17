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
import android.widget.GridView;
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
import com.r19988088.tvlauncher.ui.AppCardView;
import com.r19988088.tvlauncher.ui.AppGridAdapter;
import com.r19988088.tvlauncher.ui.GridMetrics;
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

@SuppressLint("GestureBackNavigation")
public final class LauncherActivity extends Activity implements AppGridAdapter.Listener {
    private static final long REORDER_ANIMATION_DURATION_MS = 160L;

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
    private boolean moveAnimationRunning;
    private int moveAnimationGeneration;
    private int loadGeneration;
    private int wallpaperLoadGeneration;
    private Bitmap customWallpaper;
    private boolean packageReceiverRegistered;
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            discardMoveSession();
            refreshDesktop();
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
        discardMoveSession();
        state = preferences.load();
        configureGrid();
        loadWallpaper();
        refreshDesktop();
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
        if (reorderSession != null && isMoveCommandKey(keyCode)) {
            return event.getAction() != KeyEvent.ACTION_UP || handleMoveKey(keyCode);
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
        wallpaperLoadGeneration++;
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
        String focusedComponentId = focusedComponentId();
        entries = new ArrayList<>(resolved);
        adapter.replace(entries);
        boolean empty = entries.isEmpty();
        emptyPrompt.setVisibility(empty ? View.VISIBLE : View.GONE);
        gridView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            int restoredIndex = indexOf(focusedComponentId);
            focusPosition(restoredIndex >= 0 ? restoredIndex : 0);
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
                state.settings().cardScalePercent(),
                getResources().getDisplayMetrics().density);
        gridView.setNumColumns(columns);
        gridView.setColumnWidth(metrics.columnWidth());
        gridView.setStretchMode(GridView.STRETCH_SPACING);
        gridView.setHorizontalSpacing(metrics.horizontalSpacing());
        gridView.setVerticalSpacing(metrics.verticalSpacing());
        gridView.setPadding(
                metrics.horizontalPadding(),
                metrics.topPadding(),
                metrics.horizontalPadding(),
                metrics.verticalSpacing());
        adapter.setCardMetrics(
                metrics.columnWidth(),
                metrics.cardWidth(),
                metrics.cardHeight(),
                state.settings().iconScalePercent());
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
        startActivity(new Intent().setClassName(
                getPackageName(), getPackageName() + ".SettingsActivity"));
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
                    gridView.setSelection(to);
                    toCard.requestFocus();
                    adapter.bindView(from, fromCard);
                    adapter.bindView(to, toCard);
                })
                .start();
        return true;
    }

    private View visibleCardAt(int position) {
        int childIndex = position - gridView.getFirstVisiblePosition();
        return childIndex >= 0 && childIndex < gridView.getChildCount()
                ? gridView.getChildAt(childIndex)
                : null;
    }

    private void focusPosition(int position) {
        gridView.setSelection(position);
        gridView.post(() -> {
            View child = visibleCardAt(position);
            if (child != null) {
                child.requestFocus();
            } else {
                gridView.requestFocus();
            }
        });
    }

    private String focusedComponentId() {
        int position = gridView == null ? -1 : gridView.getSelectedItemPosition();
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
        options.inSampleSize = 8;
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
