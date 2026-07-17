package com.r19988088.tvlauncher.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public final class LauncherGridView extends GridView {
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
        int focusedIndex = focusedChildIndex();
        if (focusedIndex < 0) {
            return drawingPosition;
        }
        if (drawingPosition == childCount - 1) {
            return focusedIndex;
        }
        return drawingPosition >= focusedIndex ? drawingPosition + 1 : drawingPosition;
    }

    private int focusedChildIndex() {
        View focused = findFocus();
        if (focused == null) {
            return -1;
        }
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child == focused || isDescendant(child, focused)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isDescendant(View parent, View child) {
        android.view.ViewParent current = child.getParent();
        while (current instanceof View) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}

