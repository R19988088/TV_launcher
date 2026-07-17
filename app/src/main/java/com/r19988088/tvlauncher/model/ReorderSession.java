package com.r19988088.tvlauncher.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReorderSession {
    private final List<String> original;
    private final List<String> current;
    private int selectedIndex;

    public ReorderSession(List<String> componentIds, int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= componentIds.size()) {
            throw new IndexOutOfBoundsException("Selected index is outside the grid");
        }
        this.original = immutableCopy(componentIds);
        this.current = new ArrayList<>(componentIds);
        this.selectedIndex = selectedIndex;
    }

    public void swapWith(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= current.size()) {
            throw new IndexOutOfBoundsException("Target index is outside the grid");
        }
        Collections.swap(current, selectedIndex, targetIndex);
        selectedIndex = targetIndex;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public List<String> current() {
        return immutableCopy(current);
    }

    public List<String> commit() {
        return current();
    }

    public List<String> cancel() {
        return original;
    }

    private static List<String> immutableCopy(List<String> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}

