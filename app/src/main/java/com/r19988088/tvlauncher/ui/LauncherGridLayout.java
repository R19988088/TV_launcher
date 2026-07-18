package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public final class LauncherGridLayout extends ViewGroup {
    private int columns = 1;
    private int columnWidth = 1;
    private int horizontalSpacing;
    private int verticalSpacing;
    private int horizontalPadding;
    private int topPadding;
    private int contentHeight;
    private int activePosition = -1;

    public LauncherGridLayout(Context context, AttributeSet attributes) {
        super(context, attributes);
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(true);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setMetrics(GridMetrics metrics) {
        columns = metrics.displayColumns();
        columnWidth = metrics.columnWidth();
        horizontalSpacing = metrics.horizontalSpacing();
        verticalSpacing = metrics.verticalSpacing();
        horizontalPadding = metrics.horizontalPadding();
        topPadding = metrics.topPadding();
        requestLayout();
    }

    public void setActivePosition(int position) {
        activePosition = position;
        invalidate();
    }

    public void ensurePositionVisible(int position) {
        View child = position >= 0 && position < getChildCount() ? getChildAt(position) : null;
        if (child == null) {
            return;
        }
        int target = getScrollY();
        if (child.getTop() < target) {
            target = child.getTop();
        } else if (child.getBottom() > target + getHeight()) {
            target = child.getBottom() - getHeight();
        }
        scrollTo(0, Math.max(0, Math.min(target, Math.max(0, contentHeight - getHeight()))));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int itemHeight = 0;
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            int childHeight = child.getLayoutParams().height;
            child.measure(
                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
            itemHeight = Math.max(itemHeight, childHeight);
        }
        int rows = getChildCount() == 0 ? 0 : (getChildCount() + columns - 1) / columns;
        contentHeight = topPadding
                + rows * itemHeight
                + Math.max(0, rows - 1) * verticalSpacing
                + verticalSpacing;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            int row = index / columns;
            int column = index % columns;
            int childLeft = horizontalPadding + column * (columnWidth + horizontalSpacing);
            int childTop = topPadding + row * (child.getMeasuredHeight() + verticalSpacing);
            child.layout(
                    childLeft,
                    childTop,
                    childLeft + columnWidth,
                    childTop + child.getMeasuredHeight());
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        if (activePosition < 0 || activePosition >= childCount) {
            return drawingPosition;
        }
        if (drawingPosition == childCount - 1) {
            return activePosition;
        }
        return drawingPosition >= activePosition ? drawingPosition + 1 : drawingPosition;
    }
}
