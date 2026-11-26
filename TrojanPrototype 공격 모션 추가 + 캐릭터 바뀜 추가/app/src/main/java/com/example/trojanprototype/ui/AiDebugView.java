package com.example.trojanprototype.ui;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.example.trojanprototype.ai.EnemyBrain;
import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.util.Draw;

/**
 * 적 AI 의 판단 과정을 화면에 그대로 보여 주는 패널. (발표 시연용)
 *
 * 담당: 유장원 (AI)
 *
 * 전투 화면의 [AI] 버튼을 누르면 켜집니다.
 * - 7가지 행동의 점수를 막대로 비교해서 보여 주고
 * - 지금 고른 행동과 그 이유를 한 줄로 설명하고
 * - 공격/몸통 판정 상자를 눈에 보이게 그립니다.
 *
 * "왜 이 적이 지금 막았는가?" 를 숫자로 설명할 수 있어서
 * 수업 시연에서 AI 동작을 그대로 보여 주기 좋습니다.
 */
public final class AiDebugView {

    private AiDebugView() {}

    private static final float PANEL_T = 138f;
    private static final float PANEL_B = 258f;
    private static final float BAR_BASE = 242f;
    private static final float BAR_MAX_H = 52f;
    private static final float BAR_W = 46f;
    private static final float BAR_GAP = 14f;
    private static final float BAR_X0 = 46f;

    public static void draw(Canvas c, Draw d, BattleWorld w) {
        EnemyBrain brain = w.brain;

        // ---------- 패널 ----------
        d.round(c, 20f, PANEL_T, 940f, PANEL_B, 12f, 0xE60D1420);
        d.roundStroke(c, 20f, PANEL_T, 940f, PANEL_B, 12f, 1.5f, 0x667FE0B0);

        // ---------- 1행: 요약 ----------
        float y1 = PANEL_T + 22f;
        d.text(c, "AI 판단 과정", 38f, y1, 15f, 0xFF7FE0B0, Draw.LEFT, true);
        d.text(c, w.armyMode ? "대장 AI (병사도 각자 같은 방식)" : "FSM + 유틸리티 점수 방식", 130f, y1, 12f, Theme.TEXT_DIM, Draw.LEFT, false);

        String act = EnemyBrain.ACTION_NAMES[clampIndex(brain.chosen)];
        d.text(c, "선택: " + act, 330f, y1, 14f, Theme.TEXT_GOLD, Draw.LEFT, true);
        d.text(c, "거리 " + (int) brain.distance + "px", 450f, y1, 13f, Theme.TEXT, Draw.LEFT, false);
        d.text(c, (w.armyMode ? "적 대장 체력 " : "적 체력 ") + (int) (w.enemy.hpRatio() * 100f) + "%", 560f, y1, 13f,
                Theme.TEXT, Draw.LEFT, false);

        float nextIn = brain.decisionTimer < 0f ? 0f : brain.decisionTimer;
        d.text(c, "재판단까지 " + fmt(nextIn) + "초", 690f, y1, 13f, Theme.TEXT_DIM, Draw.LEFT, false);
        if (brain.enraged) {
            d.text(c, "보스 분노", 860f, y1, 13f, 0xFFFF9C6B, Draw.LEFT, true);
        }

        // ---------- 2행: 행동 점수 막대 ----------
        float maxScore = 0.001f;
        for (int i = 0; i < EnemyBrain.ACT_COUNT; i++) {
            if (brain.scores[i] > maxScore) maxScore = brain.scores[i];
        }

        for (int i = 0; i < EnemyBrain.ACT_COUNT; i++) {
            float x = BAR_X0 + i * (BAR_W + BAR_GAP);
            float ratio = Draw.clamp(brain.scores[i] / maxScore, 0f, 1f);
            float h = BAR_MAX_H * ratio;
            boolean picked = (i == brain.chosen);

            // 배경 칸
            d.round(c, x, BAR_BASE - BAR_MAX_H, x + BAR_W, BAR_BASE, 3f, 0x33334455);
            // 점수 막대
            d.round(c, x, BAR_BASE - h, x + BAR_W, BAR_BASE, 3f,
                    picked ? 0xFFF0C878 : 0x993E6E86);
            if (picked) {
                d.roundStroke(c, x - 2f, BAR_BASE - BAR_MAX_H - 2f, x + BAR_W + 2f, BAR_BASE + 2f,
                        4f, 1.5f, 0xFFFFE6A8);
            }
            d.text(c, EnemyBrain.ACTION_NAMES[i], x + BAR_W * 0.5f, BAR_BASE + 13f, 11f,
                    picked ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.CENTER, picked);
            d.text(c, fmt(brain.scores[i]), x + BAR_W * 0.5f, BAR_BASE - h - 5f, 10f,
                    picked ? 0xFFFFE6A8 : 0x99BCCAD6, Draw.CENTER, false);
        }

        // ---------- 오른쪽: 인식 상태와 판단 근거 ----------
        float tx = 496f;
        d.text(c, "인식", tx, BAR_BASE - 40f, 12f, Theme.TEXT_DIM, Draw.LEFT, true);
        flag(c, d, tx + 42f, BAR_BASE - 44f, "상대 공격", brain.sawIncoming);
        flag(c, d, tx + 152f, BAR_BASE - 44f, "상대 빈틈", brain.sawOpening);

        d.text(c, "판단 근거", tx, BAR_BASE - 12f, 12f, Theme.TEXT_DIM, Draw.LEFT, true);
        d.text(c, brain.reason, tx + 70f, BAR_BASE - 12f, 14f, 0xFF9FE8C8, Draw.LEFT, true);

        d.text(c, "플레이어 상태: " + stateName(w.player.state), tx, BAR_BASE + 13f, 11f,
                Theme.TEXT_DIM, Draw.LEFT, false);
        d.text(c, "적 상태: " + stateName(w.enemy.state), tx + 200f, BAR_BASE + 13f, 11f,
                Theme.TEXT_DIM, Draw.LEFT, false);
    }

    /** 전투 무대 위에 충돌 판정 상자를 그립니다. */
    public static void drawHitboxes(Canvas c, Draw d, BattleWorld w) {
        for (int i = 0; i < w.unitCount; i++) {
            Fighter u = w.units[i];
            if (!u.isAlive()) continue;
            box(c, d, u.bodyBox(), u.team == 0 ? 0x883FA0D8 : 0x88D86B5A);
            box(c, d, u.attackBox(), u.team == 0 ? 0xDDFFD36B : 0xDDFF7A5A);
        }
    }

    private static void box(Canvas c, Draw d, RectF r, int color) {
        if (r == null) return;
        d.roundStroke(c, r.left, r.top, r.right, r.bottom, 2f, 2f, color);
    }

    private static void flag(Canvas c, Draw d, float x, float y, String label, boolean on) {
        d.round(c, x, y - 11f, x + 96f, y + 7f, 9f, on ? 0xCC2E6B5A : 0x55202A36);
        d.text(c, label, x + 48f, y + 3f, 11f, on ? 0xFFDFFFEF : Theme.TEXT_DIM,
                Draw.CENTER, on);
    }

    private static String stateName(int s) {
        switch (s) {
            case Fighter.IDLE: return "대기";
            case Fighter.WALK: return "이동";
            case Fighter.ATTACK: return "공격";
            case Fighter.GUARD: return "방어";
            case Fighter.DODGE: return "회피";
            case Fighter.HURT: return "피격 경직";
            case Fighter.DOWN: return "쓰러짐";
            default: return "승리";
        }
    }

    private static int clampIndex(int i) {
        if (i < 0) return 0;
        if (i >= EnemyBrain.ACT_COUNT) return 0;
        return i;
    }

    private static String fmt(float v) {
        int hundredths = Math.round(v * 100f);
        int whole = hundredths / 100;
        int frac = Math.abs(hundredths % 100);
        return whole + "." + (frac < 10 ? "0" + frac : String.valueOf(frac));
    }
}
