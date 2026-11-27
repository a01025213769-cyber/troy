package com.example.trojanprototype.core;

import android.view.MotionEvent;

import com.example.trojanprototype.ui.Theme;
import com.example.trojanprototype.util.Viewport;

/**
 * 터치 입력 처리 (멀티터치 지원).
 *
 * 담당: 유장원 (시스템)
 *
 * 왜 멀티터치가 필요한가:
 *   왼손으로 이동 버튼을 누른 채 오른손으로 공격을 눌러야 하므로
 *   손가락 하나만 처리하면 "걸으면서 공격"이 안 됩니다.
 *
 * 처리 방식:
 *   1) 손가락(pointerId) 하나하나가 어떤 버튼 위에서 눌렸는지 표에 기록합니다.
 *   2) 손가락이 떨어지면 표에서 지웁니다.
 *   3) 표를 보고 "지금 눌려 있는 버튼 목록"을 다시 계산합니다.
 *   이렇게 하면 손가락이 몇 개든 버튼 상태가 꼬이지 않습니다.
 */
public class TouchInput {

    public static final int BTN_LEFT = 0;
    public static final int BTN_RIGHT = 1;
    public static final int BTN_ATTACK = 2;
    public static final int BTN_GUARD = 3;
    public static final int BTN_DODGE = 4;
    public static final int BTN_ULT = 5;
    public static final int BTN_PAUSE = 6;
    public static final int BTN_AI = 7;
    public static final int BTN_COUNT = 8;

    private static final int MAX_POINTERS = 16;

    /** pointerId -> 버튼 번호 (-1 이면 버튼이 아닌 빈 화면) */
    private final int[] pointerButton = new int[MAX_POINTERS];
    /** pointerId 가 현재 화면에 닿아 있는지 */
    private final boolean[] pointerDown = new boolean[MAX_POINTERS];

    private final boolean[] held = new boolean[BTN_COUNT];
    private final boolean[] pressed = new boolean[BTN_COUNT];
    private final float[] heldTime = new float[BTN_COUNT];

    /** 버튼이 아닌 곳을 탭했을 때 (메뉴·컷신 진행용) */
    private boolean tapped = false;
    private float tapX = 0f, tapY = 0f;

    /** true 일 때만 전투 버튼 판정을 합니다. 메뉴 화면에서는 false. */
    private boolean battleMode = false;

    public TouchInput() {
        for (int i = 0; i < MAX_POINTERS; i++) pointerButton[i] = -1;
    }

    public void setBattleMode(boolean on) {
        if (battleMode != on) {
            battleMode = on;
            clearAll();
        }
    }

    public void clearAll() {
        for (int i = 0; i < MAX_POINTERS; i++) {
            pointerButton[i] = -1;
            pointerDown[i] = false;
        }
        for (int i = 0; i < BTN_COUNT; i++) {
            held[i] = false;
            pressed[i] = false;
            heldTime[i] = 0f;
        }
        tapped = false;
    }

    // ------------------------------------------------------------------
    //  MotionEvent 처리 — UI 스레드에서 호출되므로 synchronized
    // ------------------------------------------------------------------
    public synchronized void onTouchEvent(MotionEvent e, Viewport vp) {
        int action = e.getActionMasked();

        if (action == MotionEvent.ACTION_CANCEL) {
            clearAll();
            return;
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int idx = e.getActionIndex();
            int id = e.getPointerId(idx);
            if (id < 0 || id >= MAX_POINTERS) return;
            float vx = vp.toVirtualX(e.getX(idx));
            float vy = vp.toVirtualY(e.getY(idx));
            pointerDown[id] = true;
            pointerButton[id] = battleMode ? hitTest(vx, vy) : -1;
            rebuildHeld();

        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int idx = e.getActionIndex();
            int id = e.getPointerId(idx);
            if (id < 0 || id >= MAX_POINTERS) return;
            float vx = vp.toVirtualX(e.getX(idx));
            float vy = vp.toVirtualY(e.getY(idx));
            if (pointerButton[id] < 0) {
                // 버튼이 아닌 곳에서 손을 뗐다 = 화면 탭
                tapped = true;
                tapX = vx;
                tapY = vy;
            }
            pointerDown[id] = false;
            pointerButton[id] = -1;
            rebuildHeld();
        }
        // ACTION_MOVE 는 무시합니다. 누른 버튼은 뗄 때까지 유지됩니다.
    }

    /** 손가락 표를 보고 버튼 눌림 상태를 다시 계산합니다. */
    private void rebuildHeld() {
        boolean[] now = new boolean[BTN_COUNT];
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (pointerDown[i]) {
                int b = pointerButton[i];
                if (b >= 0 && b < BTN_COUNT) now[b] = true;
            }
        }
        for (int b = 0; b < BTN_COUNT; b++) {
            if (now[b] && !held[b]) {
                pressed[b] = true;   // 이번에 새로 눌림
                heldTime[b] = 0f;
            }
            held[b] = now[b];
        }
    }

    /** 가상 좌표가 어떤 버튼 위인지 찾습니다. 버튼이 아니면 -1. */
    private int hitTest(float x, float y) {
        if (inCircle(x, y, Theme.PAD_LEFT_X, Theme.PAD_Y, Theme.PAD_R)) return BTN_LEFT;
        if (inCircle(x, y, Theme.PAD_RIGHT_X, Theme.PAD_Y, Theme.PAD_R)) return BTN_RIGHT;
        if (inCircle(x, y, Theme.BTN_ATTACK_X, Theme.BTN_ATTACK_Y, Theme.BTN_ATTACK_R)) return BTN_ATTACK;
        if (inCircle(x, y, Theme.BTN_GUARD_X, Theme.BTN_GUARD_Y, Theme.BTN_GUARD_R)) return BTN_GUARD;
        if (inCircle(x, y, Theme.BTN_DODGE_X, Theme.BTN_DODGE_Y, Theme.BTN_DODGE_R)) return BTN_DODGE;
        if (inCircle(x, y, Theme.BTN_ULT_X, Theme.BTN_ULT_Y, Theme.BTN_ULT_R)) return BTN_ULT;
        if (inCircle(x, y, Theme.BTN_PAUSE_X, Theme.BTN_PAUSE_Y, Theme.BTN_SMALL_R)) return BTN_PAUSE;
        if (inCircle(x, y, Theme.BTN_AI_X, Theme.BTN_AI_Y, Theme.BTN_SMALL_R)) return BTN_AI;
        return -1;
    }

    private static boolean inCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        // 손가락은 굵으므로 실제 그림보다 조금 넉넉하게 잡습니다.
        float rr = r + 10f;
        return dx * dx + dy * dy <= rr * rr;
    }

    // ------------------------------------------------------------------
    //  게임 스레드에서 읽는 부분
    // ------------------------------------------------------------------
    public synchronized void update(float dt) {
        for (int b = 0; b < BTN_COUNT; b++) {
            if (held[b]) heldTime[b] += dt;
        }
    }

    /** 지금 눌려 있는지 (이동·방어처럼 누르고 있는 버튼용) */
    public synchronized boolean isHeld(int button) {
        return held[button];
    }

    /** 방금 눌렸는지 — 한 번 읽으면 사라집니다 (공격·회피처럼 한 번만 반응할 버튼용) */
    public synchronized boolean consumePress(int button) {
        if (pressed[button]) {
            pressed[button] = false;
            return true;
        }
        return false;
    }

    public synchronized float heldTime(int button) {
        return heldTime[button];
    }

    /** 빈 화면을 탭했는지 — 한 번 읽으면 사라집니다 */
    public synchronized boolean consumeTap() {
        if (tapped) {
            tapped = false;
            return true;
        }
        return false;
    }

    public synchronized float tapX() {
        return tapX;
    }

    public synchronized float tapY() {
        return tapY;
    }

    /** 아직 읽지 않은 탭이 특정 사각형 안이었는지 확인하고 소비합니다. */
    public synchronized boolean consumeTapIn(float l, float t, float r, float b) {
        if (tapped && tapX >= l && tapX <= r && tapY >= t && tapY <= b) {
            tapped = false;
            return true;
        }
        return false;
    }
}
