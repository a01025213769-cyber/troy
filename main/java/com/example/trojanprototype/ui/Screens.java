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
            {284f, 282f, 404f, 322f},
            {420f, 282f, 540f, 322f},
            {556f, 282f, 676f, 322f}
    };
    public static final float[] TITLE_START = {330f, 340f, 630f, 396f};
    public static final float[] TITLE_HOWTO = {330f, 406f, 630f, 448f};

    public static final float[] STORY_SKIP = {812f, 22f, 938f, 66f};

    public static final float[] RESULT_PRIMARY = {500f, 414f, 800f, 476f};
    public static final float[] RESULT_SECONDARY = {160f, 414f, 460f, 476f};

    // ==================================================================
    //  타이틀
    // ==================================================================
    public static void drawTitle(Canvas c, Draw d, float time, int diffIndex) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x99070B12);
        // 제목이 놓이는 구간을 가로 전체로 한 번 더 어둡게 덮어
        // 뒤에 있는 성문의 세로 경계가 글자를 가로지르지 않게 합니다.
        d.rect(c, 0f, 52f, Theme.VW, 268f, 0x77070B12);

        d.text(c, "ANDROID · JAVA · 2D FIGHTING", 480f, 84f, 13f, Theme.BRONZE_LIGHT,
                Draw.CENTER, true);
        d.textOutlined(c, "트로이 전쟁", 480f, 162f, 60f, Theme.TEXT, Draw.CENTER, true);
        d.text(c, "얼굴, 전쟁, 남자, 생각 그리고 속임수", 480f, 198f, 17f,
                Theme.TEXT_GOLD, Draw.CENTER, false);
        d.line(c, 366f, 220f, 594f, 220f, 1.5f, 0x55E9BE7B);
        d.text(c, "영화 《오디세이》의 배경이 된 전쟁을 플레이로 따라갑니다", 480f, 246f, 14f,
                Theme.TEXT_DIM, Draw.CENTER, false);

        // 난이도 선택
        d.text(c, "난이도", 252f, 308f, 14f, Theme.TEXT_DIM, Draw.RIGHT, true);
        for (int i = 0; i < TITLE_DIFF.length && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = TITLE_DIFF[i];
            boolean on = (i == diffIndex);
            d.round(c, r[0], r[1], r[2], r[3], 9f, on ? Theme.BRONZE : 0xB3222D38);
            d.roundStroke(c, r[0], r[1], r[2], r[3], 9f, 1.5f,
                    on ? 0xFFF2D49A : Theme.PANEL_LINE);
            d.text(c, GameData.DIFFICULTIES[i].name, (r[0] + r[2]) * 0.5f,
                    (r[1] + r[3]) * 0.5f + 6f, 17f,
                    on ? 0xFF231A10 : Theme.TEXT, Draw.CENTER, true);
        }
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            GameData.Difficulty df = GameData.DIFFICULTIES[diffIndex];
            d.text(c, df.note + " · AI 판단 주기 " + fmt(df.reactionTime) + "초",
                    700f, 308f, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        }

        // 깜빡이는 시작 버튼
        float blink = 0.72f + 0.28f * (float) Math.sin(time * 3.4f);
        BattleRenderer.menuButton(c, d, TITLE_START, "이야기 시작", true);
        d.round(c, TITLE_START[0], TITLE_START[1], TITLE_START[2], TITLE_START[3], 10f,
                Draw.withAlpha(0xFFFFFFFF, (int) (22 * blink)));
        BattleRenderer.menuButton(c, d, TITLE_HOWTO, "조작 방법", false);

        d.text(c, "10조 · 심신 · 유장원", 480f, 476f, 11f, Theme.TEXT_DIM, Draw.CENTER, false);
        d.text(c, StoryData.DISCLAIMER, 480f, 500f, 11f, 0x99A8B4BE, Draw.CENTER, false);
    }

    /** 조작 방법 안내 (타이틀 위에 겹쳐 띄웁니다) */
    public static void drawHowTo(Canvas c, Draw d) {
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xE6070B12);
        d.round(c, 110f, 56f, 850f, 486f, 14f, Theme.PANEL);
        d.roundStroke(c, 110f, 56f, 850f, 486f, 14f, 1.5f, Theme.PANEL_LINE);

        d.text(c, "조작 방법", 480f, 100f, 26f, Theme.TEXT_GOLD, Draw.CENTER, true);

        String[][] rows = {
                {"◀ ▶", "좌우 이동 — 누르고 있으면 계속 걷습니다"},
                {"공격", "탭해서 공격. 빠르게 연타하면 3타째에 강공격이 나갑니다"},
                {"방어", "누르고 있는 동안 피해가 75% 줄어듭니다 (강공격은 막히지 않음)"},
                {"회피", "짧게 무적이 됩니다. 적의 공격 직전에 쓰세요"},
                {"필살기", "분노가 100%가 되면 사용할 수 있습니다"},
                {"AI", "적 AI 의 판단 과정을 화면에 표시합니다 (발표 시연용)"}
        };
        for (int i = 0; i < rows.length; i++) {
            float y = 146f + i * 42f;
            d.round(c, 150f, y - 20f, 258f, y + 12f, 8f, 0x55313E4C);
            d.text(c, rows[i][0], 204f, y + 3f, 16f, Theme.TEXT_GOLD, Draw.CENTER, true);
            d.text(c, rows[i][1], 278f, y + 3f, 14f, Theme.TEXT, Draw.LEFT, false);
        }

        d.line(c, 150f, 404f, 810f, 404f, 1f, 0x33E9BE7B);
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
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x88070B12);

        boolean switching = (ch.newHero != null);
        // 주인공이 바뀌는 장에서는 글상자를 왼쪽으로 좁혀 오른쪽에 캐릭터를 세웁니다.
        float panelRight = switching ? 620f : 890f;

        d.round(c, 70f, 96f, panelRight, 420f, 14f, 0xD60E141E);
        d.roundStroke(c, 70f, 96f, panelRight, 420f, 14f, 1.5f, Theme.PANEL_LINE);

        d.text(c, ch.tag, 104f, 138f, 14f, Theme.BRONZE_LIGHT, Draw.LEFT, true);
        d.textOutlined(c, ch.title, 104f, 178f, 30f, Theme.TEXT, Draw.LEFT, true);
        d.line(c, 104f, 196f, 280f, 196f, 1.5f, 0x55E9BE7B);

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
        d.text(c, StoryData.DISCLAIMER, 480f, 448f, 11f, 0x88A8B4BE, Draw.CENTER, false);
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
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xCC070B12);

        boolean win = (w.result == BattleWorld.RESULT_WIN);

        d.round(c, 90f, 56f, 870f, 388f, 14f, 0xE60E141E);
        d.roundStroke(c, 90f, 56f, 870f, 388f, 14f, 1.5f, Theme.PANEL_LINE);

        d.text(c, w.stage.chapter + " · " + w.stage.place, 130f, 98f, 13f,
                Theme.BRONZE_LIGHT, Draw.LEFT, true);
        d.textOutlined(c, win ? "승리" : "패배", 130f, 150f, 40f,
                win ? Theme.TEXT_GOLD : 0xFFE08878, Draw.LEFT, true);

        // 해설 (스토리 전달)
        String note = win ? w.stage.victoryNote : w.stage.defeatNote;
        d.text(c, note, 130f, 196f, 16f, Theme.TEXT, Draw.LEFT, false);
        d.line(c, 130f, 222f, 830f, 222f, 1f, 0x33E9BE7B);

        // 전투 기록
        int sec = (int) w.elapsed;
        record(c, d, 130f, 262f, "남은 체력", w.player.hp + " / " + w.player.maxHp);
        record(c, d, 330f, 262f, "최대 연속타격", w.bestCombo + " 타");
        record(c, d, 530f, 262f, "소요 시간", sec + "초");
        record(c, d, 700f, 262f, "난이도", w.difficulty.name);

        record(c, d, 130f, 324f, "상대", w.enemy.name + " (" + w.enemy.title + ")");
        record(c, d, 530f, 324f, "사용 캐릭터", w.character.name);
        if (w.armyMode) {
            record(c, d, 130f, 356f - 0f, "생존 병사",
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
        d.text(c, value, x, y + 24f, 18f, Theme.TEXT, Draw.LEFT, true);
    }

    // ==================================================================
    //  엔딩 — 영화 《오디세이》로 연결
    // ==================================================================
    public static void drawFinale(Canvas c, Draw d, float time) {
        Backdrop.draw(c, d, Backdrop.SEA, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x99070B12);

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
        d.text(c, StoryData.DISCLAIMER, 480f, 508f, 11f, 0x88A8B4BE, Draw.CENTER, false);
    }

    private static String fmt(float v) {
        int hundredths = Math.round(v * 100f);
        return (hundredths / 100) + "." + pad(Math.abs(hundredths % 100));
    }

    private static String pad(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }
}
