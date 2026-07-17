package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.r19988088.tvlauncher.image.BannerLoader;
import com.r19988088.tvlauncher.model.AppEntry;
import java.util.ArrayList;
import java.util.List;

public final class AppGridAdapter extends BaseAdapter {
    public interface Listener {
        void onLaunch(AppEntry entry);
        boolean onLongPress(AppEntry entry);
    }

    private final Context context;
    private final BannerLoader bannerLoader;
    private final Listener listener;
    private final List<AppEntry> entries = new ArrayList<>();
    private int cardWidth;
    private int cardHeight;
    private int iconScalePercent = 60;

    public AppGridAdapter(Context context, BannerLoader bannerLoader, Listener listener) {
        this.context = context;
        this.bannerLoader = bannerLoader;
        this.listener = listener;
    }

    public void setCardMetrics(int cardWidth, int cardHeight, int iconScalePercent) {
        if (this.cardWidth == cardWidth
                && this.cardHeight == cardHeight
                && this.iconScalePercent == iconScalePercent) {
            return;
        }
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.iconScalePercent = iconScalePercent;
        notifyDataSetChanged();
    }

    public void replace(List<AppEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    public List<AppEntry> snapshot() {
        return new ArrayList<>(entries);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public AppEntry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).componentId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View recycled, ViewGroup parent) {
        final AppCardView card = recycled instanceof AppCardView
                ? (AppCardView) recycled
                : new AppCardView(context);
        final AppEntry entry = getItem(position);
        card.configure(cardWidth, cardHeight);
        card.bind(entry.componentId(), entry.label());
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onLaunch(entry);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return listener.onLongPress(entry);
            }
        });
        bannerLoader.load(entry, cardWidth, cardHeight, iconScalePercent,
                new BannerLoader.Callback() {
                    @Override
                    public void onLoaded(String componentId, android.graphics.Bitmap bitmap) {
                        card.setImageIfBound(componentId, bitmap);
                    }
                });
        return card;
    }
}
