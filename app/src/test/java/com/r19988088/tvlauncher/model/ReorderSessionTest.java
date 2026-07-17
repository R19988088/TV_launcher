package com.r19988088.tvlauncher.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public final class ReorderSessionTest {
    @Test
    public void cancelRestoresOriginalOrder() {
        ReorderSession session = new ReorderSession(Arrays.asList("a/A", "b/B", "c/C"), 1);

        session.swapWith(2);

        assertEquals(Arrays.asList("a/A", "c/C", "b/B"), session.current());
        assertEquals(Arrays.asList("a/A", "b/B", "c/C"), session.cancel());
    }

    @Test
    public void commitKeepsMovedOrder() {
        ReorderSession session = new ReorderSession(Arrays.asList("a/A", "b/B"), 0);

        session.swapWith(1);

        assertEquals(Arrays.asList("b/B", "a/A"), session.commit());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void rejectsTargetOutsideGrid() {
        ReorderSession session = new ReorderSession(Arrays.asList("a/A", "b/B"), 0);
        session.swapWith(2);
    }
}

