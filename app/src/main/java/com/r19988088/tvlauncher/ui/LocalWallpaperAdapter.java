package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.r19988088.tvlauncher.wallpaper.WallpaperThumbnailLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class LocalWallpaperAdapter extends BaseAdapter {
    private final Context context;
    private final WallpaperThumbnailLoader loader;
    private final List<File> files = new ArrayList<>();
    private final int cellHeight;

    public LocalWallpaperAdapter(Context context, WallpaperThumbnailLoader loader) {
        this.context = context;
        this.loader = loader;
        cellHeight = Math.round(58f * context.getResources().getDisplayMetrics().density);
    }

    public void replace(List<File> wallpapers) {
        files.clear();
        files.addAll(wallpapers);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getAbsolutePath().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View recycled, ViewGroup parent) {
        ImageView image = recycled instanceof ImageView
                ? (ImageView) recycled
                : createImageView();
        File file = getItem(position);
        String key = loader.key(file);
        image.setTag(key);
        image.setImageDrawable(new ColorDrawable(0xff353b40));
        loader.load(file, cellHeight * 16 / 9, cellHeight, (loadedKey, bitmap) -> {
            if (loadedKey.equals(image.getTag())) image.setImageBitmap(bitmap);
        });
        return image;
    }

    private ImageView createImageView() {
        ImageView image = new ImageView(context);
        image.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, cellHeight));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.TRANSPARENT);
        image.setFocusable(false);
        return image;
    }
}
