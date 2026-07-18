package com.r19988088.tvlauncher.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WallpaperThumbnailLoader {
    public interface Callback {
        void onLoaded(String key, Bitmap bitmap);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(32),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(4 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getAllocationByteCount() / 1024);
        }
    };

    public String key(File file) {
        return file.getAbsolutePath() + ':' + file.lastModified() + ':' + file.length();
    }

    public void load(File file, int targetWidth, int targetHeight, Callback callback) {
        String key = key(file);
        Bitmap cached = cache.get(key);
        if (cached != null && !cached.isRecycled()) {
            callback.onLoaded(key, cached);
            return;
        }
        executor.execute(() -> {
            Bitmap bitmap = decode(file, targetWidth, targetHeight);
            if (bitmap == null) return;
            cache.put(key, bitmap);
            mainHandler.post(() -> callback.onLoaded(key, bitmap));
        });
    }

    public void close() {
        executor.shutdownNow();
        cache.evictAll();
    }

    private static Bitmap decode(File file, int targetWidth, int targetHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = WallpaperDecodeSize.sampleSize(
                bounds.outWidth, bounds.outHeight, targetWidth, targetHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
