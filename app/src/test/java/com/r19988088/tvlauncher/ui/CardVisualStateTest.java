package com.r19988088.tvlauncher.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CardVisualStateTest {
    @Test
    public void focusedCardIsRaisedAndNamed() {
        CardVisualState state = CardVisualState.focused();

        assertEquals(1.12f, state.scale(), 0.001f);
        assertEquals(-6f, state.translationYDp(), 0.001f);
        assertEquals(24f, state.elevationDp(), 0.001f);
        assertEquals(1f, state.labelAlpha(), 0.001f);
    }

    @Test
    public void unfocusedCardHasTinyShadowAndNoName() {
        CardVisualState state = CardVisualState.unfocused();

        assertEquals(1f, state.scale(), 0.001f);
        assertEquals(0f, state.translationYDp(), 0.001f);
        assertEquals(1f, state.elevationDp(), 0.001f);
        assertEquals(0f, state.labelAlpha(), 0.001f);
    }
}
