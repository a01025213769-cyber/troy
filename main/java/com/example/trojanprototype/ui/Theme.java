package com.example.trojanprototype.ui;

/**
 * 화면 전체의 색상과 레이아웃 상수.
 *
 * 담당: 심신 (콘텐츠 / 게임 연출)
 * - 색을 바꾸고 싶으면 이 파일만 고치면 됩니다. 다른 파일은 건드리지 않습니다.
 * - 좌표는 전부 960x540 "가상 화면" 기준입니다.
 *   실제 기기 해상도로는 Viewport 가 알아서 비율을 맞춰 늘려 줍니다.
 */
public final class Theme {

    private Theme() {}

    // ===================== 가상 화면 크기 =====================
    /** 가상 화면 가로 (모든 좌표의 기준) */
    public static final float VW = 960f;
    /** 가상 화면 세로 */
    public static final float VH = 540f;

    // ===================== 전투 무대 =====================
    /** 캐릭터가 서 있는 바닥선 */
    public static final float GROUND = 428f;
    /** 캐릭터가 갈 수 있는 왼쪽 끝 */
    public static final float ARENA_LEFT = 104f;
    /** 캐릭터가 갈 수 있는 오른쪽 끝 (오른쪽 버튼에 가리지 않도록 여유를 둠) */
    public static final float ARENA_RIGHT = 826f;

    // ===================== 기본 색 =====================
    public static final int BG_DEEP = 0xFF0B1119;   // 가장 어두운 배경
    public static final int PANEL = 0xF2141C27;     // 반투명 패널
    public static final int PANEL_SOLID = 0xFF141C27;
    public static final int PANEL_LINE = 0x33E7C98A; // 패널 테두리

    public static final int TEXT = 0xFFF2EDE3;       // 본문 글자
    public static final int TEXT_DIM = 0xFF9AA7B4;   // 보조 글자
    public static final int TEXT_GOLD = 0xFFEFC378;  // 강조 글자

    public static final int BRONZE = 0xFFC8893F;     // 청동
    public static final int BRONZE_LIGHT = 0xFFE9BE7B;
    public static final int GREEK = 0xFF4E8FB8;      // 그리스 진영
    public static final int TROY = 0xFFB4544A;       // 트로이 진영

    // ===================== 게이지 색 =====================
    public static final int HP_PLAYER = 0xFF5FBF84;
    public static final int HP_PLAYER_LOW = 0xFFD9A441;
    public static final int HP_ENEMY = 0xFFC4514A;
    public static final int HP_BACK = 0xFF2A3440;
    public static final int RAGE = 0xFFE8A33D;
    public static final int RAGE_FULL = 0xFFFFD96B;

    // ===================== 버튼 색 =====================
    public static final int BTN_FACE = 0xCC32414F;
    public static final int BTN_FACE_ON = 0xEE5C7384;
    public static final int BTN_EDGE = 0x88E9BE7B;
    public static final int BTN_ATTACK = 0xCCB0453C;
    public static final int BTN_ATTACK_ON = 0xEEE2695A;
    public static final int BTN_ULT_READY = 0xDDD9A441;
    public static final int BTN_DISABLED = 0x8C2A333D;

    // ===================== 전투 HUD 배치 =====================
    public static final float HUD_TOP = 22f;
    public static final float HP_BAR_W = 330f;
    public static final float HP_BAR_H = 20f;

    // ===================== 조작 버튼 배치 =====================
    // 왼손 (이동)
    // 두 버튼의 터치 영역(반지름 + 여유 10px)이 겹치지 않도록 충분히 띄웁니다.
    public static final float PAD_LEFT_X = 80f;
    public static final float PAD_RIGHT_X = 204f;
    public static final float PAD_Y = 462f;
    public static final float PAD_R = 46f;

    // 오른손 (행동) — 공격 버튼을 중심으로 한 다이아몬드 배치
    public static final float BTN_ATTACK_X = 874f;
    public static final float BTN_ATTACK_Y = 466f;
    public static final float BTN_ATTACK_R = 52f;

    public static final float BTN_GUARD_X = 760f;
    public static final float BTN_GUARD_Y = 492f;
    public static final float BTN_GUARD_R = 38f;

    public static final float BTN_DODGE_X = 748f;
    public static final float BTN_DODGE_Y = 388f;
    public static final float BTN_DODGE_R = 38f;

    public static final float BTN_ULT_X = 866f;
    public static final float BTN_ULT_Y = 348f;
    public static final float BTN_ULT_R = 44f;

    /** 공격 버튼을 이 시간(초) 안에 다시 누르면 연속 공격으로 이어집니다(3타째 = 강공격). */
    public static final float CHAIN_WINDOW = 0.55f;

    // 상단 가운데 작은 버튼 (일시정지 / AI 디버그 표시)
    public static final float BTN_PAUSE_X = 446f;
    public static final float BTN_PAUSE_Y = 110f;
    public static final float BTN_AI_X = 514f;
    public static final float BTN_AI_Y = 110f;
    public static final float BTN_SMALL_R = 22f;
}
