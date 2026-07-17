package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public final class AppCardView extends FrameLayout {
    private static final long ANIMATION_DURATION_MS = 180L;
    private static final PathInterpolator FOCUS_INTERPOLATOR =
            new PathInterpolator(0.20f, 0f, 0f, 1f);

    private final ImageView imageView;
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
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        setBackgroundColor(Color.TRANSPARENT);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setClipToOutline(true);
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

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                applyVisualState(hasFocus ? CardVisualState.focused() : CardVisualState.unfocused(), true);
                if (getParent() instanceof View) {
                    ((View) getParent()).invalidate();
                }
            }
        });
        applyVisualState(CardVisualState.unfocused(), false);
    }

    public void configure(int cardWidth, int cardHeight) {
        int topMargin = dp(30);
        int labelHeight = dp(34);
        int totalHeight = topMargin + cardHeight + dp(14) + labelHeight + dp(22);
        setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, totalHeight));

        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(cardWidth, cardHeight);
        imageParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        imageParams.topMargin = topMargin;
        imageView.setLayoutParams(imageParams);

        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                cardWidth + dp(24), labelHeight);
        labelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        labelParams.topMargin = topMargin + cardHeight + dp(10);
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
        imageView.setImageDrawable(new ColorDrawable(0x992b3540));
        applyVisualState(isFocused() ? CardVisualState.focused() : CardVisualState.unfocused(), false);
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
        float translationY = dp(state.translationYDp());
        float elevation = dp(state.elevationDp());
        imageView.animate().cancel();
        labelView.animate().cancel();
        if (!animate) {
            imageView.setScaleX(state.scale());
            imageView.setScaleY(state.scale());
            imageView.setTranslationY(translationY);
            imageView.setElevation(dp(1));
            imageView.setTranslationZ(Math.max(0f, elevation - dp(1)));
            labelView.setAlpha(state.labelAlpha());
            return;
        }
        imageView.animate()
                .scaleX(state.scale())
                .scaleY(state.scale())
                .translationY(translationY)
                .translationZ(Math.max(0f, elevation - dp(1)))
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(FOCUS_INTERPOLATOR)
                .withLayer()
                .start();
        imageView.setElevation(dp(1));
        labelView.animate()
                .alpha(state.labelAlpha())
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(FOCUS_INTERPOLATOR)
                .start();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
