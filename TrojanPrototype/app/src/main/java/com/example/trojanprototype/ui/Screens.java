package com.example.trojanprototype.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.StoryData;
import com.example.trojanprototype.util.Assets;
import com.example.trojanprototype.util.Draw;

/**
 * 전투를 뺀 나머지 화면 그리기
 * (타이틀 · 컷신 · 캐릭터/난이도 선택 · 결과 · 엔딩)
 *
 * 담당: 심신 (화면 구성 / 문구) + 유장원 (상태 연결)
 *
 * 버튼의 위치는 여기 선언된 float[] {왼쪽, 위, 오른쪽, 아래} 값을 씁니다.
 * GameEngine 이 같은 값으로 터치를 판정하므로, 버튼을 옮길 땐 여기만 고치면 됩니다.
 */
public final class Screens {

    private Screens() {}

    // ---------------- 버튼 위치 ----------------
    public static final float[] TITLE_START = {330f, 352f, 630f, 412f};
    public static final float[] TITLE_HOWTO = {330f, 424f, 630f, 468f};

    public static final float[] STORY_SKIP = {812f, 22f, 938f, 66f};

    public static final float[] SELECT_CHAR_A = {58f, 92f, 458f, 344f};
    public static final float[] SELECT_CHAR_B = {502f, 92f, 902f, 344f};
    public static final float[][] SELECT_DIFF = {
            {284f, 362f, 404f, 404f},
            {420f, 362f, 540f, 404f},
            {556f, 362f, 676f, 404f}
    };
    public static final float[] SELECT_START = {330f, 420f, 630f, 482f};

    public static final float[] RESULT_PRIMARY = {500f, 414f, 800f, 476f};
    public static final float[] RESULT_SECONDARY = {160f, 414f, 460f, 476f};

    // ==================================================================
    //  타이틀
    // ==================================================================
    public static void drawTitle(Canvas c, Draw d, float time) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x99070B12);
        // 제목이 놓이는 구간을 가로 전체로 한 번 더 어둡게 덮어
        // 뒤에 있는 성문의 세로 경계가 글자를 가로지르지 않게 합니다.
        d.rect(c, 0f, 62f, Theme.VW, 296f, 0x77070B12);

        d.text(c, "ANDROID · JAVA · 2D FIGHTING", 480f, 96f, 13f, Theme.BRONZE_LIGHT,
                Draw.CENTER, true);
        d.textOutlined(c, "트로이 전쟁", 480f, 178f, 62f, Theme.TEXT, Draw.CENTER, true);
        d.text(c, "얼굴, 전쟁, 남자, 생각 그리고 속임수", 480f, 216f, 18f,
                Theme.TEXT_GOLD, Draw.CENTER, false);
        d.line(c, 360f, 240f, 600f, 240f, 1.5f, 0x55E9BE7B);
        d.text(c, "영화 《오디세이》의 배경이 된 전쟁을 플레이로 따라갑니다", 480f, 268f, 14f,
                Theme.TEXT_DIM, Draw.CENTER, false);

        // 깜빡이는 시작 버튼
        float blink = 0.72f + 0.28f * (float) Math.sin(time * 3.4f);
        BattleRenderer.menuButton(c, d, TITLE_START, "이야기 시작", true);
        d.round(c, TITLE_START[0], TITLE_START[1], TITLE_START[2], TITLE_START[3], 10f,
                Draw.withAlpha(0xFFFFFFFF, (int) (22 * blink)));
        BattleRenderer.menuButton(c, d, TITLE_HOWTO, "조작 방법", false);

        d.text(c, StoryData.DISCLAIMER, 480f, 508f, 11f, 0x99A8B4BE, Draw.CENTER, false);
        d.text(c, "10조 · 심신 · 유장원", 480f, 490f, 11f, Theme.TEXT_DIM, Draw.CENTER, false);
    }

    /** 조작 방법 안내 (타이틀 위에 겹쳐 띄웁니다) */
    public static void drawHowTo(Canvas c, Draw d) {
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xE6070B12);
        d.round(c, 120f, 70f, 840f, 470f, 14f, Theme.PANEL);
        d.roundStroke(c, 120f, 70f, 840f, 470f, 14f, 1.5f, Theme.PANEL_LINE);

        d.text(c, "조작 방법", 480f, 118f, 26f, Theme.TEXT_GOLD, Draw.CENTER, true);

        String[][] rows = {
                {"◀ ▶", "좌우 이동 — 누르고 있으면 계속 걷습니다"},
                {"공격", "탭해서 공격. 빠르게 연타하면 3타째에 강공격이 나갑니다"},
                {"방어", "누르고 있는 동안 피해가 75% 줄어듭니다 (강공격은 막히지 않음)"},
                {"회피", "짧게 무적이 됩니다. 적의 공격 직전에 쓰세요"},
                {"필살기", "분노가 100%가 되면 사용할 수 있습니다"},
                {"AI", "적 AI 의 판단 과정을 화면에 표시합니다 (발표 시연용)"}
        };
        for (int i = 0; i < rows.length; i++) {
            float y = 164f + i * 44f;
            d.round(c, 160f, y - 20f, 268f, y + 12f, 8f, 0x55313E4C);
            d.text(c, rows[i][0], 214f, y + 3f, 16f, Theme.TEXT_GOLD, Draw.CENTER, true);
            d.text(c, rows[i][1], 288f, y + 3f, 14f, Theme.TEXT, Draw.LEFT, false);
        }
        d.text(c, "화면을 탭하면 닫힙니다", 480f, 442f, 13f, Theme.TEXT_DIM, Draw.CENTER, false);
    }

    // ==================================================================
    //  컷신 (이야기 장면)
    // ==================================================================
    public static void drawStory(Canvas c, Draw d, StoryData.Chapter ch,
                                 int shownLines, float lineTime, float time) {
        Backdrop.draw(c, d, ch.backdrop, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0x88070B12);

        // 글상자 (배경 그림이 살짝 비치도록 완전히 불투명하게 하지 않습니다)
        d.round(c, 70f, 96f, 890f, 420f, 14f, 0xD60E141E);
        d.roundStroke(c, 70f, 96f, 890f, 420f, 14f, 1.5f, Theme.PANEL_LINE);

        d.text(c, ch.tag, 108f, 142f, 14f, Theme.BRONZE_LIGHT, Draw.LEFT, true);
        d.textOutlined(c, ch.title, 108f, 184f, 32f, Theme.TEXT, Draw.LEFT, true);
        d.line(c, 108f, 202f, 300f, 202f, 1.5f, 0x55E9BE7B);

        for (int i = 0; i < ch.lines.length && i < shownLines; i++) {
            float a = (i == shownLines - 1) ? Draw.smooth(Draw.clamp(lineTime / 0.35f, 0f, 1f)) : 1f;
            float slide = (1f - a) * 14f;
            d.text(c, ch.lines[i], 108f + slide, 240f + i * 34f, 17f,
                    Draw.withAlpha(Theme.TEXT, (int) (235 * a)), Draw.LEFT, false);
        }

        // 다음 안내
        if (shownLines >= ch.lines.length) {
            float blink = 0.5f + 0.5f * (float) Math.sin(time * 4f);
            d.text(c, "화면을 탭하여 계속  ▶", 850f, 396f, 14f,
                    Draw.withAlpha(Theme.TEXT_GOLD, (int) (120 + 135 * blink)), Draw.RIGHT, true);
        } else {
            d.text(c, "화면을 탭하면 다음 문장", 850f, 396f, 13f, Theme.TEXT_DIM, Draw.RIGHT, false);
        }

        BattleRenderer.menuButton(c, d, STORY_SKIP, "건너뛰기", false);
        d.text(c, StoryData.DISCLAIMER, 480f, 448f, 11f, 0x88A8B4BE, Draw.CENTER, false);
    }

    // ==================================================================
    //  캐릭터 / 난이도 선택
    // ==================================================================
    public static void drawSelect(Canvas c, Draw d, Assets assets,
                                  int charIndex, int diffIndex, float time) {
        Backdrop.draw(c, d, Backdrop.WALL, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xB3070B12);

        d.text(c, "전사를 선택하라", 480f, 58f, 24f, Theme.TEXT, Draw.CENTER, true);
        d.text(c, "같은 전쟁, 다른 싸움 방식", 480f, 80f, 12f, Theme.TEXT_DIM, Draw.CENTER, false);

        drawCharCard(c, d, assets, SELECT_CHAR_A, GameData.PLAYABLE[0], charIndex == 0, time);
        drawCharCard(c, d, assets, SELECT_CHAR_B, GameData.PLAYABLE[1], charIndex == 1, time);

        // 난이도
        d.text(c, "난이도", 234f, 390f, 14f, Theme.TEXT_DIM, Draw.RIGHT, true);
        for (int i = 0; i < SELECT_DIFF.length && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = SELECT_DIFF[i];
            boolean on = (i == diffIndex);
            d.round(c, r[0], r[1], r[2], r[3], 9f, on ? Theme.BRONZE : 0xB3222D38);
            d.roundStroke(c, r[0], r[1], r[2], r[3], 9f, 1.5f,
                    on ? 0xFFF2D49A : Theme.PANEL_LINE);
            d.text(c, GameData.DIFFICULTIES[i].name, (r[0] + r[2]) * 0.5f, (r[1] + r[3]) * 0.5f + 6f,
                    17f, on ? 0xFF231A10 : Theme.TEXT, Draw.CENTER, true);
        }
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            GameData.Difficulty df = GameData.DIFFICULTIES[diffIndex];
            d.text(c, df.note + " · AI 판단 주기 " + fmt(df.reactionTime) + "초",
                    700f, 390f, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        }

        BattleRenderer.menuButton(c, d, SELECT_START, "전장으로", true);
    }

    private static void drawCharCard(Canvas c, Draw d, Assets assets, float[] r,
                                     GameData.CharacterDef ch, boolean selected, float time) {
        d.round(c, r[0], r[1], r[2], r[3], 14f, selected ? 0xE61A2534 : 0xCC121A24);
        d.roundStroke(c, r[0], r[1], r[2], r[3], 14f, selected ? 2.5f : 1.2f,
                selected ? ch.accent : Theme.PANEL_LINE);

        // 스프라이트
        Bitmap bmp = assets.sprite(ch.spriteName);
        float h = 168f;
        float ratio = (bmp == null || bmp.getHeight() == 0)
                ? 0.68f : (float) bmp.getWidth() / (float) bmp.getHeight();
        float w = h * ratio;
        float cx = r[0] + 84f;
        float base = r[3] - 36f;
        float bob = selected ? (float) Math.sin(time * 2.6f) * 4f : 0f;
        d.oval(c, cx - w * 0.5f, base - 6f, cx + w * 0.5f, base + 8f, 0x55000000);
        if (bmp != null) {
            d.bitmap(c, bmp, cx - w * 0.5f, base - h - bob, cx + w * 0.5f, base - bob, 255);
        } else {
            d.round(c, cx - w * 0.3f, base - h, cx + w * 0.3f, base, 10f, ch.accent);
        }

        // 글
        float tx = r[0] + 172f;
        d.text(c, ch.name, tx, r[1] + 48f, 25f, selected ? Theme.TEXT : Theme.TEXT_DIM,
                Draw.LEFT, true);
        d.text(c, ch.title, tx, r[1] + 70f, 12f, ch.accent, Draw.LEFT, false);

        for (int i = 0; i < ch.traits.length; i++) {
            d.text(c, "· " + ch.traits[i], tx, r[1] + 98f + i * 20f, 13f,
                    Theme.TEXT, Draw.LEFT, false);
        }

        // 간단 능력치 막대
        statBar(c, d, tx, r[1] + 178f, "체력", ch.maxHp / 140f, Theme.HP_PLAYER);
        statBar(c, d, tx, r[1] + 198f, "공격", ch.attack / 1.2f, Theme.TROY);
        statBar(c, d, tx, r[1] + 218f, "기동", ch.dodgeSpeed / 420f, Theme.GREEK);

        if (selected) {
            d.round(c, r[2] - 86f, r[1] + 14f, r[2] - 16f, r[1] + 40f, 8f, ch.accent);
            d.text(c, "선택됨", r[2] - 51f, r[1] + 32f, 12f, 0xFF1A1208, Draw.CENTER, true);
        }
    }

    private static void statBar(Canvas c, Draw d, float x, float y, String label,
                                float ratio, int color) {
        d.text(c, label, x, y + 8f, 11f, Theme.TEXT_DIM, Draw.LEFT, false);
        d.gauge(c, x + 34f, y, x + 184f, y + 9f, ratio, 0x66263240, color, 4f);
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
