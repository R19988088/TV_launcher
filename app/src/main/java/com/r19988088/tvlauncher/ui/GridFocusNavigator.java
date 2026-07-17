package com.r19988088.tvlauncher.ui;

public final class GridFocusNavigator {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    private GridFocusNavigator() {}

    public static int move(int current, int itemCount, int columns, int direction) {
        if (itemCount <= 0 || columns <= 0) {
            return -1;
        }
        int position = Math.max(0, Math.min(current, itemCount - 1));
        int target = position;
        if (direction == LEFT && position % columns > 0) {
            target--;
        } else if (direction == RIGHT
                && position % columns < columns - 1
                && position + 1 < itemCount) {
            target++;
        } else if (direction == UP && position >= columns) {
            target -= columns;
        } else if (direction == DOWN && position + columns < itemCount) {
            target += columns;
        }
        return target;
    }
}
