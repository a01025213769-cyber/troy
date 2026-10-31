package com.example.trojanprototype.ui;

import android.graphics.Canvas;

import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.core.TouchInput;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.util.Draw;

/**
 * 화면 위 가상 조작 버튼 그리기.
 *
 * 담당: 심신 (UI 디자인) + 유장원 (상태 연결)
 *
 * 버튼의 "위치"는 Theme 에, "판정"은 TouchInput 에, "그림"은 여기에 있습니다.
 * 셋이 모두 Theme 의 같은 좌표를 보기 때문에 위치를 옮길 때는 Theme 만 고치면 됩니다.
 *
 *   ┌──────────────────────────────────────────────┐
 *   │  체력바            ⏱            체력바        │
 *   │                 ⏸  AI                        │
 *   │                                      ◇회피    │
 *   │            [ 전 투 무 대 ]           ◆필살기  │
 *   │                                               │
 *   │  ◀  ▶                        방어   ⚔ 공격   │
 *   └──────────────────────────────────────────────┘
 */
public final class VirtualPad {

    private VirtualPad() {}

    public static void draw(Canvas c, Draw d, TouchInput in, Fighter player, boolean aiDebugOn) {
        drawMoveButtons(c, d, in);
        drawActionButtons(c, d, in, player);
        drawSmallButtons(c, d, in, aiDebugOn);
    }

    // ------------------------------------------------------------------
    private static void drawMoveButtons(Canvas c, Draw d, TouchInput in) {
        boolean left = in.isHeld(TouchInput.BTN_LEFT);
        boolean right = in.isHeld(TouchInput.BTN_RIGHT);

        padCircle(c, d, Theme.PAD_LEFT_X, Theme.PAD_Y, Theme.PAD_R, left);
        d.triangle(c,
                Theme.PAD_LEFT_X + 13f, Theme.PAD_Y - 17f,
                Theme.PAD_LEFT_X + 13f, Theme.PAD_Y + 17f,
                Theme.PAD_LEFT_X - 15f, Theme.PAD_Y,
                left ? Theme.TEXT : 0xCCE4EAF0);

        padCircle(c, d, Theme.PAD_RIGHT_X, Theme.PAD_Y, Theme.PAD_R, right);
        d.triangle(c,
                Theme.PAD_RIGHT_X - 13f, Theme.PAD_Y - 17f,
                Theme.PAD_RIGHT_X - 13f, Theme.PAD_Y + 17f,
                Theme.PAD_RIGHT_X + 15f, Theme.PAD_Y,
                right ? Theme.TEXT : 0xCCE4EAF0);
    }

    // ------------------------------------------------------------------
    private static void drawActionButtons(Canvas c, Draw d, TouchInput in, Fighter player) {
        // --- 공격
        boolean atk = in.isHeld(TouchInput.BTN_ATTACK);
        d.circle(c, Theme.BTN_ATTACK_X, Theme.BTN_ATTACK_Y, Theme.BTN_ATTACK_R,
                atk ? Theme.BTN_ATTACK_ON : Theme.BTN_ATTACK);
        d.circleStroke(c, Theme.BTN_ATTACK_X, Theme.BTN_ATTACK_Y, Theme.BTN_ATTACK_R, 2.5f,
                atk ? 0xFFFFD9A0 : Theme.BTN_EDGE);
        d.text(c, "공격", Theme.BTN_ATTACK_X, Theme.BTN_ATTACK_Y + 3f, 23f, Theme.TEXT, Draw.CENTER, true);

        // 활과 검을 바꿔 쓰는 캐릭터(오디세우스)는 지금 어떤 무기가 나갈지 보여 줍니다.
        String sub = "연타 = 연속기";
        int subColor = 0xBBF0D7B4;
        if (player != null && player.meleeLightMove != null) {
            sub = player.usingMelee ? "검 · 근접" : "활 · 원거리";
            subColor = player.usingMelee ? 0xFFFFD9A0 : 0xFF9FD8F0;
        }
        d.text(c, sub, Theme.BTN_ATTACK_X, Theme.BTN_ATTACK_Y + 24f, 11f, subColor,
                Draw.CENTER, false);

        // --- 방어
        boolean grd = in.isHeld(TouchInput.BTN_GUARD);
        padCircle(c, d, Theme.BTN_GUARD_X, Theme.BTN_GUARD_Y, Theme.BTN_GUARD_R, grd);
        d.text(c, "방어", Theme.BTN_GUARD_X, Theme.BTN_GUARD_Y + 6f, 18f,
                grd ? 0xFFBFE4FF : Theme.TEXT, Draw.CENTER, true);

        // --- 회피 (쿨타임을 어둡게 덮어 표시)
        boolean dg = in.isHeld(TouchInput.BTN_DODGE);
        boolean dodgeReady = player == null || player.dodgeCooldown <= 0f;
        padCircle(c, d, Theme.BTN_DODGE_X, Theme.BTN_DODGE_Y, Theme.BTN_DODGE_R, dg && dodgeReady);
        if (!dodgeReady) {
            float ratio = Draw.clamp(player.dodgeCooldown / GameData.DODGE_COOLDOWN, 0f, 1f);
            d.circle(c, Theme.BTN_DODGE_X, Theme.BTN_DODGE_Y,
                    Theme.BTN_DODGE_R * ratio, 0x99121820);
        }
        d.text(c, "회피", Theme.BTN_DODGE_X, Theme.BTN_DODGE_Y + 6f, 18f,
                dodgeReady ? Theme.TEXT : Theme.TEXT_DIM, Draw.CENTER, true);

        // --- 필살기 (분노 게이지가 가득 찼을 때만)
        boolean ready = player != null && player.ultimateReady();
        float rageRatio = (player == null) ? 0f : player.rage / GameData.RAGE_MAX;
        int face = ready ? Theme.BTN_ULT_READY : Theme.BTN_DISABLED;
        d.circle(c, Theme.BTN_ULT_X, Theme.BTN_ULT_Y, Theme.BTN_ULT_R, face);
        // 충전 정도를 테두리로 표시
        d.circleStroke(c, Theme.BTN_ULT_X, Theme.BTN_ULT_Y, Theme.BTN_ULT_R - 1f, 3f,
                ready ? 0xFFFFE9A8 : 0x55E9BE7B);
        if (!ready) {
            d.text(c, (int) (rageRatio * 100f) + "%", Theme.BTN_ULT_X, Theme.BTN_ULT_Y + 20f,
                    12f, Theme.RAGE, Draw.CENTER, true);
        }
        d.text(c, "필살기", Theme.BTN_ULT_X, Theme.BTN_ULT_Y + 2f, 17f,
                ready ? 0xFF2A1E10 : Theme.TEXT_DIM, Draw.CENTER, true);
    }

    // ------------------------------------------------------------------
    private static void drawSmallButtons(Canvas c, Draw d, TouchInput in, boolean aiDebugOn) {
        // 일시정지
        d.circle(c, Theme.BTN_PAUSE_X, Theme.BTN_PAUSE_Y, Theme.BTN_SMALL_R, 0xAA1E2733);
        d.circleStroke(c, Theme.BTN_PAUSE_X, Theme.BTN_PAUSE_Y, Theme.BTN_SMALL_R, 1.6f, 0x66E9BE7B);
        d.rect(c, Theme.BTN_PAUSE_X - 6f, Theme.BTN_PAUSE_Y - 8f,
                Theme.BTN_PAUSE_X - 2f, Theme.BTN_PAUSE_Y + 8f, Theme.TEXT);
        d.rect(c, Theme.BTN_PAUSE_X + 2f, Theme.BTN_PAUSE_Y - 8f,
                Theme.BTN_PAUSE_X + 6f, Theme.BTN_PAUSE_Y + 8f, Theme.TEXT);

        // AI 디버그 표시 전환
        d.circle(c, Theme.BTN_AI_X, Theme.BTN_AI_Y, Theme.BTN_SMALL_R,
                aiDebugOn ? 0xCC2E6B5A : 0xAA1E2733);
        d.circleStroke(c, Theme.BTN_AI_X, Theme.BTN_AI_Y, Theme.BTN_SMALL_R, 1.6f,
                aiDebugOn ? 0xFF7FE0B0 : 0x66E9BE7B);
        d.text(c, "AI", Theme.BTN_AI_X, Theme.BTN_AI_Y + 5f, 15f,
                aiDebugOn ? 0xFFDFFFEF : Theme.TEXT_DIM, Draw.CENTER, true);
    }

    // ------------------------------------------------------------------
    private static void padCircle(Canvas c, Draw d, float x, float y, float r, boolean on) {
        d.circle(c, x, y, r, on ? Theme.BTN_FACE_ON : Theme.BTN_FACE);
        d.circleStroke(c, x, y, r, 2f, Theme.BTN_EDGE);
    }
}
