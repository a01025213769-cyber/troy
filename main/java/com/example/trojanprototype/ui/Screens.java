package com.example.trojanprototype.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.data.ChapterData;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.SaveManager;
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
    // 타이틀 — 큰 시작 카드 / 작은 카드 3장(이어하기·챕터 선택·조작 방법) / 난이도 칩 3개
    public static final float[][] TITLE_DIFF = {
            {60f, 376f, 204f, 416f},
            {218f, 376f, 362f, 416f},
            {376f, 376f, 520f, 416f}
    };
    public static final float[] TITLE_START = {60f, 190f, 520f, 268f};
    public static final float[] TITLE_CONTINUE = {60f, 280f, 204f, 338f};
    public static final float[] TITLE_CHAPTERS = {218f, 280f, 362f, 338f};
    public static final float[] TITLE_HOWTO = {376f, 280f, 520f, 338f};

    /** 상단 바 오른쪽의 미니게임(2인 대전) 입구 */
    public static final float[] TITLE_MINIGAME = {690f, 9f, 910f, 47f};

    public static final float[] STORY_SKIP = {812f, 22f, 938f, 66f};
    public static final float[] STORY_SAVE = {676f, 22f, 802f, 66f};

    // 결과 화면 — 타이틀로 / 저장하기 / 다음(다시 도전)
    public static final float[] RESULT_PRIMARY = {570f, 414f, 830f, 476f};
    public static final float[] RESULT_SAVE = {350f, 414f, 550f, 476f};
    public static final float[] RESULT_SECONDARY = {130f, 414f, 330f, 476f};

    // 저장 슬롯 화면
    public static final float[][] SLOT_ROWS = {
            {80f, 118f, 880f, 210f},
            {80f, 222f, 880f, 314f},
            {80f, 326f, 880f, 418f}
    };
    /** 슬롯 줄 오른쪽 끝의 삭제 버튼 (슬롯 번호 i 의 줄 안에 있습니다) */
    public static float[] slotDelete(int i) {
        float[] r = SLOT_ROWS[i];
        return new float[]{r[2] - 104f, r[1] + 30f, r[2] - 12f, r[1] + 62f};
    }
    public static final float[] SLOT_BACK = {80f, 440f, 240f, 486f};

    // 확인 창
    public static final float[] CONFIRM_YES = {290f, 296f, 470f, 344f};
    public static final float[] CONFIRM_NO = {490f, 296f, 670f, 344f};

    // 챕터 선택 화면 — 3열 2행 카드
    public static final float[][] CHAPTER_CARDS = {
            {60f, 112f, 330f, 276f}, {345f, 112f, 615f, 276f}, {630f, 112f, 900f, 276f},
            {60f, 288f, 330f, 452f}, {345f, 288f, 615f, 452f}, {630f, 288f, 900f, 452f}
    };
    public static final float[] CHAPTER_BACK = {60f, 468f, 200f, 512f};

    /** 슬롯 화면의 용도 */
    public static final int SLOT_MODE_LOAD = 0;
    public static final int SLOT_MODE_NEW = 1;
    public static final int SLOT_MODE_SAVE = 2;

    /** 확인 창 종류 */
    public static final int CONFIRM_NONE = 0;
    public static final int CONFIRM_OVERWRITE = 1;
    public static final int CONFIRM_DELETE = 2;

    /** 챕터 카드 머리띠 색 (Backdrop 번호 순서: 해안 / 성벽 / 야간 성문 / 올림포스 / 바다) */
    private static final int[] BACKDROP_TINT = {
            0xFF3A5560, 0xFF5E4C38, 0xFF55292A, 0xFF45375F, 0xFF24405F
    };

    // ==================================================================
    //  타이틀
    // ==================================================================
    public static void drawTitle(Canvas c, Draw d, Assets assets, float time, int diffIndex,
                                 int cleared, boolean hasSave) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        // 왼쪽 메뉴 영역을 어둡게 눌러 글자가 또렷하게 보이게 합니다.
        for (int i = 0; i < 8; i++) {
            d.rect(c, i * 80f, 0f, i * 80f + 81f, Theme.VH,
                    Draw.withAlpha(0xFF050302, 190 - i * 24));
        }

        // ---- 오른쪽: 마주 선 두 영웅 (왼쪽 메뉴와 균형을 맞춥니다)
        int idle = ((int) (time / 0.6f) % 2 == 0) ? SpriteSet.F_IDLE_A : SpriteSet.F_IDLE_B;
        d.oval(c, 560f, 466f, 760f, 484f, 0x88000000);
        d.oval(c, 770f, 466f, 950f, 484f, 0x88000000);
        heroSprite(c, d, assets, "achilles_sprite", 660f, 476f, 270f, false, idle);
        heroSprite(c, d, assets, "hector_sprite", 862f, 476f, 270f, true, idle);
        d.spaced(c, "그리스 · 아킬레우스", 660f, 112f, 12f, Theme.GREEK + 0x00201010,
                Draw.CENTER, true, 2f);
        d.spaced(c, "트로이 · 헥토르", 862f, 112f, 12f, Theme.TROY, Draw.CENTER, true, 2f);

        // ---- 상단 바
        d.rect(c, 0f, 0f, Theme.VW, 56f, 0xE60A0705);
        d.line(c, 0f, 56f, Theme.VW, 56f, 1.2f, 0x88B8914F);
        d.spaced(c, "트로이 전쟁", 60f, 37f, 24f, Theme.TEXT_GOLD, Draw.LEFT, true, 6f);

        // 미니게임 입구 — 2인 대전
        float[] mg = TITLE_MINIGAME;
        d.card(c, mg[0], mg[1], mg[2], mg[3], true);
        d.round(c, mg[0], mg[1], mg[2], mg[3], 3f,
                Draw.withAlpha(0xFFFFE2A8, (int) (10 + 10 * Math.sin(time * 3f))));
        d.text(c, "미니게임", mg[0] + 20f, mg[1] + 25f, 16f, Theme.TEXT_GOLD, Draw.LEFT, true);
        d.text(c, "2인 대전", mg[2] - 18f, mg[1] + 24f, 12f, Theme.TEXT_DIM, Draw.RIGHT, false);

        // ---- 로고 영역
        d.spaced(c, "트로이", 60f, 142f, 52f, Theme.TEXT, Draw.LEFT, true, 10f);
        d.line(c, 62f, 158f, 200f, 158f, 1.6f, 0xFFC9A55E);
        d.text(c, "얼굴, 전쟁, 남자, 생각 그리고 속임수", 62f, 178f, 14f,
                Theme.TEXT_GOLD, Draw.LEFT, false);

        // ---- 메인 카드: 이야기 시작 (슬롯을 고르고 프롤로그부터)
        float[] st = TITLE_START;
        d.card(c, st[0], st[1], st[2], st[3], true);
        float blink = 0.72f + 0.28f * (float) Math.sin(time * 3.4f);
        d.round(c, st[0], st[1], st[2], st[3], 3f, Draw.withAlpha(0xFFFFE2A8, (int) (14 * blink)));
        d.text(c, "새 이야기 시작", st[0] + 28f, st[1] + 38f, 28f, Theme.TEXT, Draw.LEFT, true);
        String sub = "트로이의 운명을 바꾸는 전쟁";
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            sub = GameData.DIFFICULTIES[diffIndex].note;
        }
        d.text(c, sub, st[0] + 28f, st[1] + 62f, 14f, Theme.TEXT_DIM, Draw.LEFT, false);
        d.chevron(c, st[2] - 30f, (st[1] + st[3]) * 0.5f, 8f, Theme.TEXT);

        // ---- 보조 카드 3장
        titleTile(c, d, TITLE_CONTINUE, "이어하기", hasSave ? "저장한 곳부터" : "저장 없음", hasSave);
        titleTile(c, d, TITLE_CHAPTERS, "챕터 선택",
                Math.min(cleared + 1, ChapterData.count()) + " / " + ChapterData.count() + " 열림", true);
        titleTile(c, d, TITLE_HOWTO, "조작 방법", "게임 방법", true);

        // ---- 난이도
        d.spaced(c, "난이도", 62f, 366f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
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
            d.text(c, "AI 판단 주기 " + fmt(df.reactionTime) + "초", 62f, 440f, 12f,
                    Theme.TEXT_DIM, Draw.LEFT, false);
        }

        // ---- 챕터 진행도 — 클리어한 챕터는 금색 점
        int total = ChapterData.count();
        for (int i = 0; i < total; i++) {
            float x = 70f + i * 28f;
            if (i < cleared) {
                d.circle(c, x, 464f, 7f, Theme.GOLD_EDGE);
            } else {
                d.circleStroke(c, x, 464f, 6f, 1.4f, 0x88B8914F);
            }
        }
        d.text(c, "챕터 진행 " + Math.min(cleared, total) + " / " + total, 520f, 468f, 12f,
                Theme.TEXT_DIM, Draw.RIGHT, false);

        // ---- 하단 장식선
        d.line(c, 60f, 492f, 900f, 492f, 1f, 0x55B8914F);
        d.rect(c, 476f, 488f, 484f, 496f, 0xFFC9A55E);
    }

    /** 타이틀의 작은 메뉴 카드 */
    private static void titleTile(Canvas c, Draw d, float[] r, String label, String sub,
                                  boolean enabled) {
        d.card(c, r[0], r[1], r[2], r[3], false);
        float cx = (r[0] + r[2]) * 0.5f;
        d.text(c, label, cx, r[1] + 26f, 17f, enabled ? Theme.TEXT : Theme.TEXT_DIM,
                Draw.CENTER, true);
        d.text(c, sub, cx, r[1] + 46f, 11f, Theme.TEXT_DIM, Draw.CENTER, false);
        if (!enabled) {
            d.rect(c, r[0], r[1], r[2], r[3], 0x88050302);
        }
    }

    /** 타이틀에 세우는 영웅 그림. flip 이면 왼쪽을 보게 뒤집습니다. */
    private static void heroSprite(Canvas c, Draw d, Assets assets, String name,
                                   float cx, float base, float bodyH, boolean flip, int frame) {
        if (assets == null) return;
        SpriteSet set = assets.spriteSet(name);
        Bitmap bmp = set.frame(frame);
        if (bmp == null) return;
        float scale = set.scaleFor(bodyH);
        float w = set.canvasW * scale;
        float h = set.canvasH * scale;
        int save = c.save();
        if (flip) c.scale(-1f, 1f, cx, 0f);
        d.bitmap(c, bmp, cx - set.anchorX * scale, base - h,
                cx - set.anchorX * scale + w, base, 255);
        c.restoreToCount(save);
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

        BattleRenderer.menuButton(c, d, STORY_SAVE, "저장", false);
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
        BattleRenderer.menuButton(c, d, RESULT_SAVE, "저장하기", false);
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

    // ==================================================================
    //  공통 — 상단 바 (저장 슬롯 / 챕터 선택 화면 제목줄)
    // ==================================================================
    static void topBar(Canvas c, Draw d, String title) {
        d.rect(c, 0f, 0f, Theme.VW, 56f, 0xE60A0705);
        d.line(c, 0f, 56f, Theme.VW, 56f, 1.2f, 0x88B8914F);
        d.spaced(c, title, 60f, 37f, 24f, Theme.TEXT_GOLD, Draw.LEFT, true, 6f);
    }

    // ==================================================================
    //  저장 슬롯 (이어하기 / 새 게임 / 저장하기 공용)
    // ==================================================================
    public static void drawSlots(Canvas c, Draw d, float time, int mode,
                                 SaveManager.Slot[] slots, int confirm, int confirmSlot) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xDD050302);

        String title = mode == SLOT_MODE_LOAD ? "이어하기"
                : mode == SLOT_MODE_NEW ? "새 이야기 시작" : "저장하기";
        String sub = mode == SLOT_MODE_LOAD ? "저장한 곳부터 이어서 플레이합니다"
                : mode == SLOT_MODE_NEW ? "이야기를 기록할 슬롯을 고르세요 (사용 중인 슬롯은 덮어씁니다)"
                : "지금 장면을 저장할 슬롯을 고르세요 (저장한 장면부터 다시 시작합니다)";
        topBar(c, d, title);
        d.text(c, sub, 80f, 94f, 13f, Theme.TEXT_DIM, Draw.LEFT, false);

        String hint = mode == SLOT_MODE_LOAD ? "탭하여 이어하기"
                : mode == SLOT_MODE_NEW ? "탭하여 여기에 시작" : "탭하여 여기에 저장";

        for (int i = 0; i < SaveManager.SLOT_COUNT; i++) {
            float[] r = SLOT_ROWS[i];
            SaveManager.Slot s = slots[i];
            boolean usable = s.used || mode != SLOT_MODE_LOAD;
            float cy = (r[1] + r[3]) * 0.5f;

            d.card(c, r[0], r[1], r[2], r[3], s.used);

            // 번호 상자
            d.panel(c, r[0] + 24f, cy - 28f, r[0] + 80f, cy + 28f, Theme.PANEL);
            d.text(c, String.valueOf(i + 1), r[0] + 52f, cy + 11f, 30f,
                    s.used ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.CENTER, true);

            if (s.used) {
                d.text(c, s.tag, r[0] + 104f, r[1] + 30f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true);
                d.text(c, s.title, r[0] + 104f, r[1] + 58f, 22f, Theme.TEXT, Draw.LEFT, true);
                d.text(c, s.where, r[0] + 104f, r[1] + 80f, 13f, Theme.TEXT_GOLD, Draw.LEFT, false);

                float rx = r[2] - 124f;
                d.text(c, s.date, rx, r[1] + 34f, 13f, Theme.TEXT_DIM, Draw.RIGHT, false);
                d.text(c, s.meta, rx, r[1] + 58f, 13f, Theme.TEXT, Draw.RIGHT, false);
                if (usable) {
                    float blink = 0.6f + 0.4f * (float) Math.sin(time * 3.4f + i);
                    d.text(c, hint, rx, r[1] + 80f, 13f,
                            Draw.withAlpha(Theme.TEXT_GOLD, (int) (255 * blink)), Draw.RIGHT, true);
                }
                float[] del = slotDelete(i);
                d.card(c, del[0], del[1], del[2], del[3], false);
                d.text(c, "삭제", (del[0] + del[2]) * 0.5f, (del[1] + del[3]) * 0.5f + 5f, 14f,
                        0xFFD9604C, Draw.CENTER, true);
            } else {
                d.text(c, "비어 있는 슬롯", r[0] + 104f, cy + 7f, 20f, Theme.TEXT_DIM, Draw.LEFT, true);
                if (usable) {
                    float blink = 0.6f + 0.4f * (float) Math.sin(time * 3.4f + i);
                    d.text(c, hint, r[2] - 28f, cy + 5f, 13f,
                            Draw.withAlpha(Theme.TEXT_GOLD, (int) (255 * blink)), Draw.RIGHT, true);
                }
            }
        }

        BattleRenderer.menuButton(c, d, SLOT_BACK, "← 뒤로", false);

        if (confirm != CONFIRM_NONE) {
            d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xB0050302);
            d.panel(c, 250f, 170f, 710f, 370f, Theme.PANEL_SOLID);
            String head = confirm == CONFIRM_DELETE ? "슬롯 " + (confirmSlot + 1) + " 을(를) 삭제할까요?"
                    : "슬롯 " + (confirmSlot + 1) + " 에 덮어쓸까요?";
            String body = confirm == CONFIRM_DELETE ? "삭제한 저장은 되돌릴 수 없습니다."
                    : "기존 저장 내용이 사라집니다.";
            d.text(c, head, 480f, 232f, 22f, Theme.TEXT, Draw.CENTER, true);
            d.text(c, body, 480f, 266f, 14f, Theme.TEXT_DIM, Draw.CENTER, false);
            BattleRenderer.menuButton(c, d, CONFIRM_YES,
                    confirm == CONFIRM_DELETE ? "삭제" : "덮어쓰기", true);
            BattleRenderer.menuButton(c, d, CONFIRM_NO, "취소", false);
        }
    }

    // ==================================================================
    //  챕터 선택 — 클리어한 다음 챕터까지 열립니다
    // ==================================================================
    public static void drawChapters(Canvas c, Draw d, float time, int cleared) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xDD050302);

        topBar(c, d, "챕터 선택");
        d.text(c, "클리어 " + Math.min(cleared, ChapterData.count()) + " / " + ChapterData.count(),
                900f, 37f, 18f, Theme.BRONZE_LIGHT, Draw.RIGHT, true);
        d.text(c, "이전 챕터를 클리어하면 다음 챕터가 열립니다 · 선택한 챕터부터 순서대로 이어집니다",
                60f, 94f, 13f, Theme.TEXT_DIM, Draw.LEFT, false);

        for (int i = 0; i < ChapterData.count() && i < CHAPTER_CARDS.length; i++) {
            drawChapterCard(c, d, time, i, cleared);
        }

        BattleRenderer.menuButton(c, d, CHAPTER_BACK, "← 뒤로", false);
        d.text(c, "챕터 선택으로 시작한 플레이는 저장 슬롯에 자동 기록되지 않습니다 — 일시정지 메뉴에서 저장하세요",
                220f, 495f, 11f, Theme.TEXT_DIM, Draw.LEFT, false);
    }

    private static void drawChapterCard(Canvas c, Draw d, float time, int i, int cleared) {
        float[] r = CHAPTER_CARDS[i];
        ChapterData.Entry e = ChapterData.LIST[i];
        boolean unlocked = ChapterData.isUnlocked(i, cleared);
        boolean done = i < cleared;
        boolean next = (i == cleared);
        float w = r[2] - r[0];
        int tint = BACKDROP_TINT[Math.max(0, Math.min(BACKDROP_TINT.length - 1, e.backdrop))];

        // 카드 바탕 + 위쪽 색 띠
        d.panel(c, r[0], r[1], r[2], r[3], Theme.PANEL);
        d.rect(c, r[0] + 2f, r[1] + 2f, r[2] - 2f, r[1] + 64f, Draw.withAlpha(tint, 215));
        d.line(c, r[0] + 2f, r[1] + 64f, r[2] - 2f, r[1] + 64f, 1f, 0x88B8914F);

        // 큰 번호 (장식)
        String num = i == 0 ? "P" : String.valueOf(i);
        d.text(c, num, r[2] - 18f, r[1] + 56f, 52f, Draw.withAlpha(0xFFFFFFFF, 40), Draw.RIGHT, true);

        d.spaced(c, e.tag, r[0] + 16f, r[1] + 26f, 11f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 2f);
        d.text(c, e.title, r[0] + 16f, r[1] + 51f, 19f, Theme.TEXT, Draw.LEFT, true);

        // 본문: "장소 · 대진" 두 줄로 나눠 표시
        String place = e.place;
        String line2 = "";
        int cut = place.indexOf(" · ");
        if (cut > 0) {
            line2 = place.substring(cut + 3);
            place = place.substring(0, cut);
        }
        d.text(c, place, r[0] + 16f, r[1] + 92f, 14f, Theme.TEXT, Draw.LEFT, false);
        d.text(c, line2, r[0] + 16f, r[1] + 114f, 12f, Theme.TEXT_DIM, Draw.LEFT, false);

        // 상태 표시
        float by = r[3] - 32f;
        if (done) {
            d.round(c, r[0] + 16f, by, r[0] + 96f, by + 22f, 3f, 0x33C9A55E);
            d.roundStroke(c, r[0] + 16f, by, r[0] + 96f, by + 22f, 3f, 1f, 0xAAC9A55E);
            d.text(c, "CLEAR", r[0] + 56f, by + 16f, 12f, Theme.TEXT_GOLD, Draw.CENTER, true);
            d.line(c, r[2] - 44f, r[3] - 20f, r[2] - 36f, r[3] - 12f, 3f, Theme.GOLD_EDGE);
            d.line(c, r[2] - 36f, r[3] - 12f, r[2] - 22f, r[3] - 28f, 3f, Theme.GOLD_EDGE);
        } else if (next) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(time * 4f);
            d.round(c, r[0] + 16f, by, r[0] + 96f, by + 22f, 3f, Theme.GOLD_EDGE);
            d.text(c, "NEXT ▶", r[0] + 56f, by + 16f, 12f, 0xFF1A120A, Draw.CENTER, true);
            d.roundStroke(c, r[0], r[1], r[2], r[3], 4f, 2.6f,
                    Draw.withAlpha(0xFFF2D49A, (int) (110 + 145 * pulse)));
        }

        // 잠긴 챕터 — 어둡게 덮고 자물쇠
        if (!unlocked) {
            d.round(c, r[0], r[1], r[2], r[3], 4f, 0xD0050302);
            float cx = r[0] + w * 0.5f;
            float cy = (r[1] + r[3]) * 0.5f - 6f;
            d.circleStroke(c, cx, cy - 12f, 11f, 4f, 0xFF8D8A82);
            d.round(c, cx - 19f, cy - 4f, cx + 19f, cy + 24f, 4f, 0xFF8D8A82);
            d.circle(c, cx, cy + 9f, 3.5f, 0xFF1A120A);
            d.text(c, "이전 챕터를 클리어하면 열립니다", cx, r[3] - 16f, 12f,
                    Theme.TEXT_DIM, Draw.CENTER, false);
        }
    }

    // ==================================================================
    //  짧은 알림 (저장 완료 등)
    // ==================================================================
    public static void drawToast(Canvas c, Draw d, String msg, float remain) {
        if (msg == null || remain <= 0f) return;
        float a = Draw.clamp(remain / 0.35f, 0f, 1f);
        float w = d.textWidth(msg, 15f, true) + 48f;
        float x0 = 480f - w * 0.5f;
        float y0 = 452f;
        d.round(c, x0, y0, x0 + w, y0 + 38f, 4f, Draw.withAlpha(0xFF110C09, (int) (240 * a)));
        d.roundStroke(c, x0, y0, x0 + w, y0 + 38f, 4f, 1.4f,
                Draw.withAlpha(0xFFB8914F, (int) (220 * a)));
        d.text(c, msg, 480f, y0 + 25f, 15f, Draw.withAlpha(Theme.TEXT, (int) (255 * a)),
                Draw.CENTER, true);
    }

    private static String fmt(float v) {
        int hundredths = Math.round(v * 100f);
        return (hundredths / 100) + "." + pad(Math.abs(hundredths % 100));
    }

    private static String pad(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }
}
