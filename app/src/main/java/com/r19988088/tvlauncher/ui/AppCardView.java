package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public final class AppCardView extends FrameLayout {
    private static final long ANIMATION_DURATION_MS = 220L;
    private static final PathInterpolator FOCUS_INTERPOLATOR =
            new PathInterpolator(0.20f, 0f, 0f, 1f);
    private static final OvershootInterpolator ACTIVE_INTERPOLATOR =
            new OvershootInterpolator(0.85f);

    private final ImageView imageView;
    private final GradientDrawable placeholder;
    private final TextView labelView;
    private final float density;
    private String boundComponentId = "";
    private String boundRequestKey = "";

    public AppCardView(Context context) {
        this(context, null);
    }

    public AppCardView(Context context, AttributeSet attributes) {
        super(context, attributes);
        density = getResources().getDisplayMetrics().density;
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(false);
        setClickable(true);
        setLongClickable(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        setBackgroundColor(Color.TRANSPARENT);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        placeholder = new GradientDrawable();
        placeholder.setColor(0x992b3540);
        imageView.setBackground(placeholder);
        imageView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        view.getHeight() * 0.12f);
            }
        });
        addView(imageView);

        labelView = new TextView(context);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(18f);
        labelView.setSingleLine(true);
        labelView.setAlpha(0f);
        labelView.setShadowLayer(dp(5), 0f, dp(2), 0xcc000000);
        addView(labelView);

        applyVisualState(CardVisualState.unfocused(), false);
    }

    public void configure(int columnWidth, int cardWidth, int cardHeight) {
        int topMargin = dp(8);
        int labelHeight = dp(22);
        int labelGap = dp(4);
        int totalHeight = topMargin + cardHeight + labelGap + labelHeight + dp(8);
        setLayoutParams(new AbsListView.LayoutParams(
                columnWidth, totalHeight));

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(cardWidth, cardHeight);
        imageParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        imageParams.topMargin = topMargin;
        imageView.setLayoutParams(imageParams);
        placeholder.setCornerRadius(cardHeight * 0.12f);

        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                cardWidth + dp(24), labelHeight);
        labelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        labelParams.topMargin = topMargin + cardHeight + labelGap + dp(8);
        labelView.setLayoutParams(labelParams);
        imageView.post(new Runnable() {
            @Override
            public void run() {
                imageView.invalidateOutline();
            }
        });
    }

    public void bind(String componentId, String requestKey, String label) {
        boundComponentId = componentId;
        boundRequestKey = requestKey;
        labelView.setText(label);
        setContentDescription(label);
        imageView.setImageDrawable(null);
        applyVisualState(CardVisualState.unfocused(), false);
    }

    public void setActive(boolean active, boolean animate) {
        CardVisualState state = active ? CardVisualState.focused() : CardVisualState.unfocused();
        applyVisualState(state, animate, active);
    }

    public void setImageIfBound(String requestKey, String componentId, Bitmap bitmap) {
        if (boundComponentId.equals(componentId)
                && boundRequestKey.equals(requestKey)
                && bitmap != null
                && !bitmap.isRecycled()) {
            imageView.setImageBitmap(bitmap);
        }
    }

    private void applyVisualState(CardVisualState state, boolean animate) {
        applyVisualState(state, animate, false);
    }

    private void applyVisualState(CardVisualState state, boolean animate, boolean active) {
        float translationY = dp(state.translationYDp());
        float elevation = state.elevationDp() * density;
        imageView.animate().cancel();
        labelView.animate().cancel();
        if (!animate) {
            imageView.setScaleX(state.scale());
            imageView.setScaleY(state.scale());
            imageView.setTranslationY(translationY);
            imageView.setElevation(density);
            imageView.setTranslationZ(Math.max(0f, elevation - density));
            labelView.setAlpha(state.labelAlpha());
            labelView.setTranslationY(state.labelAlpha() > 0f ? 0f : dp(6));
            return;
        }
        imageView.animate()
                .scaleX(state.scale())
                .scaleY(state.scale())
                .translationY(translationY)
                .translationZ(Math.max(0f, elevation - density))
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(active ? ACTIVE_INTERPOLATOR : FOCUS_INTERPOLATOR)
                .withLayer()
                .start();
        imageView.setElevation(density);
        if (active && labelView.getAlpha() == 0f) {
            labelView.setTranslationY(dp(6));
        }
        labelView.animate()
                .alpha(state.labelAlpha())
                .translationY(active ? 0f : -dp(4))
                .setStartDelay(active ? 45L : 0L)
                .setDuration(170L)
                .setInterpolator(FOCUS_INTERPOLATOR)
                .withLayer()
                .start();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
