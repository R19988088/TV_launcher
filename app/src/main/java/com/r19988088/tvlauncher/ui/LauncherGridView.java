package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public final class LauncherGridView extends GridView {
    private int activePosition = -1;

    public LauncherGridView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawingOrderEnabled(true);
        setSelector(new ColorDrawable(Color.TRANSPARENT));
        setCacheColorHint(Color.TRANSPARENT);
        setDrawSelectorOnTop(false);
        setSmoothScrollbarEnabled(false);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        int focusedIndex = activeChildIndex();
        if (focusedIndex < 0) {
            return drawingPosition;
        }
        if (drawingPosition == childCount - 1) {
            return focusedIndex;
        }
        return drawingPosition >= focusedIndex ? drawingPosition + 1 : drawingPosition;
    }

    public void setActivePosition(int position) {
        activePosition = position;
        invalidate();
    }

    private int activeChildIndex() {
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (getPositionForView(child) == activePosition) {
                return index;
            }
        }
        return -1;
    }
}
