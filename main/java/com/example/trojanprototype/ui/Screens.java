package com.example.trojanprototype.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.StoryData;
import com.example.trojanprototype.util.Assets;
import com.example.trojanprototype.util.Draw;
import com.example.trojanprototype.util.SpriteSet;

/**
 * 전투를 뺀 나머지 화면 그리기
 * (타이틀 · 컷신 · 결과 · 엔딩)
 *
 * 담당: 심신 (화면 구성 / 문구) + 유장원 (상태 연결)
 *
 * ★ 캐릭터 선택 화면은 없앴습니다 ★
 *   주인공은 스토리가 정합니다(1~3장 아킬레우스 → 4장부터 오디세우스).
 *   난이도만 타이틀 화면에서 고릅니다.
 *
 * 버튼의 위치는 여기 선언된 float[] {왼쪽, 위, 오른쪽, 아래} 값을 씁니다.
 * GameEngine 이 같은 값으로 터치를 판정하므로, 버튼을 옮길 땐 여기만 고치면 됩니다.
 */
public final class Screens {

    private Screens() {}

    // ---------------- 버튼 위치 ----------------
    public static final float[][] TITLE_DIFF = {
            {60f, 366f, 204f, 410f},
            {218f, 366f, 362f, 410f},
            {376f, 366f, 520f, 410f}
    };
    public static final float[] TITLE_START = {60f, 190f, 520f, 278f};
    public static final float[] TITLE_HOWTO = {60f, 290f, 520f, 340f};

    public static final float[] STORY_SKIP = {812f, 22f, 938f, 66f};

    public static final float[] RESULT_PRIMARY = {500f, 414f, 800f, 476f};
    public static final float[] RESULT_SECONDARY = {160f, 414f, 460f, 476f};

    // ==================================================================
    //  타이틀
    // ==================================================================
    public static void drawTitle(Canvas c, Draw d, float time, int diffIndex) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        // 왼쪽 메뉴 영역을 어둡게 눌러 글자가 또렷하게 보이게 합니다.
        for (int i = 0; i < 8; i++) {
            d.rect(c, i * 80f, 0f, i * 80f + 81f, Theme.VH,
                    Draw.withAlpha(0xFF050302, 190 - i * 24));
        }

        // ---- 상단 바
        d.rect(c, 0f, 0f, Theme.VW, 56f, 0xE60A0705);
        d.line(c, 0f, 56f, Theme.VW, 56f, 1.2f, 0x88B8914F);
        d.spaced(c, "트로이 전쟁", 60f, 37f, 24f, Theme.TEXT_GOLD, Draw.LEFT, true, 6f);

        // ---- 로고 영역
        d.spaced(c, "트로이", 60f, 142f, 52f, Theme.TEXT, Draw.LEFT, true, 10f);
        d.line(c, 62f, 158f, 200f, 158f, 1.6f, 0xFFC9A55E);
        d.text(c, "얼굴, 전쟁, 남자, 생각 그리고 속임수", 62f, 178f, 14f,
                Theme.TEXT_GOLD, Draw.LEFT, false);

        // ---- 메인 카드: 이야기 시작
        float[] st = TITLE_START;
        d.card(c, st[0], st[1], st[2], st[3], true);
        float blink = 0.72f + 0.28f * (float) Math.sin(time * 3.4f);
        d.round(c, st[0], st[1], st[2], st[3], 3f, Draw.withAlpha(0xFFFFE2A8, (int) (14 * blink)));
        d.text(c, "이야기 시작", st[0] + 28f, st[1] + 40f, 30f, Theme.TEXT, Draw.LEFT, true);
        String sub = "트로이의 운명을 바꾸는 전쟁";
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            sub = GameData.DIFFICULTIES[diffIndex].note;
        }
        d.text(c, sub, st[0] + 28f, st[1] + 68f, 14f, Theme.TEXT_DIM, Draw.LEFT, false);
        d.chevron(c, st[2] - 30f, (st[1] + st[3]) * 0.5f, 8f, Theme.TEXT);

        // ---- 보조 카드: 조작 방법
        float[] ho = TITLE_HOWTO;
        d.card(c, ho[0], ho[1], ho[2], ho[3], false);
        d.text(c, "조작 방법", ho[0] + 28f, (ho[1] + ho[3]) * 0.5f + 7f, 20f,
                Theme.TEXT, Draw.LEFT, true);
        d.chevron(c, ho[2] - 30f, (ho[1] + ho[3]) * 0.5f, 7f, Theme.TEXT_DIM);

        // ---- 난이도
        d.spaced(c, "난이도", 62f, 358f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
        for (int i = 0; i < TITLE_DIFF.length && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = TITLE_DIFF[i];
            boolean on = (i == diffIndex);
            d.card(c, r[0], r[1], r[2], r[3], on);
            if (on) {
                d.round(c, r[0], r[1], r[2], r[3], 3f, 0x33C9A55E);
            }
            d.text(c, GameData.DIFFICULTIES[i].name, (r[0] + r[2]) * 0.5f + (on ? 2f : 0f),
                    (r[1] + r[3]) * 0.5f + 6f, 17f,
                    on ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.CENTER, true);
        }
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            GameData.Difficulty df = GameData.DIFFICULTIES[diffIndex];
            d.text(c, "AI 판단 주기 " + fmt(df.reactionTime) + "초", 62f, 436f, 12f,
                    Theme.TEXT_DIM, Draw.LEFT, false);
        }

        // ---- 하단 장식선
        d.line(c, 60f, 492f, 900f, 492f, 1f, 0x55B8914F);
        d.rect(c, 476f, 488f, 484f, 496f, 0xFFC9A55E);
    }

    /** 조작 방법 안내 (타이틀 위에 겹쳐 띄웁니다) */
    public static void drawHowTo(Canvas c, Draw d) {
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xE6050302);
        d.panel(c, 110f, 56f, 850f, 486f, Theme.PANEL);

        d.spaced(c, "조작 방법", 480f, 100f, 26f, Theme.TEXT_GOLD, Draw.CENTER, true, 6f);

        String[][] rows = {
                {"◀ ▶", "좌우 이동 — 누르고 있으면 계속 걷습니다"},
                {"공격", "탭해서 공격. 빠르게 연타하면 3타째에 강공격이 나갑니다"},
                {"방어", "누르고 있는 동안 피해가 75% 줄어듭니다 (강공격은 막히지 않음)"},
                {"회피", "짧게 무적이 됩니다. 적의 공격 직전에 쓰세요"},
                {"필살기", "분노가 100%가 되면 사용할 수 있습니다"},
                {"AI", "적 AI 의 판단 과정을 화면에 표시합니다"}
        };
        for (int i = 0; i < rows.length; i++) {
            float y = 146f + i * 42f;
            d.round(c, 150f, y - 20f, 258f, y + 12f, 3f, 0x66241A10);
            d.roundStroke(c, 150f, y - 20f, 258f, y + 12f, 3f, 1f, 0x66B8914F);
            d.text(c, rows[i][0], 204f, y + 3f, 16f, Theme.TEXT_GOLD, Draw.CENTER, true);
            d.text(c, rows[i][1], 278f, y + 3f, 14f, Theme.TEXT, Draw.LEFT, false);
        }

        d.line(c, 150f, 404f, 810f, 404f, 1f, 0x66B8914F);
        d.text(c, "1·2장은 군대전 — 그리스군(아킬레우스 + 돌격군 3 + 궁수 2) 대 트로이군. "
                + "적 대장을 먼저 쓰러뜨리면 승리", 480f, 424f, 13f, Theme.TEXT_GOLD,
                Draw.CENTER, false);
        d.text(c, "주인공은 이야기에 따라 바뀝니다 — 1~3장 아킬레우스(창), "
                + "마지막 장 오디세우스(활)", 480f, 444f, 13f, Theme.BRONZE_LIGHT,
                Draw.CENTER, false);
        d.text(c, "화면을 탭하면 닫힙니다", 480f, 472f, 13f, Theme.TEXT_DIM, Draw.CENTER, false);
    }

    // ==================================================================
    //  컷신 (이야기 장면)
    // ==================================================================
    public static void drawStory(Canvas c, Draw d, Assets assets, StoryData.Chapter ch,
                                 int shownLines, float lineTime, float time) {
        Backdrop.draw(c, d, ch.backdrop, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x88050302);

        boolean switching = (ch.newHero != null);
        // 주인공이 바뀌는 장에서는 글상자를 왼쪽으로 좁혀 오른쪽에 캐릭터를 세웁니다.
        float panelRight = switching ? 620f : 890f;

        d.panel(c, 70f, 96f, panelRight, 420f, 0xDD0C0806);

        d.spaced(c, ch.tag, 104f, 138f, 13f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
        d.textOutlined(c, ch.title, 104f, 178f, 30f, Theme.TEXT, Draw.LEFT, true);
        d.line(c, 104f, 196f, 280f, 196f, 1.6f, 0xFFC9A55E);

        // 주인공 교체 장은 대사가 길어서 글자와 줄 간격을 줄입니다.
        float fontSize = switching ? 15f : 17f;
        float lineGap = switching ? 26f : 34f;
        float firstLineY = switching ? 222f : 228f;
        for (int i = 0; i < ch.lines.length && i < shownLines; i++) {
            float a = (i == shownLines - 1) ? Draw.smooth(Draw.clamp(lineTime / 0.35f, 0f, 1f)) : 1f;
            float slide = (1f - a) * 14f;
            d.text(c, ch.lines[i], 104f + slide, firstLineY + i * lineGap, fontSize,
                    Draw.withAlpha(Theme.TEXT, (int) (235 * a)), Draw.LEFT, false);
        }

        // 주인공 교체 연출 — 모든 문장이 나온 뒤 천천히 떠오릅니다.
        if (switching && shownLines >= ch.lines.length) {
            drawHeroSwitch(c, d, assets, ch.newHero, lineTime);
        }

        // 다음 안내 — 대사 줄 수가 많은 장에서는 더 아래에 둡니다.
        float hintY = switching ? 406f : 396f;
        if (shownLines >= ch.lines.length) {
            float blink = 0.5f + 0.5f * (float) Math.sin(time * 4f);
            d.text(c, "화면을 탭하여 계속  ▶", panelRight - 30f, hintY, 14f,
                    Draw.withAlpha(Theme.TEXT_GOLD, (int) (120 + 135 * blink)), Draw.RIGHT, true);
        } else {
            d.text(c, "화면을 탭하면 다음 문장", panelRight - 30f, hintY, 13f,
                    Theme.TEXT_DIM, Draw.RIGHT, false);
        }

        BattleRenderer.menuButton(c, d, STORY_SKIP, "건너뛰기", false);
    }

    /** "이제부터 이 캐릭터로 싸웁니다" 안내 */
    private static void drawHeroSwitch(Canvas c, Draw d, Assets assets,
                                       GameData.CharacterDef hero, float t) {
        float a = Draw.smooth(Draw.clamp(t / 0.8f, 0f, 1f));
        int alpha = (int) (255 * a);
        float rise = (1f - a) * 22f;

        float cx = 772f;
        float base = 404f + rise;

        SpriteSet set = assets.spriteSet(hero.spriteName);
        Bitmap bmp = set.frame(SpriteSet.F_IDLE_A);
        float targetH = 186f;
        float scale = set.scaleFor(targetH);
        float w = set.canvasW * scale;
        float h = set.canvasH * scale;

        d.oval(c, cx - 44f, base - 6f, cx + 44f, base + 8f,
                Draw.withAlpha(0xFF000000, (int) (70 * a)));
        if (bmp != null) {
            d.bitmap(c, bmp, cx - set.anchorX * scale, base - h, cx - set.anchorX * scale + w,
                    base, alpha);
        }

        // 캐릭터 그림과 겹치지 않도록 이름과 수식어만 위에 둡니다.
        // (세부 특징은 전투 시작 안내와 '조작 방법'에서 설명합니다)
        d.text(c, "플레이어 캐릭터 변경", cx, 144f + rise, 12f,
                Draw.withAlpha(Theme.BRONZE_LIGHT, alpha), Draw.CENTER, true);
        d.textOutlined(c, hero.name, cx, 178f + rise, 28f,
                Draw.withAlpha(Theme.TEXT, alpha), Draw.CENTER, true);
        d.text(c, hero.title, cx, 200f + rise, 12f,
                Draw.withAlpha(hero.accent, alpha), Draw.CENTER, false);
    }

    // ==================================================================
    //  전투 결과 + 해설
    // ==================================================================
    public static void drawResult(Canvas c, Draw d, BattleWorld w, boolean hasNext, float time) {
        Backdrop.draw(c, d, w.stage == null ? 0 : w.stage.backdrop, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xCC050302);

        boolean win = (w.result == BattleWorld.RESULT_WIN);

        d.panel(c, 90f, 56f, 870f, 398f, 0xEE0C0806);

        d.text(c, w.stage.chapter + " · " + w.stage.place, 130f, 98f, 13f,
                Theme.BRONZE_LIGHT, Draw.LEFT, true);
        d.textOutlined(c, win ? "승리" : "패배", 130f, 150f, 40f,
                win ? Theme.TEXT_GOLD : 0xFFD9604C, Draw.LEFT, true);

        // 해설 (스토리 전달)
        String note = win ? w.stage.victoryNote : w.stage.defeatNote;
        d.text(c, note, 130f, 196f, 16f, Theme.TEXT, Draw.LEFT, false);
        d.line(c, 130f, 222f, 830f, 222f, 1f, 0x66B8914F);

        // 전투 기록
        int sec = (int) w.elapsed;
        record(c, d, 130f, 252f, "남은 체력", w.player.hp + " / " + w.player.maxHp);
        record(c, d, 330f, 252f, "최대 연속타격", w.bestCombo + " 타");
        record(c, d, 530f, 252f, "소요 시간", sec + "초");
        record(c, d, 700f, 252f, "난이도", w.difficulty.name);

        record(c, d, 130f, 306f, "상대", w.enemy.name + " (" + w.enemy.title + ")");
        record(c, d, 530f, 306f, "사용 캐릭터", w.character.name);
        if (w.armyMode) {
            record(c, d, 130f, 360f, "생존 병사",
                    "그리스군 " + (w.aliveSoldiers(0, Fighter.ROLE_ASSAULT) + w.aliveSoldiers(0, Fighter.ROLE_ARCHER))
                            + " / " + w.totalSoldiers(0)
                            + "   트로이군 " + (w.aliveSoldiers(1, Fighter.ROLE_ASSAULT) + w.aliveSoldiers(1, Fighter.ROLE_ARCHER))
                            + " / " + w.totalSoldiers(1));
        }

        BattleRenderer.menuButton(c, d, RESULT_PRIMARY,
                win ? (hasNext ? "다음 장으로" : "엔딩 보기") : "다시 도전", true);
        BattleRenderer.menuButton(c, d, RESULT_SECONDARY, "타이틀로", false);
    }

    private static void record(Canvas c, Draw d, float x, float y, String label, String value) {
        d.text(c, label, x, y, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        d.text(c, value, x, y + 22f, 18f, Theme.TEXT, Draw.LEFT, true);
    }

    // ==================================================================
    //  엔딩 — 영화 《오디세이》로 연결
    // ==================================================================
    public static void drawFinale(Canvas c, Draw d, float time) {
        Backdrop.draw(c, d, Backdrop.SEA, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x99050302);

        float a1 = Draw.smooth(Draw.clamp(time / 1.2f, 0f, 1f));
        float a2 = Draw.smooth(Draw.clamp((time - 1.4f) / 1.2f, 0f, 1f));
        float a3 = Draw.smooth(Draw.clamp((time - 2.8f) / 1.2f, 0f, 1f));

        d.textOutlined(c, StoryData.FINALE[0], 480f, 212f, 70f,
                Draw.withAlpha(Theme.TEXT, (int) (255 * a1)), Draw.CENTER, true);
        d.text(c, StoryData.FINALE[1], 480f, 248f, 20f,
                Draw.withAlpha(Theme.TEXT_GOLD, (int) (255 * a1)), Draw.CENTER, false);

        d.text(c, StoryData.FINALE[2], 480f, 318f, 17f,
                Draw.withAlpha(Theme.TEXT, (int) (230 * a2)), Draw.CENTER, false);
        d.text(c, StoryData.FINALE[3], 480f, 348f, 17f,
                Draw.withAlpha(Theme.TEXT, (int) (230 * a3)), Draw.CENTER, false);

        if (time > 4.2f) {
            float blink = 0.5f + 0.5f * (float) Math.sin(time * 3.4f);
            d.text(c, "화면을 탭하면 타이틀로", 480f, 448f, 14f,
                    Draw.withAlpha(Theme.TEXT_GOLD, (int) (120 + 135 * blink)),
                    Draw.CENTER, true);
        }
    }

    private static String fmt(float v) {
        int hundredths = Math.round(v * 100f);
        return (hundredths / 100) + "." + pad(Math.abs(hundredths % 100));
    }

    private static String pad(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }
}
