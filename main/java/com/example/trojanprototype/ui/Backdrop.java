package com.example.trojanprototype.ui;

import android.graphics.Canvas;

import com.example.trojanprototype.util.Draw;

/**
 * 스테이지 배경 그리기 (Canvas 로 직접 그리는 방식).
 *
 * 담당: 심신 (배경 / 연출)
 *
 * 배경 이미지 파일 없이 도형으로 그리기 때문에 용량이 0이고,
 * 색만 바꾸면 분위기를 바로 바꿀 수 있습니다.
 * 나중에 배경 그림(png)을 준비하면 draw() 안에서 d.bitmap(...) 한 줄로 교체하면 됩니다.
 *
 * 배경 종류
 *   0 트로이 해안   1 트로이 성벽   2 성문 앞 야간
 *   3 신들의 산(프롤로그)          4 귀향의 바다
 */
public final class Backdrop {

    private Backdrop() {}

    public static final int BEACH = 0;
    public static final int WALL = 1;
    public static final int NIGHT_GATE = 2;
    public static final int OLYMPUS = 3;
    public static final int SEA = 4;

    /**
     * @param time     경과 시간(초) — 불꽃이나 파도의 흔들림에 사용
     * @param parallax -1 ~ +1 — 카메라가 좌우로 살짝 따라가는 느낌
     */
    public static void draw(Canvas c, Draw d, int type, float time, float parallax) {
        // 어떤 배경이든 먼저 화면 전체를 칠해 둡니다.
        // 이게 없으면 하늘과 바닥 사이에 아무것도 안 그려진 검은 띠가 생깁니다.
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, Theme.BG_DEEP);

        switch (type) {
            case WALL:
                drawWall(c, d, time, parallax);
                break;
            case NIGHT_GATE:
                drawNightGate(c, d, time, parallax);
                break;
            case OLYMPUS:
                drawOlympus(c, d, time, parallax);
                break;
            case SEA:
                drawSea(c, d, time, parallax);
                break;
            default:
                drawBeach(c, d, time, parallax);
                break;
        }
    }

    // ==================================================================
    //  공통 도우미
    // ==================================================================
    /** 위아래 색이 서서히 변하는 띠를 그립니다 (그라데이션 대용, 가볍습니다) */
    private static void band(Canvas c, Draw d, float top, float bottom,
                             int colorTop, int colorBottom, int steps) {
        float h = (bottom - top) / steps;
        for (int i = 0; i < steps; i++) {
            float t = (steps == 1) ? 0f : i / (float) (steps - 1);
            d.rect(c, 0f, top + i * h, Theme.VW, top + (i + 1) * h + 1f,
                    Draw.mix(colorTop, colorBottom, t));
        }
    }

    /** 바닥면 + 그림자 */
    private static void ground(Canvas c, Draw d, int near, int far, int shadow) {
        d.rect(c, 0f, Theme.GROUND - 14f, Theme.VW, Theme.VH, far);
        d.rect(c, 0f, Theme.GROUND, Theme.VW, Theme.VH, near);
        d.rect(c, 0f, Theme.GROUND - 3f, Theme.VW, Theme.GROUND + 2f, shadow);
    }

    private static float flicker(float time, float speed, float seed) {
        return 0.5f + 0.5f * (float) Math.sin(time * speed + seed);
    }

    // ==================================================================
    //  0. 트로이 해안 — 새벽 상륙
    // ==================================================================
    private static void drawBeach(Canvas c, Draw d, float time, float px) {
        band(c, d, 0f, 180f, 0xFF1B2A44, 0xFF3C4E63, 7);
        band(c, d, 180f, 262f, 0xFF7E5B48, 0xFFD38C4E, 6);

        // 떠오르는 해
        d.circle(c, 760f - px * 10f, 214f, 46f, 0xFFF0B35E);
        d.circle(c, 760f - px * 10f, 214f, 62f, 0x33F0B35E);

        // 바다
        band(c, d, 262f, 330f, 0xFF37586F, 0xFF1D2F3E, 5);
        for (int i = 0; i < 10; i++) {
            float w = 60f + (i % 3) * 40f;
            float x = ((i * 113f) + time * 11f) % (Theme.VW + 160f) - 80f;
            float y = 276f + (i % 4) * 13f;
            d.rect(c, x, y, x + w, y + 2.5f, 0x55A8C4D4);
        }

        // 그리스 함대 실루엣
        for (int i = 0; i < 5; i++) {
            float sx = 90f + i * 170f - px * 18f;
            float sy = 288f + (i % 2) * 9f;
            float s = 0.8f + (i % 3) * 0.12f;
            ship(c, d, sx, sy, s, 0xFF131E28);
        }

        // 젖은 모래 (바다와 모래 사이를 메웁니다)
        band(c, d, 328f, Theme.GROUND - 12f, 0xFF53432F, 0xFF8E6E44, 6);

        // 모래
        ground(c, d, 0xFFB08A58, 0xFF8E6E44, 0xFF6A5132);
        for (int i = 0; i < 9; i++) {
            float x = i * 118f - px * 26f;
            d.oval(c, x, Theme.GROUND + 16f, x + 92f, Theme.GROUND + 30f, 0xFF9C7A4C);
        }
        // 모래에 꽂힌 창
        for (int i = 0; i < 6; i++) {
            float x = 60f + i * 160f - px * 22f;
            d.line(c, x, Theme.GROUND - 2f, x + 7f, Theme.GROUND - 62f, 3f, 0xFF6B5436);
        }
    }

    private static void ship(Canvas c, Draw d, float x, float y, float s, int col) {
        // 선체
        d.oval(c, x - 58f * s, y - 14f * s, x + 58f * s, y + 12f * s, col);
        d.rect(c, x - 52f * s, y - 16f * s, x + 52f * s, y - 2f * s, col);
        // 뱃머리
        d.triangle(c, x + 50f * s, y - 16f * s, x + 50f * s, y + 4f * s,
                x + 76f * s, y - 22f * s, col);
        // 돛대와 돛
        d.line(c, x, y - 14f * s, x, y - 62f * s, 3.5f * s, col);
        d.rect(c, x - 26f * s, y - 58f * s, x + 26f * s, y - 26f * s, col);
    }

    // ==================================================================
    //  1. 트로이 성벽 — 10년째 포위전
    // ==================================================================
    private static void drawWall(Canvas c, Draw d, float time, float px) {
        band(c, d, 0f, 200f, 0xFF2B3240, 0xFF55596B, 7);
        band(c, d, 200f, 300f, 0xFF6E6A6E, 0xFF8C7A68, 5);

        // 멀리 보이는 도시
        for (int i = 0; i < 7; i++) {
            float x = 40f + i * 136f - px * 10f;
            float h = 58f + (i % 3) * 26f;
            d.rect(c, x, 300f - h, x + 92f, 306f, 0xFF474351);
        }

        // 거대한 성벽
        float wallTop = 150f;
        d.rect(c, 0f, wallTop, Theme.VW, Theme.GROUND - 10f, 0xFF6B5E4F);
        for (int row = 0; row < 7; row++) {
            float y = wallTop + 18f + row * 38f;
            d.rect(c, 0f, y, Theme.VW, y + 2.5f, 0x44241D16);
            for (int i = 0; i < 9; i++) {
                float bx = (row % 2 == 0 ? 0f : 54f) + i * 110f - px * 4f;
                d.rect(c, bx, y - 34f, bx + 3f, y, 0x44241D16);
            }
        }
        // 성가퀴
        for (int i = 0; i < 14; i++) {
            float x = i * 70f - px * 4f;
            d.rect(c, x, wallTop - 26f, x + 42f, wallTop + 4f, 0xFF7A6B59);
        }
        // 탑
        tower(c, d, 130f - px * 6f);
        tower(c, d, 820f - px * 6f);

        // 트로이 깃발
        for (int i = 0; i < 3; i++) {
            float x = 250f + i * 240f - px * 6f;
            float wave = (float) Math.sin(time * 2.2f + i) * 5f;
            d.line(c, x, wallTop - 24f, x, wallTop - 86f, 3f, 0xFF4A3F33);
            d.rect(c, x, wallTop - 84f, x + 44f, wallTop - 56f + wave, Theme.TROY);
        }

        ground(c, d, 0xFF6E6049, 0xFF554A39, 0xFF3E3528);
    }

    private static void tower(Canvas c, Draw d, float x) {
        d.rect(c, x - 54f, 96f, x + 54f, Theme.GROUND - 10f, 0xFF766656);
        for (int i = 0; i < 5; i++) {
            d.rect(c, x - 54f + i * 24f, 70f, x - 54f + i * 24f + 15f, 100f, 0xFF83735F);
        }
        d.rect(c, x - 14f, 150f, x + 14f, 196f, 0xFF352C22);
    }

    // ==================================================================
    //  2. 성문 앞 야간 — 결투 / 함락
    // ==================================================================
    private static void drawNightGate(Canvas c, Draw d, float time, float px) {
        band(c, d, 0f, 230f, 0xFF0A0F1A, 0xFF28202C, 7);
        band(c, d, 230f, 326f, 0xFF28202C, 0xFF1A1620, 4);

        // 별
        for (int i = 0; i < 34; i++) {
            float sx = (i * 173f) % Theme.VW;
            float sy = 18f + (i * 61f) % 170f;
            float tw = flicker(time, 1.4f + (i % 5) * 0.4f, i);
            d.circle(c, sx, sy, 1.3f, Draw.withAlpha(0xFFE8E4D8, (int) (90 + 120 * tw)));
        }

        // 불타는 도시 실루엣
        for (int i = 0; i < 8; i++) {
            float x = 20f + i * 124f - px * 8f;
            float h = 70f + (i % 4) * 34f;
            d.rect(c, x, 300f - h, x + 86f, 320f, 0xFF171320);
            d.rect(c, x + 20f, 300f - h - 22f, x + 54f, 300f - h + 6f, 0xFF171320);
        }

        // 화염 빛
        for (int i = 0; i < 5; i++) {
            float fx = 90f + i * 200f - px * 8f;
            float f = flicker(time, 3.1f + i, i * 2f);
            d.circle(c, fx, 300f, 56f + f * 16f, Draw.withAlpha(0xFFD9602C, (int) (34 + 26 * f)));
        }

        // 성문
        float gx = 480f - px * 6f;
        d.rect(c, gx - 150f, 120f, gx + 150f, Theme.GROUND - 8f, 0xFF352C28);
        d.rect(c, gx - 110f, 170f, gx + 110f, Theme.GROUND - 8f, 0xFF1D1716);
        d.oval(c, gx - 110f, 118f, gx + 110f, 222f, 0xFF1D1716);
        d.roundStroke(c, gx - 150f, 120f, gx + 150f, Theme.GROUND - 8f, 6f, 5f, 0xFF4E4036);

        // 횃불
        torch(c, d, gx - 190f, time);
        torch(c, d, gx + 190f, time);

        ground(c, d, 0xFF3A3129, 0xFF2A231D, 0xFF17120F);
        // 바닥에 비치는 불빛
        d.rect(c, 0f, Theme.GROUND, Theme.VW, Theme.GROUND + 26f, 0x22D9602C);
    }

    private static void torch(Canvas c, Draw d, float x, float time) {
        d.rect(c, x - 4f, 300f, x + 4f, Theme.GROUND - 10f, 0xFF3A2E24);
        float f = flicker(time, 7f, x);
        d.circle(c, x, 292f, 11f + f * 3f, 0xFFE07A33);
        d.circle(c, x, 288f, 6f + f * 2f, 0xFFFFD27A);
        d.circle(c, x, 294f, 34f + f * 8f, 0x33FFA64D);
    }

    // ==================================================================
    //  3. 신들의 산 — 프롤로그 (황금 사과)
    // ==================================================================
    private static void drawOlympus(Canvas c, Draw d, float time, float px) {
        band(c, d, 0f, 300f, 0xFF2A1E3C, 0xFFB06A4E, 8);
        band(c, d, 300f, Theme.VH, 0xFFB06A4E, 0xFF4A2E33, 5);

        // 구름
        for (int i = 0; i < 6; i++) {
            float x = ((i * 220f) + time * 7f) % (Theme.VW + 300f) - 150f;
            float y = 120f + (i % 3) * 56f;
            d.oval(c, x, y, x + 190f, y + 40f, 0x3AFFE3C0);
            d.oval(c, x + 50f, y - 16f, x + 170f, y + 28f, 0x30FFE3C0);
        }

        // 신전 기둥
        for (int i = 0; i < 6; i++) {
            float x = 70f + i * 164f - px * 12f;
            d.rect(c, x, 150f, x + 34f, Theme.GROUND - 4f, 0x99E8D7BC);
            d.rect(c, x - 9f, 140f, x + 43f, 156f, 0xB3F0E2CB);
            d.rect(c, x - 9f, Theme.GROUND - 14f, x + 43f, Theme.GROUND - 2f, 0xB3F0E2CB);
        }
        d.rect(c, 20f, 112f, Theme.VW - 20f, 144f, 0xA6F0E2CB);

        // 황금 사과
        float ax = 480f;
        float ay = 250f + (float) Math.sin(time * 1.6f) * 9f;
        float glow = flicker(time, 2.4f, 0f);
        d.circle(c, ax, ay, 44f + glow * 10f, Draw.withAlpha(0xFFFFD36B, 46));
        d.circle(c, ax, ay, 24f, 0xFFFFC94F);
        d.circle(c, ax - 7f, ay - 8f, 7f, 0xFFFFF0B8);
        d.line(c, ax, ay - 22f, ax + 7f, ay - 36f, 3f, 0xFF6E5A2C);

        ground(c, d, 0xFF57414A, 0xFF3E2E36, 0xFF2A1F26);
    }

    // ==================================================================
    //  4. 귀향의 바다 — 엔딩
    // ==================================================================
    private static void drawSea(Canvas c, Draw d, float time, float px) {
        band(c, d, 0f, 248f, 0xFF201B3A, 0xFFC2653F, 8);

        // 지는 해
        d.circle(c, 480f, 244f, 78f, 0x55FFD08A);
        d.circle(c, 480f, 244f, 54f, 0xFFF2B36B);

        band(c, d, 248f, Theme.VH, 0xFF2A3E55, 0xFF101C2A, 8);

        for (int i = 0; i < 16; i++) {
            float y = 258f + i * 18f;
            float off = (float) Math.sin(time * 1.1f + i * 0.7f) * 22f;
            float w = 100f + i * 14f;
            d.rect(c, 480f + off - w * 0.5f, y, 480f + off + w * 0.5f, y + 3f,
                    Draw.withAlpha(0xFFE8B183, 70 - i * 3));
        }

        ship(c, d, 500f - px * 10f, 330f, 1.5f, 0xFF0E1620);
    }
}
