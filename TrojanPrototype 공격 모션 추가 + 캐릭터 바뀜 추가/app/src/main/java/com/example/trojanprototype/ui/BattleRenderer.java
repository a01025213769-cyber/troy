package com.example.trojanprototype.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.core.TouchInput;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.util.Draw;
import com.example.trojanprototype.util.SpriteSet;

/**
 * 전투 화면 그리기 전담.
 *
 * 담당: 심신 (연출) + 유장원 (상태 연결)
 *
 * BattleWorld 는 "계산"만 하고, 이 파일은 "그리기"만 합니다.
 * 두 가지를 나눠 두면 연출을 바꿔도 게임 규칙이 깨지지 않습니다.
 *
 * 스프라이트가 한 장짜리 그림이라, 걷기/공격 동작은
 * 기울이기·늘이기·흔들기 같은 변형으로 표현합니다.
 * 나중에 심신이 프레임 애니메이션을 만들면 drawFighter() 안만 바꾸면 됩니다.
 */
public final class BattleRenderer {

    private BattleRenderer() {}

    // 일시정지 창의 버튼 위치 — GameEngine 도 이 값을 그대로 씁니다.
    public static final float[] PAUSE_RESUME = {350f, 212f, 610f, 264f};
    public static final float[] PAUSE_RETRY = {350f, 276f, 610f, 328f};
    public static final float[] PAUSE_TITLE = {350f, 340f, 610f, 392f};

    public static void draw(Canvas c, Draw d, BattleWorld w, TouchInput in,
                            boolean aiDebug, boolean paused) {
        // ---------- 화면 흔들림 ----------
        int save = c.save();
        if (w.shake > 0f) {
            float s = w.shake * 7f;
            c.translate((float) (Math.random() * 2 - 1) * s, (float) (Math.random() * 2 - 1) * s);
        }

        // ---------- 배경 ----------
        float focus = (w.player.x + w.enemy.x) * 0.5f;
        float parallax = Draw.clamp((focus - 480f) / 400f, -1f, 1f);
        Backdrop.draw(c, d, w.stage == null ? 0 : w.stage.backdrop, w.elapsed, parallax);

        // ---------- 캐릭터 ----------
        // 뒤쪽(화면 위)에 있는 쪽을 먼저 그려 겹침이 자연스럽게 보이도록
        if (w.player.x <= w.enemy.x) {
            drawFighter(c, d, w.enemy);
            drawFighter(c, d, w.player);
        } else {
            drawFighter(c, d, w.player);
            drawFighter(c, d, w.enemy);
        }

        drawArrows(c, d, w);
        drawEffects(c, d, w);

        c.restoreToCount(save);

        // ---------- 화면 전체 번쩍임 ----------
        if (w.screenFlash > 0f) {
            d.rect(c, 0f, 0f, Theme.VW, Theme.VH,
                    Draw.withAlpha(0xFFFFF0D0, (int) (w.screenFlash * 90f)));
        }

        // ---------- HUD ----------
        drawHud(c, d, w);
        VirtualPad.draw(c, d, in, w.player, aiDebug);

        if (aiDebug) {
            AiDebugView.drawHitboxes(c, d, w);
            AiDebugView.draw(c, d, w);
        }

        if (w.phase == BattleWorld.PHASE_INTRO) drawIntro(c, d, w);
        if (w.phase == BattleWorld.PHASE_END) drawOutro(c, d, w);
        if (paused) drawPause(c, d);
    }

    // ==================================================================
    //  캐릭터
    // ==================================================================
    /**
     * 캐릭터 상태에 맞는 동작 프레임을 고릅니다.
     * (f0 대기 / f1 대기2 / f2 걷기 / f3 달리기 / f4 공격 / f5 방어)
     */
    private static int frameFor(Fighter f) {
        switch (f.state) {
            case Fighter.WALK:
                // 두 프레임을 번갈아 걷는 느낌을 냅니다.
                return (f.animTime % 0.36f < 0.18f) ? SpriteSet.F_WALK : SpriteSet.F_RUN;

            case Fighter.ATTACK:
                if (f.move == null) return SpriteSet.F_IDLE_A;
                // 활은 "당기는 모습"이 곧 공격 프레임이라 선딜부터 보여 줍니다.
                if (f.move.ranged) return SpriteSet.F_ATTACK;
                // 근접 공격은 선딜에 준비 자세 → 판정부터 찌르는 그림
                return (f.attackPhase() == Fighter.PHASE_WINDUP)
                        ? SpriteSet.F_IDLE_B : SpriteSet.F_ATTACK;

            case Fighter.GUARD:
                return SpriteSet.F_GUARD;

            case Fighter.DODGE:
                return SpriteSet.F_RUN;

            case Fighter.HURT:
                return SpriteSet.F_IDLE_B;

            case Fighter.DOWN:
                return SpriteSet.F_IDLE_A;

            case Fighter.VICTORY:
                return (f.animTime % 1.4f < 0.7f) ? SpriteSet.F_IDLE_A : SpriteSet.F_IDLE_B;

            default: // IDLE
                return (f.animTime % 1.6f < 0.8f) ? SpriteSet.F_IDLE_A : SpriteSet.F_IDLE_B;
        }
    }

    private static void drawFighter(Canvas c, Draw d, Fighter f) {
        SpriteSet ss = f.sprites;
        Bitmap bmp = (ss == null) ? null : ss.frame(frameFor(f));

        // 프레임 캔버스를 통째로 그립니다.
        //  · 캔버스 아래 = 바닥선
        //  · 캔버스 안의 몸 중심(anchorX) = 캐릭터 좌표 f.x
        // 이렇게 하면 공격 프레임의 긴 창이 자연스럽게 앞으로 뻗습니다.
        float scale = (ss == null) ? 1f : ss.scaleFor(f.spriteHeight);
        float canvasW = (ss == null) ? GameData.BODY_W : ss.canvasW;
        float canvasH = (ss == null) ? f.spriteHeight : ss.canvasH;
        float anchor = (ss == null) ? canvasW * 0.5f : ss.anchorX;

        float drawW = canvasW * scale;
        float drawH = canvasH * scale;

        // 발밑 그림자 — 캔버스가 아니라 몸 크기에 맞춥니다.
        float shW = GameData.BODY_W * 0.62f;
        d.oval(c, f.x - shW, Theme.GROUND - 5f, f.x + shW, Theme.GROUND + 5f, 0x4A000000);

        // 앞쪽(+x) = 바라보는 방향이 되도록 좌우 반전
        int save = c.save();
        if (f.facing < 0) c.scale(-1f, 1f, f.x, Theme.GROUND);

        float lean = 0f;      // + 면 앞으로
        float bob = 0f;       // 위아래 흔들림
        float scaleX = 1f, scaleY = 1f;
        float rot = 0f;
        int alpha = 255;

        switch (f.state) {
            case Fighter.IDLE:
                bob = (float) Math.sin(f.animTime * 3.0f) * 2.5f;
                break;
            case Fighter.WALK:
                bob = Math.abs((float) Math.sin(f.animTime * 10f)) * 5f;
                lean = 3f;
                break;
            case Fighter.ATTACK:
                if (f.move != null) {
                    int ph = f.attackPhase();
                    if (ph == Fighter.PHASE_WINDUP) {
                        float t = f.stateTime / Math.max(0.001f, f.move.windup);
                        lean = -9f * t;
                        scaleY = 1f + 0.05f * t;
                    } else if (ph == Fighter.PHASE_ACTIVE) {
                        lean = 20f;
                        scaleX = 1.08f;
                        scaleY = 0.96f;
                    } else {
                        float t = (f.stateTime - f.move.windup - f.move.active)
                                / Math.max(0.001f, f.move.recover);
                        lean = 14f * (1f - Draw.clamp(t, 0f, 1f));
                    }
                    if (f.usingUltimate) {
                        bob = (float) Math.sin(f.animTime * 26f) * 3f;
                    }
                }
                break;
            case Fighter.GUARD:
                lean = -6f;
                scaleY = 0.95f;
                break;
            case Fighter.DODGE:
                rot = -14f;
                lean = -10f;
                alpha = f.isInvincible() ? 140 : 235;
                break;
            case Fighter.HURT:
                lean = -13f;
                scaleX = 0.97f;
                break;
            case Fighter.DOWN:
                rot = 74f;
                alpha = 220;
                break;
            case Fighter.VICTORY:
                bob = (float) Math.sin(f.animTime * 2.6f) * 5f;
                scaleY = 1.02f;
                break;
            default:
                break;
        }

        if (rot != 0f) c.rotate(rot, f.x, Theme.GROUND);

        float w = drawW * scaleX;
        float hgt = drawH * scaleY;
        float left = f.x - anchor * scale * scaleX + lean;
        float top = Theme.GROUND - hgt - bob;

        if (bmp != null) {
            d.bitmap(c, bmp, left, top, left + w, Theme.GROUND - bob, alpha);
        } else {
            // 그림이 없을 때도 게임이 보이도록 간단한 실루엣을 그립니다.
            d.round(c, f.x - GameData.BODY_W * 0.4f, Theme.GROUND - f.spriteHeight,
                    f.x + GameData.BODY_W * 0.4f, Theme.GROUND - bob,
                    10f, Draw.withAlpha(f.accent, alpha));
        }

        // 맞은 직후 하얗게 번쩍 — 캔버스가 아니라 몸 위치에 맞춥니다.
        if (f.flashTime > 0f) {
            int a = (int) (f.flashTime * 900f);
            d.round(c, f.x - GameData.BODY_W * 0.5f, Theme.GROUND - f.spriteHeight - bob,
                    f.x + GameData.BODY_W * 0.5f, Theme.GROUND - bob,
                    8f, Draw.withAlpha(0xFFFFFFFF, Math.min(150, a)));
        }

        // 방어 중 방패 빛
        if (f.state == Fighter.GUARD) {
            float gx = f.x + GameData.BODY_W * 0.55f;
            d.circle(c, gx, Theme.GROUND - f.spriteHeight * 0.52f, 26f, 0x553FA0D8);
            d.circleStroke(c, gx, Theme.GROUND - f.spriteHeight * 0.52f, 26f, 2.5f, 0xAA9FD8F0);
        }

        c.restoreToCount(save);
    }

    // ==================================================================
    //  화살
    // ==================================================================
    private static void drawArrows(Canvas c, Draw d, BattleWorld w) {
        for (int i = 0; i < w.arrows.length; i++) {
            BattleWorld.Arrow a = w.arrows[i];
            if (!a.active) continue;

            float len = 30f;
            float tipX = a.x + a.dir * len * 0.5f;
            float tailX = a.x - a.dir * len * 0.5f;

            // 잔상
            d.line(c, tailX - a.dir * 18f, a.y, tailX, a.y, 2f, 0x44FFE8B8);
            // 화살대
            d.line(c, tailX, a.y, tipX, a.y, 3f, 0xFF6B5436);
            // 촉
            d.triangle(c, tipX + a.dir * 8f, a.y,
                    tipX - a.dir * 4f, a.y - 5f,
                    tipX - a.dir * 4f, a.y + 5f, 0xFFD8DEE4);
            // 깃
            d.triangle(c, tailX, a.y,
                    tailX - a.dir * 10f, a.y - 6f,
                    tailX - a.dir * 10f, a.y + 6f, 0xFFB8604C);
        }
    }

    // ==================================================================
    //  이펙트
    // ==================================================================
    private static void drawEffects(Canvas c, Draw d, BattleWorld w) {
        for (int i = 0; i < w.effects.length; i++) {
            BattleWorld.Effect e = w.effects[i];
            if (!e.active) continue;
            float t = e.life / Math.max(0.001f, e.maxLife);
            int a = (int) (255 * Draw.clamp(t, 0f, 1f));

            switch (e.type) {
                case BattleWorld.FX_SPARK:
                    d.circle(c, e.x, e.y, e.size * t, Draw.withAlpha(e.color, a));
                    break;
                case BattleWorld.FX_DUST:
                    d.circle(c, e.x, e.y, e.size * t, Draw.withAlpha(e.color, (int) (a * 0.6f)));
                    break;
                case BattleWorld.FX_GUARD:
                    d.circleStroke(c, e.x, e.y, e.size * (1.6f - t), 3f,
                            Draw.withAlpha(e.color, a));
                    break;
                case BattleWorld.FX_TEXT:
                    d.textOutlined(c, e.text, e.x, e.y, e.size,
                            Draw.withAlpha(e.color, a), Draw.CENTER, true);
                    break;
                default:
                    break;
            }
        }
    }

    // ==================================================================
    //  HUD (체력 · 분노 · 시간)
    // ==================================================================
    private static void drawHud(Canvas c, Draw d, BattleWorld w) {
        float top = Theme.HUD_TOP;

        // ---- 플레이어 (왼쪽)
        d.text(c, w.player.name, 38f, top + 18f, 17f, Theme.TEXT, Draw.LEFT, true);
        d.text(c, w.player.title, 38f + d.textWidth(w.player.name, 17f, true) + 10f, top + 18f,
                11f, Theme.TEXT_DIM, Draw.LEFT, false);

        float hpT = top + 26f;
        d.gauge(c, 38f, hpT, 38f + Theme.HP_BAR_W, hpT + Theme.HP_BAR_H,
                w.player.hpRatio(), Theme.HP_BACK,
                w.player.hpRatio() < 0.3f ? Theme.HP_PLAYER_LOW : Theme.HP_PLAYER, 5f);
        d.roundStroke(c, 38f, hpT, 38f + Theme.HP_BAR_W, hpT + Theme.HP_BAR_H, 5f, 1.5f, 0x55FFFFFF);
        d.text(c, w.player.hp + " / " + w.player.maxHp, 38f + Theme.HP_BAR_W - 6f, hpT + 15f,
                11f, 0xDDFFFFFF, Draw.RIGHT, true);

        // 분노 게이지
        float rgT = hpT + Theme.HP_BAR_H + 5f;
        float rageRatio = w.player.rage / GameData.RAGE_MAX;
        d.text(c, "분노", 38f, rgT + 10f, 11f, Theme.TEXT_DIM, Draw.LEFT, true);
        d.gauge(c, 70f, rgT, 70f + 200f, rgT + 11f, rageRatio, Theme.HP_BACK,
                w.player.ultimateReady() ? Theme.RAGE_FULL : Theme.RAGE, 4f);
        if (w.player.ultimateReady()) {
            d.text(c, "필살기 준비!", 280f, rgT + 10f, 11f, Theme.RAGE_FULL, Draw.LEFT, true);
        }

        // 콤보
        if (w.combo >= 2) {
            d.textOutlined(c, w.combo + " 연속타격", 38f, rgT + 34f, 16f,
                    Theme.TEXT_GOLD, Draw.LEFT, true);
        }

        // ---- 적 (오른쪽)
        d.text(c, w.enemy.name, Theme.VW - 38f, top + 18f, 17f, Theme.TEXT, Draw.RIGHT, true);
        d.text(c, w.enemy.title,
                Theme.VW - 38f - d.textWidth(w.enemy.name, 17f, true) - 10f, top + 18f,
                11f, Theme.TEXT_DIM, Draw.RIGHT, false);

        float eL = Theme.VW - 38f - Theme.HP_BAR_W;
        d.round(c, eL, hpT, eL + Theme.HP_BAR_W, hpT + Theme.HP_BAR_H, 5f, Theme.HP_BACK);
        float ew = Theme.HP_BAR_W * Draw.clamp(w.enemy.hpRatio(), 0f, 1f);
        if (ew > 1f) {
            // 오른쪽에서 왼쪽으로 줄어들도록
            d.round(c, eL + Theme.HP_BAR_W - ew, hpT, eL + Theme.HP_BAR_W, hpT + Theme.HP_BAR_H,
                    5f, Theme.HP_ENEMY);
        }
        d.roundStroke(c, eL, hpT, eL + Theme.HP_BAR_W, hpT + Theme.HP_BAR_H, 5f, 1.5f, 0x55FFFFFF);
        d.text(c, w.enemy.hp + " / " + w.enemy.maxHp, eL + 6f, hpT + 15f, 11f,
                0xDDFFFFFF, Draw.LEFT, true);
        if (w.stage != null && w.stage.boss) {
            d.text(c, "BOSS", Theme.VW - 38f, rgT + 10f, 12f, 0xFFFF9C6B, Draw.RIGHT, true);
        }

        // ---- 가운데: 남은 시간
        int sec = (int) Math.ceil(w.timeLeft);
        if (sec < 0) sec = 0;
        d.textOutlined(c, String.valueOf(sec), 480f, top + 40f, 36f,
                sec <= 10 ? 0xFFFF8E72 : Theme.TEXT, Draw.CENTER, true);
        if (w.stage != null) {
            d.text(c, w.stage.chapter + " · " + w.stage.place, 480f, top + 62f, 12f,
                    Theme.TEXT_DIM, Draw.CENTER, false);
        }
    }

    // ==================================================================
    //  전투 시작 / 종료 연출
    // ==================================================================
    private static void drawIntro(Canvas c, Draw d, BattleWorld w) {
        float t = w.phaseTime;
        if (t < 1.25f) {
            float a = Draw.smooth(Draw.clamp(t / 0.35f, 0f, 1f));
            int fade = (int) (170 * a);
            d.rect(c, 0f, 0f, Theme.VW, Theme.VH, Draw.withAlpha(0xFF070B12, fade));

            float slide = (1f - a) * 60f;
            d.textOutlined(c, w.stage.chapter, 480f - slide, 206f, 16f,
                    Theme.TEXT_GOLD, Draw.CENTER, true);
            d.textOutlined(c, w.stage.place, 480f - slide, 248f, 30f,
                    Theme.TEXT, Draw.CENTER, true);
            d.textOutlined(c, w.player.name + "   VS   " + w.enemy.name, 480f + slide, 300f, 20f,
                    Theme.BRONZE_LIGHT, Draw.CENTER, true);
            if (w.tip != null && w.tip.length() > 0) {
                d.text(c, w.tip, 480f, 338f, 13f, Theme.TEXT_DIM, Draw.CENTER, false);
            }
        } else {
            float a = Draw.clamp((1.9f - t) / 0.5f, 0f, 1f);
            float scale = 1f + (1f - a) * 0.5f;
            d.textOutlined(c, "시작!", 480f, 262f, 54f * scale,
                    Draw.withAlpha(Theme.TEXT_GOLD, (int) (255 * a)), Draw.CENTER, true);
        }
    }

    private static void drawOutro(Canvas c, Draw d, BattleWorld w) {
        float a = Draw.smooth(Draw.clamp(w.phaseTime / 0.5f, 0f, 1f));
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, Draw.withAlpha(0xFF070B12, (int) (120 * a)));
        boolean win = (w.result == BattleWorld.RESULT_WIN);
        d.textOutlined(c, win ? "승리" : "패배", 480f, 268f, 62f,
                Draw.withAlpha(win ? Theme.TEXT_GOLD : 0xFFE08878, (int) (255 * a)),
                Draw.CENTER, true);
    }

    private static void drawPause(Canvas c, Draw d) {
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xCC070B12);
        d.text(c, "일시정지", 480f, 168f, 30f, Theme.TEXT, Draw.CENTER, true);
        menuButton(c, d, PAUSE_RESUME, "계속하기", true);
        menuButton(c, d, PAUSE_RETRY, "이 전투 다시 시작", false);
        menuButton(c, d, PAUSE_TITLE, "타이틀로 돌아가기", false);
    }

    /** 공용 메뉴 버튼 — Screens 에서도 같은 모양을 씁니다. */
    public static void menuButton(Canvas c, Draw d, float[] r, String label, boolean primary) {
        d.round(c, r[0], r[1], r[2], r[3], 10f,
                primary ? Theme.BRONZE : 0xCC28333F);
        d.roundStroke(c, r[0], r[1], r[2], r[3], 10f, 1.6f,
                primary ? 0xFFF2D49A : Theme.PANEL_LINE);
        d.text(c, label, (r[0] + r[2]) * 0.5f, (r[1] + r[3]) * 0.5f + 7f, 19f,
                primary ? 0xFF231A10 : Theme.TEXT, Draw.CENTER, true);
    }
}
