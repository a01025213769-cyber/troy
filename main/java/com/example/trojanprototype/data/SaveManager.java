package com.example.trojanprototype.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 저장 슬롯과 챕터 클리어 기록.
 *
 * 담당: 유장원 (시스템)
 *
 * ── 저장되는 것 ──────────────────────────────────────────
 *  [슬롯 3개]  각각 "어디부터 다시 시작할지"(step) · 난이도 · 플레이 시간 · 저장 시각
 *  [공용 기록] 클리어한 챕터 수(cleared) — 챕터 선택 화면의 해금에 쓰입니다.
 *              슬롯과 상관없이 한 번 열린 챕터는 계속 열려 있습니다.
 *
 * SharedPreferences 에 저장하므로 앱을 껐다 켜도 남습니다.
 */
public final class SaveManager {

    public static final int SLOT_COUNT = 3;

    private static final String PREFS = "troy_save";
    private static final String KEY_CLEARED = "cleared";

    /** 슬롯 하나의 내용 (화면에 보여 주기 쉽게 문구도 같이 들고 있습니다) */
    public static final class Slot {
        public boolean used = false;
        /** 이어하기를 누르면 시작할 flowStep */
        public int step = 1;
        public int difficulty = 1;
        public long playSeconds = 0L;
        public long savedAt = 0L;

        // 화면 표시용 (GameEngine 이 채웁니다)
        public String tag = "";
        public String title = "";
        public String where = "";
        public String meta = "";
        public String date = "";
    }

    private final SharedPreferences sp;

    public SaveManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------
    //  슬롯
    // ------------------------------------------------------------------
    public synchronized Slot read(int index) {
        Slot s = new Slot();
        if (index < 0 || index >= SLOT_COUNT) return s;
        String k = "slot" + index + "_";
        s.used = sp.getBoolean(k + "used", false);
        if (!s.used) return s;
        s.step = clamp(sp.getInt(k + "step", 1), 1, ChapterData.ENDING_STEP);
        s.difficulty = clamp(sp.getInt(k + "diff", 1), 0, GameData.DIFFICULTIES.length - 1);
        s.playSeconds = Math.max(0L, sp.getLong(k + "play", 0L));
        s.savedAt = sp.getLong(k + "at", 0L);
        return s;
    }

    public synchronized void write(int index, int step, int difficulty, long playSeconds) {
        if (index < 0 || index >= SLOT_COUNT) return;
        String k = "slot" + index + "_";
        sp.edit()
                .putBoolean(k + "used", true)
                .putInt(k + "step", clamp(step, 1, ChapterData.ENDING_STEP))
                .putInt(k + "diff", difficulty)
                .putLong(k + "play", playSeconds)
                .putLong(k + "at", System.currentTimeMillis())
                .apply();
    }

    public synchronized void delete(int index) {
        if (index < 0 || index >= SLOT_COUNT) return;
        String k = "slot" + index + "_";
        sp.edit()
                .remove(k + "used").remove(k + "step").remove(k + "diff")
                .remove(k + "play").remove(k + "at")
                .apply();
    }

    public synchronized boolean hasAnySlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (sp.getBoolean("slot" + i + "_used", false)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    //  챕터 클리어 기록
    // ------------------------------------------------------------------
    /** 클리어한 챕터 수 (0 ~ 챕터 개수) */
    public synchronized int cleared() {
        return clamp(sp.getInt(KEY_CLEARED, 0), 0, ChapterData.count());
    }

    /** chapter 번호의 챕터를 클리어했다고 기록합니다. 이미 더 많이 깼다면 그대로 둡니다. */
    public synchronized void markCleared(int chapter) {
        int now = cleared();
        if (chapter + 1 > now) {
            sp.edit().putInt(KEY_CLEARED, Math.min(chapter + 1, ChapterData.count())).apply();
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
