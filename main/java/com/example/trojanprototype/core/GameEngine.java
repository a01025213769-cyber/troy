package com.example.trojanprototype.core;

import android.content.Context;
import android.graphics.Canvas;

import com.example.trojanprototype.audio.SoundManager;
import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.PadInput;
import com.example.trojanprototype.data.ChapterData;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.SaveManager;
import com.example.trojanprototype.data.StoryData;
import com.example.trojanprototype.net.LanDiscovery;
import com.example.trojanprototype.net.NetLink;
import com.example.trojanprototype.net.Proto;
import com.example.trojanprototype.ui.BattleRenderer;
import com.example.trojanprototype.ui.MiniScreens;
import com.example.trojanprototype.ui.Screens;
import com.example.trojanprototype.ui.Theme;
import com.example.trojanprototype.util.Assets;
import com.example.trojanprototype.util.Draw;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 게임 전체 흐름(씬 전환)을 관리합니다.
 *
 * 담당: 유장원 (시스템)
 *
 * ── 전체 화면 흐름 ────────────────────────────────────────
 *
 *   타이틀 ─► 프롤로그(컷신) ─► 캐릭터/난이도 선택
 *                                      │
 *        ┌─────────────────────────────┘
 *        ▼
 *   1장 컷신 ─► 전투 1 ─► 결과/해설 ┐
 *   2장 컷신 ─► 전투 2 ─► 결과/해설 ├─ 패배하면 같은 전투 재도전
 *   3장 컷신 ─► 보스전 ─► 결과/해설 ┘
 *        │
 *        ▼
 *   4장 트로이 함락(컷신) ─► 5장 귀향(컷신) ─► ODYSSEY 엔딩 ─► 타이틀
 *
 * flowStep 숫자 하나로 위 순서를 관리하므로,
 * 장면을 추가하려면 advanceFlow() 의 switch 에 한 줄만 넣으면 됩니다.
 *
 * ── 저장 · 이어하기 · 챕터 선택 ───────────────────────────
 *   타이틀 ─► 새 이야기 시작 ─► 저장 슬롯 고르기 ─► 프롤로그부터
 *          ─► 이어하기       ─► 저장 슬롯 고르기 ─► 저장한 장면부터
 *          ─► 챕터 선택      ─► 열린 챕터 고르기 ─► 그 챕터부터 (이후 순서대로)
 *
 *   · 저장 슬롯은 3개. 저장 내용은 "다시 시작할 flowStep" 하나입니다.
 *     (컷신 중에 저장하면 그 컷신부터, 전투 중에 저장하면 그 전투를 처음부터)
 *   · 슬롯으로 시작한 플레이는 전투에 이길 때마다 그 슬롯에 자동 저장됩니다.
 *   · 챕터는 앞 챕터를 클리어해야 열립니다 (ChapterData 참고).
 */
public class GameEngine {

    public static final int SCENE_TITLE = 0;
    public static final int SCENE_HOWTO = 1;
    public static final int SCENE_STORY = 2;
    public static final int SCENE_BATTLE = 3;
    public static final int SCENE_RESULT = 4;
    public static final int SCENE_FINALE = 5;
    public static final int SCENE_SLOTS = 6;
    public static final int SCENE_CHAPTERS = 7;
    // 미니게임 (2인 대전)
    public static final int SCENE_MINI_HUB = 8;
    public static final int SCENE_MINI_JOIN = 9;
    public static final int SCENE_MINI_ROOM = 10;
    public static final int SCENE_MINI_BATTLE = 11;
    public static final int SCENE_MINI_RESULT = 12;

    private final Assets assets;
    private final SoundManager sound;
    private final TouchInput input;
    private final Draw d = new Draw();
    private final BattleWorld world = new BattleWorld();
    private final SaveManager save;

    private int scene = SCENE_TITLE;
    private float sceneTime = 0f;

    private int flowStep = 0;
    private int stageIndex = 0;
    private int diffIndex = 1;      // 기본 '보통'

    private boolean aiDebug = false;
    private boolean paused = false;

    // 컷신 진행 상태
    private StoryData.Chapter story = StoryData.PROLOGUE;
    private int storyShown = 1;
    private float storyLineTime = 0f;
    private static final float STORY_AUTO_DELAY = 2.4f;

    private int tipIndex = 0;

    // ---- 저장 / 챕터 선택 상태 ----
    private final SaveManager.Slot[] slots = new SaveManager.Slot[SaveManager.SLOT_COUNT];
    private boolean hasSave = false;
    private int slotMode = Screens.SLOT_MODE_LOAD;
    private int slotReturnScene = SCENE_TITLE;
    private int confirm = Screens.CONFIRM_NONE;
    private int confirmSlot = -1;
    /** 지금 플레이가 기록되고 있는 슬롯 (-1 이면 자동 저장 없음) */
    private int activeSlot = -1;
    /** 클리어한 챕터 수 (챕터 선택 해금 기준) */
    private int cleared = 0;
    /** 이번 플레이의 누적 시간(초) — 슬롯에 함께 저장됩니다 */
    private float playTime = 0f;

    private String toast = null;
    private float toastTime = 0f;

    // ---- 미니게임 (2인 대전) 상태 ----
    private final WifiHelper wifi;
    private VersusSession vs = null;
    private String joinIp = "";
    private String myIp = "";
    private float exitArm = 0f;
    private boolean endSounded = false;
    private final PadInput guestPad = new PadInput();

    public GameEngine(Context ctx, TouchInput input) {
        this.input = input;
        this.wifi = new WifiHelper(ctx);
        this.assets = new Assets(ctx);
        this.sound = new SoundManager(ctx, assets);
        this.save = new SaveManager(ctx);
        this.cleared = save.cleared();
        refreshSlots();
        setScene(SCENE_TITLE);
    }

    public Assets assets() {
        return assets;
    }

    public SoundManager sound() {
        return sound;
    }

    // ==================================================================
    //  씬 전환
    // ==================================================================
    private void setScene(int s) {
        scene = s;
        sceneTime = 0f;
        paused = false;
        input.setBattleMode(s == SCENE_BATTLE || s == SCENE_MINI_BATTLE);
        updateBgm();
    }

    private void updateBgm() {
        switch (scene) {
            case SCENE_BATTLE:
                sound.playBgm(world.stage != null && world.stage.boss ? "bgm_boss" : "bgm_battle");
                break;
            case SCENE_MINI_BATTLE:
                sound.playBgm("bgm_battle");
                break;
            case SCENE_STORY:
            case SCENE_RESULT:
            case SCENE_FINALE:
                sound.playBgm("bgm_story");
                break;
            case SCENE_SLOTS:
                sound.playBgm(slotMode == Screens.SLOT_MODE_SAVE ? "bgm_story" : "bgm_title");
                break;
            default:
                sound.playBgm("bgm_title");
                break;
        }
    }

    /**
     * 다음 장면으로 넘어갑니다. 게임 전체 순서가 여기 한곳에 모여 있습니다.
     *
     * 주인공은 플레이어가 고르지 않고 스토리가 정합니다.
     * 1~3장은 아킬레우스, 아킬레우스가 쓰러지는 4장 이후로는 오디세우스입니다.
     * (각 전투의 주인공은 GameData.STAGES[i].hero 에 적혀 있습니다)
     */
    private void advanceFlow() {
        flowStep++;

        // 컷신뿐인 챕터는 끝까지 보면 클리어 (전투 챕터는 전투에 이겼을 때 클리어)
        if (flowStep == ChapterData.LIST[1].startStep) {
            onChapterCleared(0, flowStep);          // 프롤로그를 끝까지 봄
        } else if (flowStep == ChapterData.ENDING_STEP) {
            onChapterCleared(ChapterData.count() - 1, flowStep);   // 5장 귀향
        }

        switch (flowStep) {
            case 1: showStory(StoryData.PROLOGUE); break;

            case 2: showStory(StoryData.CH_WAR); break;
            case 3: startBattle(0); break;          // 아킬레우스 vs 트로이 병사

            case 4: showStory(StoryData.CH_WALL); break;
            case 5: startBattle(1); break;          // 아킬레우스 vs 트로이 정예병

            case 6: showStory(StoryData.CH_HEROES); break;
            case 7: startBattle(2); break;          // 아킬레우스 vs 헥토르 (보스)

            // 아킬레우스가 파리스의 화살에 쓰러지고 오디세우스로 교체
            case 8: showStory(StoryData.CH_ACHILLES_FALL); break;
            case 9: startBattle(3); break;          // 오디세우스 vs 왕궁 수비대

            case 10: showStory(StoryData.CH_RETURN); break;
            case 11: setScene(SCENE_FINALE); break;

            default: backToTitle(); break;
        }
    }

    private void showStory(StoryData.Chapter ch) {
        story = ch;
        storyShown = 1;
        storyLineTime = 0f;
        setScene(SCENE_STORY);
    }

    private void startBattle(int index) {
        stageIndex = index;
        if (stageIndex < 0) stageIndex = 0;
        if (stageIndex >= GameData.STAGES.length) stageIndex = GameData.STAGES.length - 1;

        GameData.StageDef stage = GameData.STAGES[stageIndex];
        // 활을 쓰는 주인공(오디세우스)은 조작 요령이 다르므로 안내 문구도 다릅니다.
        String[] tips = stage.army ? StoryData.BATTLE_TIP_ARMY
                : (stage.hero.light != null && stage.hero.light.ranged)
                ? StoryData.BATTLE_TIP_RANGED : StoryData.BATTLE_TIP;
        String tip = tips[tipIndex % tips.length];
        tipIndex++;

        world.start(stage.hero, stage, GameData.DIFFICULTIES[diffIndex], assets, sound, tip);
        setScene(SCENE_BATTLE);
    }

    private void backToTitle() {
        flowStep = 0;
        stageIndex = 0;
        activeSlot = -1;
        confirm = Screens.CONFIRM_NONE;
        refreshSlots();
        setScene(SCENE_TITLE);
    }

    // ==================================================================
    //  저장 · 이어하기 · 챕터 선택
    // ==================================================================
    private void toast(String msg) {
        toast = msg;
        toastTime = 2.4f;
    }

    /** 저장소에서 슬롯 3개를 다시 읽어 화면 표시용 문구까지 만들어 둡니다. */
    private void refreshSlots() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        for (int i = 0; i < SaveManager.SLOT_COUNT; i++) {
            SaveManager.Slot s = save.read(i);
            if (s.used) {
                ChapterData.Entry e = ChapterData.LIST[ChapterData.chapterOfStep(s.step)];
                boolean ending = s.step >= ChapterData.ENDING_STEP;
                s.tag = ending ? "ENDING" : e.tag;
                s.title = ending ? "ODYSSEY · The Journey Home" : e.title;
                s.where = ChapterData.resumeLabel(s.step);
                s.meta = "난이도 " + GameData.DIFFICULTIES[s.difficulty].name
                        + " · 플레이 " + formatPlayTime(s.playSeconds);
                s.date = s.savedAt > 0 ? fmt.format(new Date(s.savedAt)) : "";
            }
            slots[i] = s;
        }
        hasSave = save.hasAnySlot();
        cleared = save.cleared();
    }

    private static String formatPlayTime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return h + "시간 " + m + "분";
        if (m > 0) return m + "분 " + s + "초";
        return s + "초";
    }

    private void openSlots(int mode, int returnScene) {
        refreshSlots();
        slotMode = mode;
        slotReturnScene = returnScene;
        confirm = Screens.CONFIRM_NONE;
        confirmSlot = -1;
        setScene(SCENE_SLOTS);
    }

    /** 슬롯 화면을 닫고 원래 화면으로 돌아갑니다. */
    private void leaveSlots() {
        confirm = Screens.CONFIRM_NONE;
        if (slotMode == Screens.SLOT_MODE_SAVE) {
            setScene(slotReturnScene);
            if (slotReturnScene == SCENE_BATTLE) paused = true;   // 전투는 멈춘 채로 돌아옵니다
        } else {
            setScene(SCENE_TITLE);
        }
    }

    /** 지금 저장하면 "다시 시작할 장면"이 되는 flowStep */
    private int currentCheckpoint() {
        if (slotReturnScene == SCENE_RESULT && world.result == BattleWorld.RESULT_WIN) {
            return Math.min(flowStep + 1, ChapterData.ENDING_STEP);   // 이긴 전투는 다음 장면부터
        }
        return Math.max(1, Math.min(flowStep, ChapterData.ENDING_STEP));
    }

    private void saveToSlot(int index) {
        save.write(index, currentCheckpoint(), diffIndex, (long) playTime);
        activeSlot = index;
        sound.play("sfx_select");
        refreshSlots();
        toast("슬롯 " + (index + 1) + " 에 저장했습니다");
        leaveSlots();
    }

    private void startNewGame(int index) {
        activeSlot = index;
        playTime = 0f;
        save.write(index, 1, diffIndex, 0L);
        refreshSlots();
        flowStep = 0;
        advanceFlow();
    }

    private void loadSlot(int index) {
        SaveManager.Slot s = save.read(index);
        if (!s.used) {
            toast("비어 있는 슬롯입니다");
            return;
        }
        activeSlot = index;
        diffIndex = s.difficulty;
        playTime = s.playSeconds;
        flowStep = s.step - 1;
        advanceFlow();
    }

    private void startChapter(int chapter) {
        activeSlot = -1;        // 챕터 선택 플레이는 슬롯에 자동 기록하지 않습니다.
        playTime = 0f;
        flowStep = ChapterData.LIST[chapter].startStep - 1;
        advanceFlow();
    }

    /** 챕터 하나를 클리어했을 때: 해금 기록 + (슬롯으로 하는 중이면) 자동 저장 */
    private void onChapterCleared(int chapter, int nextStep) {
        save.markCleared(chapter);
        cleared = save.cleared();
        if (activeSlot >= 0) {
            save.write(activeSlot, nextStep, diffIndex, (long) playTime);
            hasSave = true;
        }
    }

    // ==================================================================
    //  갱신
    // ==================================================================
    public void update(float dt) {
        sceneTime += dt;
        input.update(dt);
        if (toastTime > 0f) toastTime -= dt;
        if (scene == SCENE_STORY || scene == SCENE_RESULT || scene == SCENE_FINALE
                || (scene == SCENE_BATTLE && !paused)) {
            playTime += dt;
        }

        switch (scene) {
            case SCENE_SLOTS: updateSlots(); break;
            case SCENE_CHAPTERS: updateChapters(); break;
            case SCENE_MINI_HUB: updateMiniHub(); break;
            case SCENE_MINI_JOIN: updateMiniJoin(dt); break;
            case SCENE_MINI_ROOM: updateMiniRoom(dt); break;
            case SCENE_MINI_BATTLE: updateMiniBattle(dt); break;
            case SCENE_MINI_RESULT: updateMiniResult(dt); break;
            case SCENE_TITLE: updateTitle(); break;
            case SCENE_HOWTO: updateHowTo(); break;
            case SCENE_STORY: updateStory(dt); break;
            case SCENE_BATTLE: updateBattle(dt); break;
            case SCENE_RESULT: updateResult(); break;
            case SCENE_FINALE: updateFinale(); break;
            default: break;
        }
    }

    private void updateTitle() {
        // 난이도 — 캐릭터 선택 화면을 없앴으므로 타이틀에서 고릅니다.
        // (발표 때 AI 반응 속도 차이를 보여 주는 데 쓰입니다)
        for (int i = 0; i < Screens.TITLE_DIFF.length
                && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = Screens.TITLE_DIFF[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                diffIndex = i;
                sound.play("sfx_select");
                return;
            }
        }
        if (input.consumeTapIn(Screens.TITLE_MINIGAME[0], Screens.TITLE_MINIGAME[1],
                Screens.TITLE_MINIGAME[2], Screens.TITLE_MINIGAME[3])) {
            sound.play("sfx_select");
            setScene(SCENE_MINI_HUB);
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_START[0], Screens.TITLE_START[1],
                Screens.TITLE_START[2], Screens.TITLE_START[3])) {
            sound.play("sfx_select");
            openSlots(Screens.SLOT_MODE_NEW, SCENE_TITLE);
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_CONTINUE[0], Screens.TITLE_CONTINUE[1],
                Screens.TITLE_CONTINUE[2], Screens.TITLE_CONTINUE[3])) {
            if (hasSave) {
                sound.play("sfx_select");
                openSlots(Screens.SLOT_MODE_LOAD, SCENE_TITLE);
            } else {
                toast("저장된 이야기가 없습니다");
            }
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_CHAPTERS[0], Screens.TITLE_CHAPTERS[1],
                Screens.TITLE_CHAPTERS[2], Screens.TITLE_CHAPTERS[3])) {
            sound.play("sfx_select");
            cleared = save.cleared();
            setScene(SCENE_CHAPTERS);
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_HOWTO[0], Screens.TITLE_HOWTO[1],
                Screens.TITLE_HOWTO[2], Screens.TITLE_HOWTO[3])) {
            sound.play("sfx_select");
            setScene(SCENE_HOWTO);
        }
    }

    private void updateHowTo() {
        if (input.consumeTap()) {
            setScene(SCENE_TITLE);
        }
    }

    private void updateStory(float dt) {
        storyLineTime += dt;

        // 건너뛰기
        if (input.consumeTapIn(Screens.STORY_SKIP[0], Screens.STORY_SKIP[1],
                Screens.STORY_SKIP[2], Screens.STORY_SKIP[3])) {
            sound.play("sfx_select");
            advanceFlow();
            return;
        }

        if (input.consumeTapIn(Screens.STORY_SAVE[0], Screens.STORY_SAVE[1],
                Screens.STORY_SAVE[2], Screens.STORY_SAVE[3])) {
            sound.play("sfx_select");
            openSlots(Screens.SLOT_MODE_SAVE, SCENE_STORY);
            return;
        }

        if (input.consumeTap()) {
            if (storyShown < story.lines.length) {
                storyShown++;
                storyLineTime = 0f;
            } else {
                advanceFlow();
            }
            return;
        }

        // 탭하지 않아도 한 줄씩 저절로 나타납니다.
        if (storyShown < story.lines.length && storyLineTime >= STORY_AUTO_DELAY) {
            storyShown++;
            storyLineTime = 0f;
        }
    }

    private void updateBattle(float dt) {
        if (input.consumePress(TouchInput.BTN_AI)) {
            aiDebug = !aiDebug;
            sound.play("sfx_select");
        }
        if (input.consumePress(TouchInput.BTN_PAUSE)) {
            paused = !paused;
            sound.play("sfx_select");
        }

        if (paused) {
            if (input.consumeTapIn(BattleRenderer.PAUSE_RESUME[0], BattleRenderer.PAUSE_RESUME[1],
                    BattleRenderer.PAUSE_RESUME[2], BattleRenderer.PAUSE_RESUME[3])) {
                paused = false;
                return;
            }
            if (input.consumeTapIn(BattleRenderer.PAUSE_RETRY[0], BattleRenderer.PAUSE_RETRY[1],
                    BattleRenderer.PAUSE_RETRY[2], BattleRenderer.PAUSE_RETRY[3])) {
                world.restart(assets);
                paused = false;
                return;
            }
            if (input.consumeTapIn(BattleRenderer.PAUSE_SAVE[0], BattleRenderer.PAUSE_SAVE[1],
                    BattleRenderer.PAUSE_SAVE[2], BattleRenderer.PAUSE_SAVE[3])) {
                sound.play("sfx_select");
                openSlots(Screens.SLOT_MODE_SAVE, SCENE_BATTLE);
                return;
            }
            if (input.consumeTapIn(BattleRenderer.PAUSE_TITLE[0], BattleRenderer.PAUSE_TITLE[1],
                    BattleRenderer.PAUSE_TITLE[2], BattleRenderer.PAUSE_TITLE[3])) {
                backToTitle();
            }
            return;
        }

        world.update(dt, input);

        if (world.finished) {
            if (world.result == BattleWorld.RESULT_WIN) {
                // 전투 챕터는 이겨야 클리어 — 다음 챕터가 열리고, 슬롯 플레이면 자동 저장됩니다.
                onChapterCleared(stageIndex + 1, Math.min(flowStep + 1, ChapterData.ENDING_STEP));
                if (activeSlot >= 0) toast("슬롯 " + (activeSlot + 1) + " 에 자동 저장했습니다");
            }
            setScene(SCENE_RESULT);
        }
    }

    private void updateResult() {
        boolean win = (world.result == BattleWorld.RESULT_WIN);

        if (input.consumeTapIn(Screens.RESULT_PRIMARY[0], Screens.RESULT_PRIMARY[1],
                Screens.RESULT_PRIMARY[2], Screens.RESULT_PRIMARY[3])) {
            sound.play("sfx_select");
            if (win) {
                advanceFlow();
            } else {
                world.restart(assets);
                setScene(SCENE_BATTLE);
            }
            return;
        }
        if (input.consumeTapIn(Screens.RESULT_SAVE[0], Screens.RESULT_SAVE[1],
                Screens.RESULT_SAVE[2], Screens.RESULT_SAVE[3])) {
            sound.play("sfx_select");
            openSlots(Screens.SLOT_MODE_SAVE, SCENE_RESULT);
            return;
        }
        if (input.consumeTapIn(Screens.RESULT_SECONDARY[0], Screens.RESULT_SECONDARY[1],
                Screens.RESULT_SECONDARY[2], Screens.RESULT_SECONDARY[3])) {
            sound.play("sfx_select");
            backToTitle();
        }
    }

    private void updateSlots() {
        // 확인 창이 떠 있으면 그 창만 반응합니다.
        if (confirm != Screens.CONFIRM_NONE) {
            if (input.consumeTapIn(Screens.CONFIRM_YES[0], Screens.CONFIRM_YES[1],
                    Screens.CONFIRM_YES[2], Screens.CONFIRM_YES[3])) {
                int target = confirmSlot;
                int kind = confirm;
                confirm = Screens.CONFIRM_NONE;
                if (kind == Screens.CONFIRM_DELETE) {
                    save.delete(target);
                    if (activeSlot == target) activeSlot = -1;
                    refreshSlots();
                    toast("슬롯 " + (target + 1) + " 을(를) 삭제했습니다");
                } else if (slotMode == Screens.SLOT_MODE_NEW) {
                    startNewGame(target);
                } else {
                    saveToSlot(target);
                }
                return;
            }
            if (input.consumeTapIn(Screens.CONFIRM_NO[0], Screens.CONFIRM_NO[1],
                    Screens.CONFIRM_NO[2], Screens.CONFIRM_NO[3])) {
                confirm = Screens.CONFIRM_NONE;
                return;
            }
            input.consumeTap();
            return;
        }

        if (input.consumeTapIn(Screens.SLOT_BACK[0], Screens.SLOT_BACK[1],
                Screens.SLOT_BACK[2], Screens.SLOT_BACK[3])) {
            sound.play("sfx_select");
            leaveSlots();
            return;
        }

        for (int i = 0; i < SaveManager.SLOT_COUNT; i++) {
            // 삭제 버튼은 줄 안쪽에 있으므로 줄보다 먼저 확인합니다.
            if (slots[i].used) {
                float[] del = Screens.slotDelete(i);
                if (input.consumeTapIn(del[0], del[1], del[2], del[3])) {
                    sound.play("sfx_select");
                    confirm = Screens.CONFIRM_DELETE;
                    confirmSlot = i;
                    return;
                }
            }
            float[] r = Screens.SLOT_ROWS[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                onSlotChosen(i);
                return;
            }
        }
        input.consumeTap();     // 빈 곳을 누른 탭은 버립니다.
    }

    private void onSlotChosen(int i) {
        boolean used = slots[i].used;
        switch (slotMode) {
            case Screens.SLOT_MODE_LOAD:
                if (used) {
                    sound.play("sfx_select");
                    loadSlot(i);
                } else {
                    toast("비어 있는 슬롯입니다");
                }
                break;
            case Screens.SLOT_MODE_NEW:
                sound.play("sfx_select");
                if (used) {
                    confirm = Screens.CONFIRM_OVERWRITE;
                    confirmSlot = i;
                } else {
                    startNewGame(i);
                }
                break;
            default:    // SLOT_MODE_SAVE
                if (used) {
                    sound.play("sfx_select");
                    confirm = Screens.CONFIRM_OVERWRITE;
                    confirmSlot = i;
                } else {
                    saveToSlot(i);
                }
                break;
        }
    }

    private void updateChapters() {
        if (input.consumeTapIn(Screens.CHAPTER_BACK[0], Screens.CHAPTER_BACK[1],
                Screens.CHAPTER_BACK[2], Screens.CHAPTER_BACK[3])) {
            sound.play("sfx_select");
            setScene(SCENE_TITLE);
            return;
        }
        for (int i = 0; i < ChapterData.count() && i < Screens.CHAPTER_CARDS.length; i++) {
            float[] r = Screens.CHAPTER_CARDS[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                if (ChapterData.isUnlocked(i, cleared)) {
                    sound.play("sfx_select");
                    startChapter(i);
                } else {
                    toast("이전 챕터를 클리어하면 열립니다");
                }
                return;
            }
        }
        input.consumeTap();
    }


    // ==================================================================
    //  미니게임 — 2인 대전 (Wi-Fi)
    //
    //   허브 ─┬─ 방 만들기 ─► 방(방장) ──[전투 시작]──► 전투 ─► 결과 ─┐
    //         │                 ▲  모드·캐릭터                         │ 한 번 더
    //         └─ 방 참가하기 ─► 방 찾기 ─► 방(참가자) ──[준비]─────────┘
    //
    //  계산은 방장 기기에서만 합니다. 참가자 기기는 자기 조작을 보내고 장면을 받아 그립니다.
    //  (자세한 구조: net/VersusSync, core/VersusSession)
    // ==================================================================
    private static String deviceName() {
        try {
            String m = android.os.Build.MODEL;
            return m == null ? "플레이어" : m;
        } catch (Throwable t) {
            return "플레이어";
        }
    }

    private void closeSession() {
        if (vs != null) {
            vs.close();
            vs = null;
        }
        wifi.releaseMulticast();
    }

    /** 미니게임 허브로 (연결은 모두 닫습니다) */
    private void leaveMini() {
        closeSession();
        setScene(SCENE_MINI_HUB);
    }

    private void updateMiniHub() {
        if (input.consumeTapIn(MiniScreens.HUB_BACK[0], MiniScreens.HUB_BACK[1],
                MiniScreens.HUB_BACK[2], MiniScreens.HUB_BACK[3])) {
            sound.play("sfx_select");
            setScene(SCENE_TITLE);
            return;
        }
        if (input.consumeTapIn(MiniScreens.HUB_HOST[0], MiniScreens.HUB_HOST[1],
                MiniScreens.HUB_HOST[2], MiniScreens.HUB_HOST[3])) {
            sound.play("sfx_select");
            closeSession();
            vs = new VersusSession(true, deviceName());
            vs.room.hostChar = 0;
            vs.openRoom();
            myIp = LanDiscovery.localIp();
            setScene(SCENE_MINI_ROOM);
            return;
        }
        if (input.consumeTapIn(MiniScreens.HUB_JOIN[0], MiniScreens.HUB_JOIN[1],
                MiniScreens.HUB_JOIN[2], MiniScreens.HUB_JOIN[3])) {
            sound.play("sfx_select");
            startGuestSearch();
            setScene(SCENE_MINI_JOIN);
            return;
        }
        input.consumeTap();
    }

    private void startGuestSearch() {
        closeSession();
        vs = new VersusSession(false, deviceName());
        wifi.acquireMulticast();
        vs.startFinder();
    }

    /** "192.168.0.12" 처럼 생긴 주소인지 */
    static boolean validIp(String s) {
        String[] p = s.split("\\.", -1);
        if (p.length != 4) return false;
        for (int i = 0; i < 4; i++) {
            if (p[i].length() < 1 || p[i].length() > 3) return false;
            for (int k = 0; k < p[i].length(); k++) {
                if (p[i].charAt(k) < '0' || p[i].charAt(k) > '9') return false;
            }
            if (Integer.parseInt(p[i]) > 255) return false;
        }
        return true;
    }

    private void connectTo(String ip, int port) {
        if (vs == null) return;
        sound.play("sfx_select");
        vs.join(ip, port);
    }

    private void updateMiniJoin(float dt) {
        if (vs == null) {
            setScene(SCENE_MINI_HUB);
            return;
        }
        int ev = vs.pump(dt, null);
        if (ev == VersusSession.EV_LEFT || ev == VersusSession.EV_REJECTED) {
            toast(vs.message.length() > 0 ? vs.message : "연결하지 못했습니다");
            startGuestSearch();         // 처음 상태로 돌려 다시 시도할 수 있게
            return;
        }
        if (vs.joined()) {
            vs.stopFinder();
            wifi.releaseMulticast();
            setScene(SCENE_MINI_ROOM);
            return;
        }

        if (input.consumeTapIn(MiniScreens.JOIN_BACK[0], MiniScreens.JOIN_BACK[1],
                MiniScreens.JOIN_BACK[2], MiniScreens.JOIN_BACK[3])) {
            sound.play("sfx_select");
            leaveMini();
            return;
        }
        if (vs.connecting()) {          // 접속 중에는 다른 입력을 받지 않습니다
            input.consumeTap();
            return;
        }

        java.util.List<LanDiscovery.Room> rooms = vs.foundRooms();
        for (int i = 0; i < rooms.size() && i < MiniScreens.JOIN_ROOMS.length; i++) {
            float[] r = MiniScreens.JOIN_ROOMS[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                LanDiscovery.Room rm = rooms.get(i);
                if (rm.full) toast("이미 가득 찬 방입니다");
                else connectTo(rm.ip, rm.port);
                return;
            }
        }
        for (int i = 0; i < MiniScreens.KEY_LABELS.length; i++) {
            float[] k = MiniScreens.joinKey(i);
            if (input.consumeTapIn(k[0], k[1], k[2], k[3])) {
                String key = MiniScreens.KEY_LABELS[i];
                if (key.equals("<")) {
                    if (joinIp.length() > 0) joinIp = joinIp.substring(0, joinIp.length() - 1);
                } else if (joinIp.length() < 15) {
                    joinIp += key;
                }
                return;
            }
        }
        if (input.consumeTapIn(MiniScreens.JOIN_GO[0], MiniScreens.JOIN_GO[1],
                MiniScreens.JOIN_GO[2], MiniScreens.JOIN_GO[3])) {
            if (validIp(joinIp)) connectTo(joinIp, NetLink.PORT);
            else toast("주소 형식이 올바르지 않습니다 (예: 192.168.0.12)");
            return;
        }
        input.consumeTap();
    }

    private void updateMiniRoom(float dt) {
        if (vs == null) {
            setScene(SCENE_MINI_HUB);
            return;
        }
        boolean hadGuest = vs.hadGuest;
        int ev = vs.pump(dt, null);
        switch (ev) {
            case VersusSession.EV_JOINED:
                toast("참가자가 입장했습니다");
                sound.play("sfx_select");
                break;
            case VersusSession.EV_LEFT:
            case VersusSession.EV_REJECTED:
                toast(vs.message.length() > 0 ? vs.message : "연결이 끊어졌습니다");
                if (vs.host && hadGuest) {
                    vs.reopenRoom();        // 방은 그대로 두고 새 참가자를 기다립니다
                } else {
                    leaveMini();
                }
                return;
            case VersusSession.EV_START:
                startVersusBattle();
                return;
            default:
                break;
        }

        if (input.consumeTapIn(MiniScreens.ROOM_BACK[0], MiniScreens.ROOM_BACK[1],
                MiniScreens.ROOM_BACK[2], MiniScreens.ROOM_BACK[3])) {
            sound.play("sfx_select");
            leaveMini();
            return;
        }

        // 모드 선택 — 방장만 (참가자의 탭은 무시)
        for (int i = 0; i < 2; i++) {
            float[] r = MiniScreens.ROOM_MODE[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                if (vs.host) {
                    if (vs.room.mode != i) {
                        sound.play("sfx_select");
                        vs.setMode(i);
                    }
                } else {
                    toast("모드는 방장만 고를 수 있습니다");
                }
                return;
            }
        }

        // 내 캐릭터 고르기 (◀ ▶)
        float[] mine = vs.host ? MiniScreens.ROOM_P1 : MiniScreens.ROOM_P2;
        int n = GameData.VERSUS_ROSTER.length;
        for (int dir = -1; dir <= 1; dir += 2) {
            float[] a = dir < 0 ? MiniScreens.arrowLeft(mine) : MiniScreens.arrowRight(mine);
            if (input.consumeTapIn(a[0], a[1], a[2], a[3])) {
                sound.play("sfx_select");
                if (vs.host) {
                    vs.setHostChar(vs.room.hostChar + dir);
                } else {
                    vs.myChar = ((vs.myChar + dir) % n + n) % n;
                    vs.myReady = false;             // 캐릭터를 바꾸면 준비가 풀립니다
                    vs.sendGuestState();
                }
                return;
            }
        }

        if (input.consumeTapIn(MiniScreens.ROOM_ACTION[0], MiniScreens.ROOM_ACTION[1],
                MiniScreens.ROOM_ACTION[2], MiniScreens.ROOM_ACTION[3])) {
            if (vs.host) {
                if (vs.canStart()) {
                    sound.play("sfx_select");
                    vs.sendStart();
                    startVersusBattle();
                } else if (!vs.guestPresent()) {
                    toast("참가자가 아직 없습니다");
                } else {
                    toast("참가자가 아직 준비하지 않았습니다");
                }
            } else {
                sound.play("sfx_select");
                vs.myReady = !vs.myReady;
                vs.sendGuestState();
            }
            return;
        }
        input.consumeTap();
    }

    private void startVersusBattle() {
        boolean host = vs.host;
        int mode = host ? vs.room.mode : vs.startMode;
        int p1 = host ? vs.room.hostChar : vs.startHostChar;
        int p2 = host ? vs.room.guestChar : vs.startGuestChar;
        world.startVersus(p1, p2, mode == Proto.MODE_ARMY, assets, sound, !host, host ? 0 : 1);
        vs.resetBattleState();
        guestPad.clear();
        exitArm = 0f;
        endSounded = false;
        setScene(SCENE_MINI_BATTLE);
    }

    /** 전투 중 나가기 — 실수로 누르지 않도록 두 번 눌러야 나갑니다. */
    private void requestMiniExit() {
        if (exitArm > 0f) {
            leaveMini();
            return;
        }
        exitArm = 2.2f;
        toast("한 번 더 누르면 대전에서 나갑니다");
    }

    private void updateMiniBattle(float dt) {
        if (vs == null) {
            setScene(SCENE_MINI_HUB);
            return;
        }
        int ev = vs.pump(dt, world);
        if (ev == VersusSession.EV_LEFT) {
            toast(vs.message.length() > 0 ? vs.message : "연결이 끊어졌습니다");
            leaveMini();
            return;
        }
        if (input.consumePress(TouchInput.BTN_PAUSE)) requestMiniExit();
        input.consumePress(TouchInput.BTN_AI);
        if (exitArm > 0f) exitArm -= dt;

        if (vs.host) {
            world.pad0.readFrom(input);
            world.update(dt, input);
            vs.sendSnapshot(dt, world, world.phase == BattleWorld.PHASE_END);
        } else {
            guestPad.readFrom(input);
            vs.sendInput(dt, guestPad);
            world.update(dt, input);
        }

        if (world.phase == BattleWorld.PHASE_END && !endSounded) {
            endSounded = true;
            int r = world.localResult();
            if (r == BattleWorld.RESULT_WIN) sound.play("sfx_win");
            else if (r == BattleWorld.RESULT_LOSE) sound.play("sfx_lose");
        }
        if (world.finished) setScene(SCENE_MINI_RESULT);
    }

    private void updateMiniResult(float dt) {
        if (vs == null) {
            setScene(SCENE_MINI_HUB);
            return;
        }
        boolean hadGuest = vs.hadGuest;
        int ev = vs.pump(dt, null);
        if (ev == VersusSession.EV_BACK) {
            setScene(SCENE_MINI_ROOM);
            return;
        }
        if (ev == VersusSession.EV_LEFT) {
            toast(vs.message.length() > 0 ? vs.message : "상대가 나갔습니다");
            if (vs.host && hadGuest) {
                vs.reopenRoom();
                setScene(SCENE_MINI_ROOM);
            } else {
                leaveMini();
            }
            return;
        }
        if (input.consumeTapIn(MiniScreens.RES_EXIT[0], MiniScreens.RES_EXIT[1],
                MiniScreens.RES_EXIT[2], MiniScreens.RES_EXIT[3])) {
            sound.play("sfx_select");
            leaveMini();
            return;
        }
        if (vs.host && input.consumeTapIn(MiniScreens.RES_PRIMARY[0], MiniScreens.RES_PRIMARY[1],
                MiniScreens.RES_PRIMARY[2], MiniScreens.RES_PRIMARY[3])) {
            sound.play("sfx_select");
            vs.sendBack();
            setScene(SCENE_MINI_ROOM);
            return;
        }
        input.consumeTap();
    }

    private void updateFinale() {
        if (sceneTime > 4.2f && input.consumeTap()) {
            backToTitle();
        } else {
            input.consumeTap();
        }
    }

    // ==================================================================
    //  그리기
    // ==================================================================
    public void render(Canvas c) {
        switch (scene) {
            case SCENE_TITLE:
                Screens.drawTitle(c, d, assets, sceneTime, diffIndex, cleared, hasSave);
                break;
            case SCENE_HOWTO:
                Screens.drawTitle(c, d, assets, sceneTime, diffIndex, cleared, hasSave);
                Screens.drawHowTo(c, d);
                break;
            case SCENE_SLOTS:
                Screens.drawSlots(c, d, sceneTime, slotMode, slots, confirm, confirmSlot);
                break;
            case SCENE_CHAPTERS:
                Screens.drawChapters(c, d, sceneTime, cleared);
                break;
            case SCENE_STORY:
                Screens.drawStory(c, d, assets, story, storyShown, storyLineTime, sceneTime);
                break;
            case SCENE_BATTLE:
                BattleRenderer.draw(c, d, world, input, aiDebug, paused);
                break;
            case SCENE_RESULT:
                Screens.drawResult(c, d, world,
                        stageIndex < GameData.STAGES.length - 1, sceneTime);
                break;
            case SCENE_FINALE:
                Screens.drawFinale(c, d, sceneTime);
                break;
            case SCENE_MINI_HUB:
                MiniScreens.drawHub(c, d, sceneTime);
                break;
            case SCENE_MINI_JOIN:
                MiniScreens.drawJoin(c, d, sceneTime,
                        vs == null ? new java.util.ArrayList<LanDiscovery.Room>() : vs.foundRooms(),
                        vs != null && vs.finderFailed(), joinIp, vs != null && vs.connecting());
                break;
            case SCENE_MINI_ROOM:
                if (vs != null) MiniScreens.drawRoom(c, d, assets, sceneTime, vs, myIp);
                break;
            case SCENE_MINI_BATTLE:
                BattleRenderer.draw(c, d, world, input, false, false);
                break;
            case SCENE_MINI_RESULT:
                MiniScreens.drawResult(c, d, world, sceneTime, vs != null && vs.host,
                        vs != null && vs.isLinkAlive());
                break;
            default:
                d.rect(c, 0f, 0f, Theme.VW, Theme.VH, Theme.BG_DEEP);
                break;
        }
        Screens.drawToast(c, d, toast, toastTime);
    }

    // ==================================================================
    //  안드로이드 생명주기
    // ==================================================================
    /** 뒤로 가기 버튼 처리. true 를 반환하면 앱을 종료하지 않습니다. */
    public boolean onBackPressed() {
        switch (scene) {
            case SCENE_TITLE:
                return false;   // 타이틀에서 뒤로 = 앱 종료
            case SCENE_BATTLE:
                paused = !paused;
                return true;
            case SCENE_SLOTS:
                if (confirm != Screens.CONFIRM_NONE) confirm = Screens.CONFIRM_NONE;
                else leaveSlots();
                return true;
            case SCENE_CHAPTERS:
                setScene(SCENE_TITLE);
                return true;
            case SCENE_MINI_HUB:
                setScene(SCENE_TITLE);
                return true;
            case SCENE_MINI_JOIN:
            case SCENE_MINI_ROOM:
            case SCENE_MINI_RESULT:
                leaveMini();
                return true;
            case SCENE_MINI_BATTLE:
                requestMiniExit();
                return true;
            default:
                backToTitle();
                return true;
        }
    }

    public void onPause() {
        if (scene == SCENE_BATTLE) paused = true;
        input.clearAll();
        sound.onPause();
    }

    public void onResume() {
        sound.onResume();
        updateBgm();
    }

    public void release() {
        closeSession();
        sound.release();
        assets.clear();
    }
}
