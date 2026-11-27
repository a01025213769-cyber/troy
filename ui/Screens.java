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
 *   난이도는 타이틀(홈) 화면의 설정 — 톱니바퀴 또는 '설정' 탭 — 에서 고릅니다.
 *
 * 버튼의 위치는 여기 선언된 float[] {왼쪽, 위, 오른쪽, 아래} 값을 씁니다.
 * GameEngine 이 같은 값으로 터치를 판정하므로, 버튼을 옮길 땐 여기만 고치면 됩니다.
 */
public final class Screens {

    private Screens() {}

    // ---------------- 버튼 위치 ----------------
    // 타이틀(홈) — 사진 시안(1536x1024)을 그대로 옮긴 배치입니다.
    //   시안 좌표 → 가상 화면:  x * 0.625,  (y - 40) * 0.625
    //   (배경 그림 title_bg 가 시안의 y 40~904 구간이라 이 식으로 맞춰집니다.)
    // 메뉴 카드 4장 — 배경 사진 위 같은 자리에 카드 그림을 얹습니다.
    /** 캠페인 시작 — 새 이야기 (슬롯 고르기 → 프롤로그) */
    public static final float[] TITLE_START = {159.375f, 138.75f, 531.25f, 253.75f};
    /** 멀티플레이 — 미니게임(2인 대전) */
    public static final float[] TITLE_MINIGAME = {159.375f, 259.375f, 531.25f, 355f};
    /** 영웅 — 챕터 선택 (챕터마다 만나는 영웅) */
    public static final float[] TITLE_CHAPTERS = {159.375f, 361.25f, 343.75f, 458.75f};
    /** 군대 관리 — 이어하기 (저장해 둔 군대 / 진행 불러오기) */
    public static final float[] TITLE_CONTINUE = {350.625f, 361.25f, 531.25f, 458.75f};

    /** 상단 바 높이 */
    public static final float TITLE_BAR_H = 46f;
    /** 상단 탭 — 홈 · 캠페인 · 멀티플레이 · 영웅 · 군대 · 도감 · 설정 */
    public static final String[] TITLE_TAB_NAMES = {"홈", "캠페인", "멀티플레이", "영웅", "군대", "도감", "설정"};
    /** 탭 글자 가운데 x (시안 그대로) */
    private static final float[] TITLE_TAB_CX = {240.6f, 304.4f, 383.75f, 457.5f, 515f, 572.5f, 628.75f};
    /** 탭 터치 영역의 경계 x (탭 i 는 [i] ~ [i+1]) */
    private static final float[] TITLE_TAB_EDGE = {212.5f, 272f, 344f, 421f, 486f, 544f, 600f, 660f};
    public static final int TAB_HOME = 0, TAB_CAMPAIGN = 1, TAB_MULTI = 2, TAB_HEROES = 3,
            TAB_ARMY = 4, TAB_CODEX = 5, TAB_SETTINGS = 6;

    /** 탭 i 의 터치 영역 */
    public static float[] titleTab(int i) {
        return new float[]{TITLE_TAB_EDGE[i], 0f, TITLE_TAB_EDGE[i + 1], TITLE_BAR_H};
    }

    /** 오른쪽 위 톱니바퀴 — 설정 */
    public static final float[] TITLE_GEAR = {910f, 2f, 954f, 44f};
    /** 주요 목표 패널 */
    public static final float[] TITLE_OBJ = {705.625f, 295f, 946.875f, 438.125f};
    /** 주요 목표 '더보기' — 챕터 선택 */
    public static final float[] TITLE_OBJ_MORE = {870f, 295f, 946.875f, 332f};
    /** 소식 카드 (새로운 전장이 열렸습니다) — 챕터 선택 */
    public static final float[] TITLE_NEWS = {690f, 442f, 944f, 513f};

    /**
     * 재화 표시 — 이 게임에는 아직 재화 시스템이 없어서 시안의 숫자를 그대로 보여 줍니다.
     * 월계관 숫자만 실제 클리어한 챕터 수를 씁니다.
     */
    public static final String TITLE_GOLD = "12,450";
    public static final String TITLE_SILVER = "320";

    // 설정 창 (톱니바퀴 / 설정 탭) — 난이도 칩 3개 · 조작 방법 · 닫기
    public static final float[] SETTINGS_PANEL = {270f, 120f, 690f, 420f};
    public static final float[][] TITLE_DIFF = {
            {302f, 214f, 414f, 262f},
            {424f, 214f, 536f, 262f},
            {546f, 214f, 658f, 262f}
    };
    public static final float[] SETTINGS_HOWTO = {302f, 336f, 474f, 384f};
    public static final float[] SETTINGS_CLOSE = {486f, 336f, 658f, 384f};

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
    //  타이틀 (홈 화면) — 사진 시안을 그대로 옮겼습니다
    // ==================================================================
    //  그림 파일 (res/drawable-nodpi, 스프라이트와 같은 폴더 / 같은 방식으로 이름으로 불러옴)
    //    title_bg             전체 배경 — 성벽 · 트로이 목마 · 병사들 · 왼쪽 스파르타 전사
    //    title_card_campaign  캠페인 시작 카드 사진     title_card_multi   멀티플레이 카드 사진
    //    title_card_heroes    영웅 카드 사진            title_card_army    군대 관리 카드 사진
    //    title_news           소식 카드(불타는 도시)     title_banner_*     진영 깃발
    //    title_icon_*         재화 · 설정 아이콘         title_ornament     하단 월계수 장식
    //    title_logo_helmet    로고 옆 투구 문장
    //  카드 사진에는 글자가 없고, 문구는 아래 코드가 직접 씁니다 (문구를 바꾸려면 여기만 고치면 됨).
    //  배경 그림이 없으면 예전 Canvas 배경(Backdrop)으로 대신 그립니다.

    private static final int T_CREAM = 0xFFEFDCB8;    // 카드 제목
    private static final int T_BODY = 0xFFD9CDB8;     // 카드 설명
    private static final int T_GOLD = 0xFFD9B26A;     // 금색 강조
    private static final int T_GOLD_DIM = 0xFFB2965F; // 흐린 금색 (영웅 · 설정 탭)
    private static final int T_LINE = 0x66B8914F;     // 얇은 청동 선
    private static final int T_FOOT = 0xFF7F6C51;     // 하단 문구

    public static void drawTitle(Canvas c, Draw d, Assets assets, float time, int diffIndex,
                                 int cleared, boolean hasSave) {
        Bitmap bg = assets == null ? null : assets.sprite("title_bg");
        hasTitleArt = (bg != null);
        if (bg != null) {
            d.photo(c, bg, 0f, 0f, Theme.VW, Theme.VH, 255);
        } else {
            Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        }

        // ---- 왼쪽 문구 (영문은 배경 그림에 들어 있고, 한글 두 줄만 씁니다)
        d.textFaceShadow(c, "영웅은 싸우고,", 79.4f, 347.5f, 10.5f, 0xFFE8DCC6, Draw.CENTER, Draw.SANS);
        d.textFaceShadow(c, "신들은 모든 것을 결정한다.", 79.4f, 363f, 10.5f, 0xFFE8DCC6, Draw.CENTER, Draw.SANS);

        // ---- 메뉴 카드 4장
        float glow = 0.5f + 0.5f * (float) Math.sin(time * 2.6f);
        menuCard(c, d, assets, "title_card_campaign", TITLE_START, "캠페인 시작",
                "트로이의 운명을 바꾸는 전쟁에 뛰어들어라.", 22.6f, 50.5f, 73.6f, 20f, glow);
        menuCard(c, d, assets, "title_card_multi", TITLE_MINIGAME, "멀티플레이",
                "전 세계의 지휘관들과 격돌하라.", 22.6f, 49.6f, 72.2f, 19.4f, -1f);
        menuCard(c, d, assets, "title_card_heroes", TITLE_CHAPTERS, "영웅",
                "전설적인 영웅들을 만나라.", 22.6f, 54.6f, 77.6f, 20.4f, -1f);
        menuCard(c, d, assets, "title_card_army", TITLE_CONTINUE, "군대 관리",
                "당신의 군대를 준비하라.", 22.6f, 54.6f, 77.6f, 20.4f, -1f);

        // ---- 주요 목표
        drawObjectives(c, d, cleared);

        // ---- 아래 띠: 두 진영 + 소식 카드 + 바닥글
        drawFactionBand(c, d, assets);
        drawNews(c, d, assets, time);
        drawFooter(c, d, assets);

        // ---- 상단 바 (맨 마지막 — 배경 그림의 윗부분을 덮습니다)
        drawTitleBar(c, d, assets, cleared);
    }

    /**
     * 사진 카드 한 장. 사진(글자 없음) 위에 제목 · 설명 · 화살표를 씁니다.
     * titleY / subY 는 카드 위쪽에서부터의 글자 기준선, chevronInset 은 오른쪽 아래 화살표 여백.
     * glow 가 0 이상이면 금테가 은은하게 숨 쉬듯 밝아집니다 (대표 버튼 강조).
     */
    private static void menuCard(Canvas c, Draw d, Assets assets, String image, float[] r,
                                 String title, String sub, float titleSize, float titleY,
                                 float subY, float chevronInset, float glow) {
        Bitmap bmp = assets == null ? null : assets.sprite(image);
        if (bmp != null) {
            d.photo(c, bmp, r[0], r[1], r[2], r[3], 255);
        } else {
            d.card(c, r[0], r[1], r[2], r[3], false);
        }
        // 사진에 이미 얇은 테두리가 있어서, 그 위에 같은 색 선을 한 번 더 둘러 또렷하게 합니다.
        d.roundStroke(c, r[0] + 0.5f, r[1] + 0.5f, r[2] - 0.5f, r[3] - 0.5f, 2f, 1f, 0x99C9A066);
        if (glow >= 0f) {
            d.roundStroke(c, r[0] + 0.5f, r[1] + 0.5f, r[2] - 0.5f, r[3] - 0.5f, 2f, 1.6f,
                    Draw.withAlpha(0xFFF0CF8A, (int) (60 + 110 * glow)));
        }

        float x = r[0] + 17.5f;
        d.textFaceShadow(c, title, x, r[1] + titleY, titleSize, T_CREAM, Draw.LEFT, Draw.SERIF_KR);
        d.textFaceShadow(c, sub, x + 0.6f, r[1] + subY, 10.4f, T_BODY, Draw.LEFT, Draw.SANS);
        d.chevron(c, r[2] - chevronInset, r[3] - chevronInset - 0.4f, 5f, 0xFFE9DCC4);
    }

    /** 주요 목표 패널 — 실제 진행도(클리어한 챕터 수)로 표시합니다. */
    private static void drawObjectives(Canvas c, Draw d, int cleared) {
        float[] p = TITLE_OBJ;
        // 패널 바탕은 배경 그림에 들어 있습니다 (글자만 지운 상태). 그림이 없을 때만 직접 그립니다.
        if (!hasTitleArt) {
            d.panel(c, p[0], p[1], p[2], p[3], 0xEE0E0B09);
        }
        d.roundStroke(c, p[0] + 0.5f, p[1] + 0.5f, p[2] - 0.5f, p[3] - 0.5f, 2f, 1f, 0x88B8914F);

        d.textFace(c, "주요 목표", p[0] + 13.8f, p[1] + 25.4f, 14.6f, 0xFFEDE1C9, Draw.LEFT, Draw.SERIF_KR);
        d.textFace(c, "더보기", p[2] - 17.5f, p[1] + 22.9f, 9.4f, 0xFFD8CBB4, Draw.RIGHT, Draw.SANS);
        d.chevron(c, p[2] - 10.5f, p[1] + 19.6f, 3.4f, 0xFFD8CBB4);
        d.line(c, p[0] + 4f, p[1] + 35f, p[2] - 4f, p[1] + 35f, 1f, 0x55B8914F);

        int total = ChapterData.count();
        int done = Math.min(cleared, total);
        String[] rows = {
                "트로이의 성벽에 도달하라.",
                total + "개의 챕터를 정복하라. (" + done + "/" + total + ")",
                "헥토르를 쓰러뜨려라."
        };
        // 0 = 아직, 0~1 = 진행 중(원호), 1 = 완료
        float[] state = {
                cleared >= 3 ? 1f : 0f,                 // 2장(성벽 아래)까지 클리어
                done >= total ? 1f : done / (float) total,
                cleared >= 4 ? 1f : 0f                  // 3장(헥토르) 클리어
        };
        float[] base = {56.25f, 88.75f, 121.9f};
        for (int i = 0; i < rows.length; i++) {
            float y = p[1] + base[i];
            float cx = p[0] + 20f;
            float cy = y - 4.1f;
            if (state[i] >= 1f) {
                d.circle(c, cx, cy, 5.6f, 0xFFC9A55E);
                d.line(c, cx - 2.6f, cy + 0.2f, cx - 0.6f, cy + 2.2f, 1.4f, 0xFF1A120A);
                d.line(c, cx - 0.6f, cy + 2.2f, cx + 2.9f, cy - 1.9f, 1.4f, 0xFF1A120A);
            } else {
                d.circleStroke(c, cx, cy, 5.4f, 1.1f, 0xAAD8CBB4);
                if (state[i] > 0f) {
                    d.arc(c, cx, cy, 5.4f, 360f * state[i], 1.6f, 0xFFE0B96C);
                }
            }
            d.textFace(c, rows[i], p[0] + 33.8f, y, 10.4f,
                    state[i] >= 1f ? 0xFFB9AC94 : 0xFFE3D8C4, Draw.LEFT, Draw.SANS);
            if (i < rows.length - 1) {
                d.line(c, p[0] + 9f, y + 12.5f, p[2] - 9f, y + 12.5f, 1f, 0x33B8914F);
            }
        }
    }

    /** 배경 그림을 불러왔는지 (drawTitle 이 매 프레임 갱신) */
    private static boolean hasTitleArt = true;

    /** 아래 띠 — 아카이아 연합 / 트로이 */
    private static void drawFactionBand(Canvas c, Draw d, Assets assets) {
        float top = 465f, bottom = 520f;
        d.gradRect(c, 0f, top - 10f, Theme.VW, top, 0x00080605, 0x99080605, true);
        d.gradRect(c, 0f, top, Theme.VW, bottom, 0xFA0C0A08, 0xFF090706, true);
        d.line(c, 0f, top, Theme.VW, top, 1f, 0x55B8914F);

        Bitmap greek = assets == null ? null : assets.sprite("title_banner_achaea");
        Bitmap troy = assets == null ? null : assets.sprite("title_banner_troy");
        // 깃발은 띠 위쪽 선에 걸리도록 살짝 위로 올립니다 (시안처럼 깃대가 선 위에 걸침).
        if (greek != null) d.photo(c, greek, 15f, 459f, 15f + 43.4f, 459f + 55f, 255);
        if (troy != null) d.photo(c, troy, 332f, 459f, 332f + 45f, 459f + 55f, 255);

        d.textFace(c, "아카이아 연합", 92f, 487.5f, 14.4f, 0xFFEDE1C9, Draw.LEFT, Draw.SANS);
        d.textFace(c, "하나의 바다, 하나의 꿈.", 92.4f, 505f, 10f, 0xFFB9AC94, Draw.LEFT, Draw.SANS);

        d.line(c, 292.5f, 480f, 292.5f, 500f, 1f, 0x99D8CBB4);

        d.textFace(c, "트로이", 409f, 487.5f, 14.4f, 0xFFD8CBB4, Draw.LEFT, Draw.SANS);
        d.textFace(c, "우리는 끝까지 싸운다.", 409.4f, 505f, 10f, 0xFFB9AC94, Draw.LEFT, Draw.SANS);
    }

    /** 오른쪽 아래 소식 카드 — 새로운 전장이 열렸습니다 */
    private static void drawNews(Canvas c, Draw d, Assets assets, float time) {
        float[] r = TITLE_NEWS;
        Bitmap news = assets == null ? null : assets.sprite("title_news");
        d.rect(c, r[0], r[1], r[2], r[3], 0xFF0C0907);
        if (news != null) {
            // 시안 카드보다 납작하므로 아래(글자 자리)를 기준으로 잘라 씁니다.
            d.photoCover(c, news, r[0], r[1], r[2], r[3], 0.72f, 255);
        }
        d.roundStroke(c, r[0] + 0.5f, r[1] + 0.5f, r[2] - 0.5f, r[3] - 0.5f, 2f, 1f, 0x99C9A066);
        d.textFaceShadow(c, "새로운 전장이 열렸습니다.", r[0] + 10.5f, r[3] - 18f, 11f,
                0xFFF1E6D2, Draw.LEFT, Draw.SANS_BOLD);
        d.textFace(c, "일리온의 성벽 너머, 마지막 전투가 다가옵니다.", r[0] + 10.8f, r[3] - 6f, 7.8f,
                0xFFCDBFA6, Draw.LEFT, Draw.SANS);

        // 넘김 점 3개 — 천천히 돌아가며 밝아집니다 (시안의 배너 넘김 표시)
        int on = ((int) (time / 3f)) % 3;
        float cx = (r[0] + r[2]) * 0.5f;
        for (int i = 0; i < 3; i++) {
            float x = cx - 9.4f + i * 9.4f;
            if (i == on) d.circle(c, x, 516.4f, 1.8f, 0xFFD9B26A);
            else d.circleStroke(c, x, 516.4f, 1.5f, 0.9f, 0x99B9AC94);
        }
    }

    /** 맨 아래 바닥글 — VIRTVS · HONOR · AETERNA */
    private static void drawFooter(Canvas c, Draw d, Assets assets) {
        float top = 520f;
        d.rect(c, 0f, top, Theme.VW, Theme.VH, 0xFF080605);
        d.line(c, 0f, top, Theme.VW, top, 1f, 0x44B8914F);
        d.spacedFace(c, "VIRTVS  ·  HONOR  ·  AETERNA", 27f, 534f, 9.2f, T_FOOT, Draw.LEFT,
                Draw.CINZEL, 3.4f);
        d.line(c, 276f, 530.5f, 466f, 530.5f, 0.8f, 0x66B8914F);
        d.line(c, 504f, 530.5f, 767f, 530.5f, 0.8f, 0x66B8914F);
        Bitmap orn = assets == null ? null : assets.sprite("title_ornament");
        if (orn != null) d.photo(c, orn, 466f, 523.5f, 504f, 541.6f, 255);
        d.textFace(c, "신화는, 아직 끝나지 않았다.", 944f, 534.5f, 11.6f, T_FOOT, Draw.RIGHT, Draw.SANS);
    }

    /** 상단 바 — 로고 · 탭 · 재화 · 설정 */
    private static void drawTitleBar(Canvas c, Draw d, Assets assets, int cleared) {
        float h = TITLE_BAR_H;
        d.gradRect(c, 0f, 0f, Theme.VW, h, 0xFF0E0B09, 0xFF090706, true);
        d.line(c, 0f, h, Theme.VW, h, 1f, 0x77B8914F);
        d.gradRect(c, 0f, h, Theme.VW, h + 6f, 0x66000000, 0x00000000, true);

        // 로고 — 투구 문장(배경 그림 속 전사의 투구) + TROY
        Bitmap helmet = assets == null ? null : assets.sprite("title_logo_helmet");
        if (helmet != null) d.photo(c, helmet, 18f, 5f, 70f, 41f, 255);
        else d.round(c, 18f, 5f, 70f, 41f, 4f, 0xFF15100C);
        d.roundStroke(c, 18.5f, 5.5f, 69.5f, 40.5f, 4f, 1f, 0xAAB8914F);
        d.spacedFace(c, "TROY", 84f, 28.4f, 25f, 0xFFD9B26A, Draw.LEFT, Draw.CINZEL_BOLD, 3.2f);
        d.spacedFace(c, "GODS. MEN. DESTINY.", 85.4f, 38.6f, 6.2f, 0xFF9C8A6A, Draw.LEFT,
                Draw.CINZEL, 0.9f);

        // 탭 — 홈이 선택된 상태
        d.gradRect(c, TITLE_TAB_EDGE[0], 0f, TITLE_TAB_EDGE[1] - 2f, h, 0x10E0B96C, 0x40C99A55, true);
        d.line(c, TITLE_TAB_EDGE[0], 0f, TITLE_TAB_EDGE[0], h, 1f, 0x55B8914F);
        d.line(c, TITLE_TAB_EDGE[1] - 2f, 0f, TITLE_TAB_EDGE[1] - 2f, h, 1f, 0x55B8914F);
        d.line(c, TITLE_TAB_EDGE[0], h - 0.8f, TITLE_TAB_EDGE[1] - 2f, h - 0.8f, 1.6f, 0xFFC9A55E);
        d.circle(c, TITLE_TAB_CX[0], h + 1.4f, 1.8f, 0xFFE0B96C);
        for (int i = 0; i < TITLE_TAB_NAMES.length; i++) {
            int col = i == TAB_HOME ? 0xFFF5EAD5
                    : (i == TAB_HEROES || i == TAB_SETTINGS) ? T_GOLD_DIM : 0xFFDCD1BE;
            d.textFace(c, TITLE_TAB_NAMES[i], TITLE_TAB_CX[i], 28.6f, 13.8f, col, Draw.CENTER, Draw.SANS);
        }

        // 재화
        Bitmap coin = assets == null ? null : assets.sprite("title_icon_coin");
        Bitmap silver = assets == null ? null : assets.sprite("title_icon_silver");
        Bitmap laurel = assets == null ? null : assets.sprite("title_icon_laurel");
        Bitmap gear = assets == null ? null : assets.sprite("title_icon_gear");
        if (coin != null) d.photo(c, coin, 718f, 12f, 741f, 34.5f, 255);
        d.textFace(c, TITLE_GOLD, 743.5f, 28.4f, 12.6f, 0xFFEDE3CF, Draw.LEFT, Draw.SANS);
        if (silver != null) d.photo(c, silver, 793f, 12f, 816f, 34.5f, 255);
        d.textFace(c, TITLE_SILVER, 818.5f, 28.4f, 12.6f, 0xFFCFC7B8, Draw.LEFT, Draw.SANS);
        if (laurel != null) d.photo(c, laurel, 856.5f, 12f, 880.5f, 34.5f, 255);
        d.textFace(c, String.valueOf(cleared), 883f, 28.4f, 12.6f, 0xFFCFC7B8, Draw.LEFT, Draw.SANS);
        if (gear != null) {
            d.photo(c, gear, 915.5f, 7.4f, 947.5f, 38.8f, 255);
        } else {
            d.circleStroke(c, 931.5f, 23f, 14f, 1f, 0x99B8914F);
        }
    }

    // ==================================================================
    //  설정 창 (톱니바퀴 / 설정 탭) — 난이도 선택
    // ==================================================================
    public static void drawSettings(Canvas c, Draw d, int diffIndex) {
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xB0050302);
        float[] p = SETTINGS_PANEL;
        d.panel(c, p[0], p[1], p[2], p[3], 0xF60E0B09);
        d.textFace(c, "설정", (p[0] + p[2]) * 0.5f, p[1] + 44f, 24f, 0xFFEDE1C9, Draw.CENTER, Draw.SERIF_KR);
        d.line(c, p[0] + 30f, p[1] + 60f, p[2] - 30f, p[1] + 60f, 1f, T_LINE);

        d.spaced(c, "난이도", p[0] + 32f, p[1] + 84f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
        for (int i = 0; i < TITLE_DIFF.length && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = TITLE_DIFF[i];
            boolean on = (i == diffIndex);
            d.card(c, r[0], r[1], r[2], r[3], on);
            if (on) d.round(c, r[0], r[1], r[2], r[3], 3f, 0x33C9A55E);
            d.text(c, GameData.DIFFICULTIES[i].name, (r[0] + r[2]) * 0.5f + (on ? 2f : 0f),
                    (r[1] + r[3]) * 0.5f + 6f, 17f, on ? Theme.TEXT_GOLD : Theme.TEXT_DIM,
                    Draw.CENTER, true);
        }
        if (diffIndex >= 0 && diffIndex < GameData.DIFFICULTIES.length) {
            GameData.Difficulty df = GameData.DIFFICULTIES[diffIndex];
            d.text(c, df.note, (p[0] + p[2]) * 0.5f, p[1] + 172f, 13f, Theme.TEXT, Draw.CENTER, false);
            d.text(c, "AI 판단 주기 " + fmt(df.reactionTime) + "초", (p[0] + p[2]) * 0.5f, p[1] + 194f,
                    12f, Theme.TEXT_DIM, Draw.CENTER, false);
        }
        BattleRenderer.menuButton(c, d, SETTINGS_HOWTO, "조작 방법", false);
        BattleRenderer.menuButton(c, d, SETTINGS_CLOSE, "닫기", true);
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
