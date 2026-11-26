package com.example.trojanprototype.core;

public class TouchInput {
    public static final int BTN_LEFT = 0, BTN_RIGHT = 1, BTN_ATTACK = 2, BTN_GUARD = 3,
            BTN_DODGE = 4, BTN_ULT = 5, BTN_PAUSE = 6, BTN_AI = 7, BTN_COUNT = 8;
    public final boolean[] held = new boolean[BTN_COUNT];
    public final boolean[] pressed = new boolean[BTN_COUNT];

    public boolean consumePress(int b) {
        boolean p = pressed[b];
        pressed[b] = false;
        return p;
    }

    public boolean isHeld(int b) { return held[b]; }

    public void clear() {
        for (int i = 0; i < BTN_COUNT; i++) { held[i] = false; pressed[i] = false; }
    }
}
