package com.example.trojanprototype.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.core.VersusSession;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.net.LanDiscovery;
import com.example.trojanprototype.net.Proto;
import com.example.trojanprototype.util.Assets;
import com.example.trojanprototype.util.Draw;
import com.example.trojanprototype.util.SpriteSet;

import java.util.List;

/**
 * 미니게임(2인 대전) 화면 그리기 — 허브 · 방 찾기 · 방(모드/캐릭터 선택) · 결과.
 *
 * 담당: 심신 (화면 구성 / 문구) + 유장원 (상태 연결)
 *
 * 버튼 위치는 Screens 와 같은 방식(float[] {왼쪽, 위, 오른쪽, 아래})이고,
 * GameEngine 이 같은 값으로 터치를 판정합니다.
 *
 *  허브   : 방 만들기 / 방 참가하기
 *  방 찾기: 같은 Wi-Fi 의 방 목록 + 방장 IP 직접 입력(숫자 패드)
 *  방     : [모드 선택 — 방장만] [P1 캐릭터] [P2 캐릭터] [시작 / 준비]
 */
public final class MiniScreens {

    private MiniScreens() {}

    // ---------------- 허브 ----------------
    public static final float[] HUB_HOST = {110f, 130f, 470f, 330f};
    public static final float[] HUB_JOIN = {490f, 130f, 850f, 330f};
    public static final float[] HUB_BACK = {60f, 448f, 220f, 494f};

    // ---------------- 방 찾기 ----------------
    public static final float[][] JOIN_ROOMS = {
            {60f, 128f, 520f, 188f}, {60f, 196f, 520f, 256f},
            {60f, 264f, 520f, 324f}, {60f, 332f, 520f, 392f}
    };
    public static final float[] JOIN_FIELD = {560f, 128f, 880f, 172f};
    public static final float[] JOIN_GO = {560f, 400f, 880f, 446f};
    public static final float[] JOIN_BACK = {60f, 448f, 220f, 494f};
    /** 숫자 패드: 1~9, '.', 0, 지우기 (순서대로 12칸) */
    public static final String[] KEY_LABELS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<"};

    public static float[] joinKey(int i) {
        int col = i % 3, row = i / 3;
        float x = 560f + col * 110f;
        float y = 184f + row * 54f;
        return new float[]{x, y, x + 100f, y + 44f};
    }

    // ---------------- 방 ----------------
    public static final float[][] ROOM_MODE = {
            {40f, 92f, 470f, 172f}, {490f, 92f, 920f, 172f}
    };
    public static final float[] ROOM_P1 = {40f, 190f, 470f, 400f};
    public static final float[] ROOM_P2 = {490f, 190f, 920f, 400f};
    public static final float[] ROOM_BACK = {40f, 436f, 200f, 490f};
    public static final float[] ROOM_ACTION = {620f, 436f, 920f, 490f};

    /** 패널 안쪽 ◀ ▶ (자기 패널에서만 눌립니다) */
    public static float[] arrowLeft(float[] panel) {
        return new float[]{panel[0] + 6f, 262f, panel[0] + 50f, 336f};
    }

    public static float[] arrowRight(float[] panel) {
        return new float[]{panel[2] - 50f, 262f, panel[2] - 6f, 336f};
    }

    // ---------------- 결과 ----------------
    public static final float[] RES_PRIMARY = {570f, 414f, 830f, 476f};
    public static final float[] RES_EXIT = {350f, 414f, 550f, 476f};

    public static final String[] MODE_NAMES = {"1대1 격투 대전 모드", "다대다 전투 모드"};
    public static final String[] MODE_NOTES = {
            "일대일 결투 · 제한 시간 90초",
            "대장 + 돌격군 3 + 궁수 2 · 상대 대장을 먼저 쓰러뜨리면 승리"
    };

    // ==================================================================
    //  허브
    // ==================================================================
    public static void drawHub(Canvas c, Draw d, float time) {
        backdrop(c, d, time);
        Screens.topBar(c, d, "미니게임 · 2인 대전");
        d.text(c, "기기 두 대를 같은 Wi-Fi 에 연결해 1대1로 겨룹니다", 60f, 92f, 14f,
                Theme.TEXT_DIM, Draw.LEFT, false);

        bigCard(c, d, HUB_HOST, "방 만들기", "내가 방장이 됩니다",
                "모드 선택 · 전투 시작은 방장만 할 수 있습니다", true);
        bigCard(c, d, HUB_JOIN, "방 참가하기", "열려 있는 방에 들어갑니다",
                "방 목록에서 고르거나 방장의 주소를 직접 입력", false);

        menu(c, d, HUB_BACK, "뒤로", false);
        d.text(c, "※ 두 기기가 같은 공유기(Wi-Fi) 에 연결되어 있어야 합니다", 480f, 392f, 12f,
                Theme.TEXT_DIM, Draw.CENTER, false);
    }

    private static void bigCard(Canvas c, Draw d, float[] r, String title, String sub,
                                String note, boolean accent) {
        d.card(c, r[0], r[1], r[2], r[3], accent);
        float cx = (r[0] + r[2]) * 0.5f;
        d.text(c, title, cx, r[1] + 78f, 30f, Theme.TEXT, Draw.CENTER, true);
        d.line(c, cx - 40f, r[1] + 94f, cx + 40f, r[1] + 94f, 1.4f, 0xFFC9A55E);
        d.text(c, sub, cx, r[1] + 126f, 15f, Theme.TEXT_GOLD, Draw.CENTER, true);
        d.text(c, note, cx, r[1] + 158f, 12f, Theme.TEXT_DIM, Draw.CENTER, false);
    }

    // ==================================================================
    //  방 찾기
    // ==================================================================
    public static void drawJoin(Canvas c, Draw d, float time, List<LanDiscovery.Room> rooms,
                                boolean finderFailed, String ip, boolean connecting) {
        backdrop(c, d, time);
        Screens.topBar(c, d, "방 참가하기");

        d.spaced(c, "열려 있는 방", 62f, 112f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
        if (rooms.isEmpty()) {
            String dots = "...".substring(0, 1 + ((int) (time * 2f) % 3));
            d.text(c, finderFailed ? "방을 자동으로 찾을 수 없습니다 → 오른쪽에 주소를 입력하세요"
                            : "같은 Wi-Fi 의 방을 찾는 중" + dots,
                    62f, 156f, 14f, Theme.TEXT_DIM, Draw.LEFT, false);
            d.text(c, "방장 화면에 표시된 주소(예: 192.168.0.12)를 오른쪽에 입력해도 됩니다.",
                    62f, 182f, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        }
        for (int i = 0; i < rooms.size() && i < JOIN_ROOMS.length; i++) {
            LanDiscovery.Room rm = rooms.get(i);
            float[] r = JOIN_ROOMS[i];
            d.card(c, r[0], r[1], r[2], r[3], !rm.full);
            d.text(c, (rm.name.length() == 0 ? "방" : rm.name) + " 의 방", r[0] + 22f, r[1] + 26f,
                    17f, rm.full ? Theme.TEXT_DIM : Theme.TEXT, Draw.LEFT, true);
            d.text(c, rm.ip + " · " + MODE_NAMES[rm.mode == 1 ? 1 : 0], r[0] + 22f, r[1] + 47f,
                    11f, Theme.TEXT_DIM, Draw.LEFT, false);
            d.text(c, rm.full ? "가득 참" : "입장", r[2] - 24f, r[1] + 36f, 14f,
                    rm.full ? Theme.TEXT_DIM : Theme.TEXT_GOLD, Draw.RIGHT, true);
        }

        // ---- 주소 직접 입력
        d.spaced(c, "방장 주소 입력", 562f, 112f, 12f, Theme.BRONZE_LIGHT, Draw.LEFT, true, 3f);
        float[] f = JOIN_FIELD;
        d.round(c, f[0], f[1], f[2], f[3], 4f, 0xEE0C0806);
        d.roundStroke(c, f[0], f[1], f[2], f[3], 4f, 1.4f, 0xCCB8914F);
        boolean blink = ((int) (time * 2f) % 2) == 0;
        d.text(c, ip.length() == 0 ? "예: 192.168.0.12" : ip + (blink ? "|" : ""),
                f[0] + 16f, f[1] + 30f, 20f,
                ip.length() == 0 ? Theme.TEXT_DIM : Theme.TEXT, Draw.LEFT, true);
        for (int i = 0; i < KEY_LABELS.length; i++) {
            float[] k = joinKey(i);
            d.card(c, k[0], k[1], k[2], k[3], false);
            d.text(c, KEY_LABELS[i].equals("<") ? "지움" : KEY_LABELS[i],
                    (k[0] + k[2]) * 0.5f, (k[1] + k[3]) * 0.5f + 7f, 20f, Theme.TEXT, Draw.CENTER, true);
        }
        menu(c, d, JOIN_GO, connecting ? "접속 중..." : "이 주소로 접속", ip.length() > 0 && !connecting);
        menu(c, d, JOIN_BACK, "뒤로", false);
    }

    // ==================================================================
    //  방 (모드 / 캐릭터 선택)
    // ==================================================================
    public static void drawRoom(Canvas c, Draw d, Assets assets, float time, VersusSession s,
                                String myIp) {
        backdrop(c, d, time);
        Screens.topBar(c, d, s.host ? "내 방 · 방장" : "참가한 방");

        // ---- 안내 줄
        if (s.host) {
            d.text(c, "내 주소  " + (myIp.length() == 0 ? "(Wi-Fi 에 연결하세요)" : myIp)
                            + "   —   참가자가 방 목록에 없으면 이 주소를 입력하게 하세요",
                    60f, 82f, 12f, Theme.TEXT_GOLD, Draw.LEFT, true);
        } else {
            d.text(c, "방장 " + s.room.hostName + " 의 방에 들어왔습니다", 60f, 82f, 12f,
                    Theme.TEXT_GOLD, Draw.LEFT, true);
        }

        // ---- 모드 선택 (방장만 누를 수 있음)
        for (int i = 0; i < 2; i++) {
            float[] r = ROOM_MODE[i];
            boolean on = (s.room.mode == i);
            d.card(c, r[0], r[1], r[2], r[3], on);
            if (on) d.round(c, r[0], r[1], r[2], r[3], 3f, 0x33C9A55E);
            int tc = on ? Theme.TEXT_GOLD : (s.host ? Theme.TEXT : Theme.TEXT_DIM);
            d.text(c, MODE_NAMES[i], r[0] + 24f, r[1] + 34f, 19f, tc, Draw.LEFT, true);
            d.text(c, MODE_NOTES[i], r[0] + 24f, r[1] + 58f, 11f, Theme.TEXT_DIM, Draw.LEFT, false);
            if (on) d.text(c, "선택됨", r[2] - 18f, r[1] + 28f, 12f, Theme.TEXT_GOLD, Draw.RIGHT, true);
            if (!s.host && !on) d.rect(c, r[0], r[1], r[2], r[3], 0x66050302);
        }
        if (!s.host) {
            d.text(c, "모드는 방장만 고를 수 있습니다", 480f, 182f, 11f, Theme.TEXT_DIM, Draw.CENTER, false);
        }

        // ---- 두 사람의 캐릭터
        int p1 = s.room.hostChar;
        int p2 = s.host ? s.room.guestChar : s.myChar;
        boolean guestHere = s.host ? s.guestPresent() : true;

        playerPanel(c, d, assets, time, ROOM_P1, "P1 · 방장", s.room.hostName, p1, s.host, true,
                "방장");
        String p2State = !guestHere ? "참가자를 기다리는 중..."
                : (s.room.guestReady ? "준비 완료" : "준비 대기");
        playerPanel(c, d, assets, time, ROOM_P2, "P2 · 참가자", guestHere ? s.room.guestName : "",
                p2, !s.host, guestHere, p2State);

        // ---- 아래 버튼
        menu(c, d, ROOM_BACK, "나가기", false);
        if (s.host) {
            boolean ok = s.canStart();
            menu(c, d, ROOM_ACTION, "전투 시작", ok);
            d.text(c, !guestHere ? "참가자가 들어오면 시작할 수 있습니다"
                            : (ok ? "준비되었습니다 — 시작하세요!" : "참가자가 준비하면 시작할 수 있습니다"),
                    480f, 468f, 12f, ok ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.CENTER, false);
        } else {
            menu(c, d, ROOM_ACTION, s.myReady ? "준비 취소" : "준비 완료", !s.myReady);
            d.text(c, s.myReady ? "방장이 시작하기를 기다리는 중..." : "캐릭터를 고르고 '준비 완료' 를 누르세요",
                    480f, 468f, 12f, s.myReady ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.CENTER, false);
        }
    }

    private static void playerPanel(Canvas c, Draw d, Assets assets, float time, float[] r,
                                    String tag, String name, int charIdx, boolean mine,
                                    boolean present, String state) {
        d.card(c, r[0], r[1], r[2], r[3], mine);
        if (mine) d.round(c, r[0], r[1], r[2], r[3], 3f, 0x22C9A55E);
        d.text(c, tag + (mine ? "  (나)" : ""), r[0] + 22f, r[1] + 30f, 15f,
                mine ? Theme.TEXT_GOLD : Theme.TEXT, Draw.LEFT, true);
        d.text(c, state, r[2] - 20f, r[1] + 30f, 12f,
                state.equals("준비 완료") ? Theme.TEXT_GOLD : Theme.TEXT_DIM, Draw.RIGHT, true);

        if (!present) {
            d.text(c, "?", (r[0] + r[2]) * 0.5f, r[1] + 130f, 64f, 0x55B8914F, Draw.CENTER, true);
            return;
        }

        GameData.CharacterDef ch = GameData.VERSUS_ROSTER[Math.max(0,
                Math.min(charIdx, GameData.VERSUS_ROSTER.length - 1))];

        // 그림
        float sx = r[0] + 130f;
        float base = r[3] - 26f;
        d.oval(c, sx - 60f, base - 6f, sx + 60f, base + 6f, 0x66000000);
        previewSprite(c, d, assets, ch, sx, base, 128f, time);

        // 이름 · 설명
        float tx = r[0] + 224f;
        d.text(c, ch.name, tx, r[1] + 84f, 24f, Theme.TEXT, Draw.LEFT, true);
        d.text(c, ch.title, tx, r[1] + 106f, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        for (int i = 0; i < ch.traits.length && i < 3; i++) {
            d.text(c, "· " + ch.traits[i], tx, r[1] + 134f + i * 20f, 13f, Theme.TEXT, Draw.LEFT, false);
        }
        d.text(c, "체력 " + ch.maxHp, tx, r[1] + 196f, 11f, Theme.TEXT_DIM, Draw.LEFT, false);

        // ◀ ▶ (자기 패널만)
        if (mine) {
            float[] al = arrowLeft(r), ar = arrowRight(r);
            d.round(c, al[0], al[1], al[2], al[3], 4f, 0xAA0C0806);
            d.roundStroke(c, al[0], al[1], al[2], al[3], 4f, 1.2f, 0xCCB8914F);
            d.triangle(c, al[2] - 14f, al[1] + 20f, al[2] - 14f, al[3] - 20f, al[0] + 14f,
                    (al[1] + al[3]) * 0.5f, Theme.TEXT);
            d.round(c, ar[0], ar[1], ar[2], ar[3], 4f, 0xAA0C0806);
            d.roundStroke(c, ar[0], ar[1], ar[2], ar[3], 4f, 1.2f, 0xCCB8914F);
            d.triangle(c, ar[0] + 14f, ar[1] + 20f, ar[0] + 14f, ar[3] - 20f, ar[2] - 14f,
                    (ar[1] + ar[3]) * 0.5f, Theme.TEXT);
        }
    }

    private static void previewSprite(Canvas c, Draw d, Assets assets, GameData.CharacterDef ch,
                                      float cx, float base, float bodyH, float time) {
        if (assets == null) return;
        SpriteSet set = assets.spriteSet(ch.spriteName);
        int idle = ((int) (time / 0.6f) % 2 == 0) ? SpriteSet.F_IDLE_A : SpriteSet.F_IDLE_B;
        Bitmap bmp = set.frame(idle);
        if (bmp == null) return;
        float scale = set.scaleFor(bodyH);
        float w = set.canvasW * scale;
        float h = set.canvasH * scale;
        d.bitmap(c, bmp, cx - set.anchorX * scale, base - h, cx - set.anchorX * scale + w, base, 255);
    }

    // ==================================================================
    //  결과
    // ==================================================================
    public static void drawResult(Canvas c, Draw d, BattleWorld w, float time, boolean host,
                                  boolean peerHere) {
        Backdrop.draw(c, d, w.stage == null ? 0 : w.stage.backdrop, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xCC050302);
        d.panel(c, 90f, 56f, 870f, 398f, 0xEE0C0806);

        int res = w.localResult();
        boolean win = res == BattleWorld.RESULT_WIN;
        boolean draw = res == BattleWorld.RESULT_DRAW;
        d.text(c, "MINI GAME · " + (w.armyMode ? MODE_NAMES[1] : MODE_NAMES[0]), 130f, 98f, 13f,
                Theme.BRONZE_LIGHT, Draw.LEFT, true);
        d.textOutlined(c, draw ? "무승부" : (win ? "승리" : "패배"), 130f, 150f, 40f,
                draw ? Theme.TEXT : (win ? Theme.TEXT_GOLD : 0xFFD9604C), Draw.LEFT, true);

        Fighter me = w.localFighter();
        Fighter foe = w.remoteFighter();
        String line = draw ? "제한 시간이 끝났고 체력이 같습니다."
                : win ? "상대를 쓰러뜨렸습니다!" : "상대에게 패했습니다. 한 번 더 겨뤄 보세요.";
        d.text(c, line, 130f, 196f, 16f, Theme.TEXT, Draw.LEFT, false);
        d.line(c, 130f, 222f, 830f, 222f, 1f, 0x66B8914F);

        record(c, d, 130f, 252f, "나 (" + (w.localSide == 0 ? "P1" : "P2") + ")",
                me.name + "  " + Math.max(0, me.hp) + " / " + me.maxHp);
        record(c, d, 530f, 252f, "상대 (" + (w.localSide == 0 ? "P2" : "P1") + ")",
                foe.name + "  " + Math.max(0, foe.hp) + " / " + foe.maxHp);
        record(c, d, 130f, 306f, "소요 시간", (int) w.elapsed + "초");
        record(c, d, 530f, 306f, "모드", w.armyMode ? MODE_NAMES[1] : MODE_NAMES[0]);

        menu(c, d, RES_EXIT, "나가기", false);
        if (host) {
            menu(c, d, RES_PRIMARY, peerHere ? "한 번 더 (방으로)" : "방으로", true);
        } else {
            d.card(c, RES_PRIMARY[0], RES_PRIMARY[1], RES_PRIMARY[2], RES_PRIMARY[3], false);
            d.text(c, "방장이 정하는 중...", (RES_PRIMARY[0] + RES_PRIMARY[2]) * 0.5f,
                    (RES_PRIMARY[1] + RES_PRIMARY[3]) * 0.5f + 6f, 16f, Theme.TEXT_DIM, Draw.CENTER, true);
        }
    }

    private static void record(Canvas c, Draw d, float x, float y, String label, String value) {
        d.text(c, label, x, y, 12f, Theme.TEXT_DIM, Draw.LEFT, false);
        d.text(c, value, x, y + 22f, 18f, Theme.TEXT, Draw.LEFT, true);
    }

    // ==================================================================
    //  공용
    // ==================================================================
    private static void backdrop(Canvas c, Draw d, float time) {
        Backdrop.draw(c, d, Backdrop.NIGHT_GATE, time, 0f);
        d.rect(c, 0f, 0f, Theme.VW, Theme.VH, 0xDD050302);
    }

    private static void menu(Canvas c, Draw d, float[] r, String label, boolean primary) {
        BattleRenderer.menuButton(c, d, r, label, primary);
    }
}
