package com.r19988088.tvlauncher.image;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.BitmapShader;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import com.r19988088.tvlauncher.model.AppEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class BannerLoader {
    public interface Callback {
        void onLoaded(String requestKey, String componentId, Bitmap bitmap);
    }

    private static final int MEMORY_CACHE_KB = 12 * 1024;
    private static final long DISK_CACHE_LIMIT_BYTES = 64L * 1024L * 1024L;

    private final PackageManager packageManager;
    private final File diskDirectory;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor executor;
    private volatile boolean closed;
    private final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(
            MEMORY_CACHE_KB) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getAllocationByteCount() / 1024);
        }
    };

    public BannerLoader(Context context) {
        packageManager = context.getPackageManager();
        diskDirectory = new File(context.getCacheDir(), "banners");
        executor = new ThreadPoolExecutor(
                2,
                2,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(64),
                new ThreadFactory() {
                    private int sequence;

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "banner-loader-" + sequence++);
                        thread.setPriority(Thread.NORM_PRIORITY - 1);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    public void load(
            final AppEntry entry,
            final int width,
            final int height,
            final int iconScalePercent,
            final Callback callback) {
        if (!hasRenderableSize(width, height) || closed) {
            return;
        }
        final BannerCacheKey key = new BannerCacheKey(
                entry.componentId(),
                entry.lastUpdateTime(),
                width,
                height,
                iconScalePercent);
        final String cacheName = key.fileName();
        Bitmap cached = memoryCache.get(cacheName);
        if (cached != null && !cached.isRecycled()) {
            callback.onLoaded(cacheName, entry.componentId(), cached);
            return;
        }
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = readDisk(cacheName);
                    if (bitmap == null) {
                        bitmap = render(entry, width, height, iconScalePercent);
                        if (bitmap != null) {
                            writeDisk(cacheName, bitmap);
                        }
                    }
                    if (bitmap == null || closed) {
                        return;
                    }
                    memoryCache.put(cacheName, bitmap);
                    final Bitmap loaded = bitmap;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!closed) {
                                callback.onLoaded(cacheName, entry.componentId(), loaded);
                            }
                        }
                    });
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Activity shutdown won the race after the closed check.
        }
    }

    public void trimMemory(int level) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            memoryCache.trimToSize(MEMORY_CACHE_KB / 2);
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            memoryCache.evictAll();
        }
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        memoryCache.evictAll();
    }

    static boolean hasRenderableSize(int width, int height) {
        return width > 0 && height > 0;
    }

    private Bitmap render(AppEntry entry, int width, int height, int iconScalePercent) {
        Drawable banner = loadBanner(entry.componentName());
        Drawable drawable = banner != null ? banner : loadIcon(entry.componentName());
        if (drawable == null) {
            return null;
        }
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(source);
        if (banner == null) {
            float hue = Math.abs(entry.componentId().hashCode() % 360);
            canvas.drawColor(Color.HSVToColor(new float[] {hue, 0.42f, 0.36f}));
            int iconSize = Math.max(1, height * iconScalePercent / 100);
            int left = (width - iconSize) / 2;
            int top = (height - iconSize) / 2;
            drawable.setBounds(left, top, left + iconSize, top + iconSize);
        } else {
            drawable.setBounds(0, 0, width, height);
        }
        drawable.draw(canvas);
        Bitmap rounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = height * 0.12f;
        new Canvas(rounded).drawRoundRect(
                new RectF(0.5f, 0.5f, width - 0.5f, height - 0.5f),
                radius,
                radius,
                paint);
        source.recycle();
        return rounded;
    }

    private Drawable loadBanner(ComponentName componentName) {
        try {
            Drawable activityBanner = packageManager.getActivityBanner(componentName);
            if (activityBanner != null) {
                return activityBanner;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
        try {
            return packageManager.getApplicationBanner(componentName.getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    private Drawable loadIcon(ComponentName componentName) {
        try {
            return packageManager.getActivityIcon(componentName);
        } catch (PackageManager.NameNotFoundException ignored) {
            try {
                return packageManager.getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException missing) {
                return null;
            }
        }
    }

    private Bitmap readDisk(String cacheName) {
        File file = new File(diskDirectory, cacheName + ".webp");
        return file.isFile() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
    }

    private void writeDisk(String cacheName, Bitmap bitmap) {
        if (!diskDirectory.exists() && !diskDirectory.mkdirs()) {
            return;
        }
        File target = new File(diskDirectory, cacheName + ".webp");
        File temporary = new File(diskDirectory, cacheName + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            if (bitmap.compress(Bitmap.CompressFormat.WEBP, 90, output)) {
                if (!temporary.renameTo(target)) {
                    temporary.delete();
                }
                trimDiskCache();
            }
        } catch (IOException ignored) {
            temporary.delete();
        }
    }

    private void trimDiskCache() {
        File[] files = diskDirectory.listFiles();
        if (files == null) {
            return;
        }
        long total = 0L;
        for (File file : files) {
            total += file.length();
        }
        if (total <= DISK_CACHE_LIMIT_BYTES) {
            return;
        }
        java.util.Arrays.sort(files, new java.util.Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                return Long.compare(left.lastModified(), right.lastModified());
            }
        });
        for (File file : files) {
            if (total <= DISK_CACHE_LIMIT_BYTES) {
                break;
            }
            long length = file.length();
            if (file.delete()) {
                total -= length;
            }
        }
    }
}
