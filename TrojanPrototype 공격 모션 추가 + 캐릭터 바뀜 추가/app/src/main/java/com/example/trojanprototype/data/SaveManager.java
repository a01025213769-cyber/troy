package com.example.trojanprototype.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
 * 슬롯 내용은 Gson(외부 라이브러리)으로 JSON 문자열 하나로 묶어 저장합니다.
 * (예전 버전의 항목별 저장은 처음 읽을 때 JSON 으로 자동 변환됩니다)
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
    /**
     * 슬롯 하나에 실제로 저장되는 내용. Gson 이 이 객체를 JSON 문자열로 바꿔 저장합니다.
     *   예) {"step":5,"difficulty":1,"playSeconds":312,"savedAt":1764120000000,"version":2}
     * 항목을 늘리고 싶으면 필드만 추가하면 됩니다. (예전 저장에는 기본값이 들어갑니다)
     */
    static final class SlotData {
        int version = 2;
        int step = 1;
        int difficulty = 1;
        long playSeconds = 0L;
        long savedAt = 0L;
    }

    private final Gson gson = new Gson();

    private static String jsonKey(int index) {
        return "slot" + index + "_json";
    }

    /** 저장된 JSON 을 읽습니다. 예전 방식(항목별 저장)이면 읽어서 JSON 으로 옮겨 둡니다. */
    private SlotData load(int index) {
        String json = sp.getString(jsonKey(index), null);
        if (json != null) {
            try {
                SlotData d = gson.fromJson(json, SlotData.class);
                if (d != null) return d;
            } catch (JsonSyntaxException ignored) {
                // 깨진 저장은 비어 있는 슬롯으로 취급합니다.
            }
            return null;
        }
        String k = "slot" + index + "_";
        if (!sp.getBoolean(k + "used", false)) return null;
        SlotData d = new SlotData();
        d.step = sp.getInt(k + "step", 1);
        d.difficulty = sp.getInt(k + "diff", 1);
        d.playSeconds = sp.getLong(k + "play", 0L);
        d.savedAt = sp.getLong(k + "at", 0L);
        sp.edit().putString(jsonKey(index), gson.toJson(d)).apply();
        removeLegacy(sp.edit(), index).apply();
        return d;
    }

    private static SharedPreferences.Editor removeLegacy(SharedPreferences.Editor e, int index) {
        String k = "slot" + index + "_";
        return e.remove(k + "used").remove(k + "step").remove(k + "diff")
                .remove(k + "play").remove(k + "at");
    }

    public synchronized Slot read(int index) {
        Slot s = new Slot();
        if (index < 0 || index >= SLOT_COUNT) return s;
        SlotData d = load(index);
        if (d == null) return s;
        s.used = true;
        s.step = clamp(d.step, 1, ChapterData.ENDING_STEP);
        s.difficulty = clamp(d.difficulty, 0, GameData.DIFFICULTIES.length - 1);
        s.playSeconds = Math.max(0L, d.playSeconds);
        s.savedAt = d.savedAt;
        return s;
    }

    public synchronized void write(int index, int step, int difficulty, long playSeconds) {
        if (index < 0 || index >= SLOT_COUNT) return;
        SlotData d = new SlotData();
        d.step = clamp(step, 1, ChapterData.ENDING_STEP);
        d.difficulty = difficulty;
        d.playSeconds = playSeconds;
        d.savedAt = System.currentTimeMillis();
        removeLegacy(sp.edit(), index).putString(jsonKey(index), gson.toJson(d)).apply();
    }

    public synchronized void delete(int index) {
        if (index < 0 || index >= SLOT_COUNT) return;
        removeLegacy(sp.edit(), index).remove(jsonKey(index)).apply();
    }

    public synchronized boolean hasAnySlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (load(i) != null) return true;
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
