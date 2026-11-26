package com.example.trojanprototype.core;

import android.content.Context;
import android.graphics.Canvas;

import com.example.trojanprototype.audio.SoundManager;
import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.data.ChapterData;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.SaveManager;
import com.example.trojanprototype.data.AiMemory;
import com.example.trojanprototype.ai.PlayerModel;
import com.example.trojanprototype.data.StoryData;
import com.example.trojanprototype.data.record.BattleRecord;
import com.example.trojanprototype.data.record.RecordRepository;
import com.example.trojanprototype.ui.Backdrop;
import com.example.trojanprototype.ui.BattleRenderer;
import com.example.trojanprototype.ui.LottieFx;
import com.example.trojanprototype.ui.VirtualPad;
import com.example.trojanprototype.util.Haptics;
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
    public static final int SCENE_RECORDS = 8;

    /** 전투 기록 데이터베이스 (Room) */
    private final RecordRepository records;
    /** 진동 피드백 */
    private final Haptics haptics;
    /** Lottie 승리 연출 */
    private final LottieFx fx;
    /** 폰 흔들기 감지 (가속도 센서) — 전투 중 흔들면 필살기 */
    private final ShakeDetector shake;
    /** 적 AI 가 배운 플레이어 습관 (전투가 바뀌어도 같은 모델) + 저장소 */
    private final PlayerModel playerModel = new PlayerModel();
    private final AiMemory aiMemory;

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

    public GameEngine(Context ctx, TouchInput input) {
        this.input = input;
        this.assets = new Assets(ctx);
        Backdrop.setAssets(assets);
        this.sound = new SoundManager(ctx, assets);
        this.save = new SaveManager(ctx);
        this.records = new RecordRepository(ctx);
        this.haptics = new Haptics(ctx);
        this.world.setHaptics(haptics);
        this.fx = new LottieFx(ctx, assets);
        this.shake = new ShakeDetector(ctx);
        this.aiMemory = new AiMemory(ctx);
        aiMemory.load(playerModel);
        world.model = playerModel;
        VirtualPad.shakeHint = shake.isAvailable() && shake.isOn();
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
        input.setBattleMode(s == SCENE_BATTLE);
        updateBgm();
    }

    private void updateBgm() {
        switch (scene) {
            case SCENE_BATTLE:
                sound.playBgm(world.stage != null && world.stage.boss ? "bgm_boss" : "bgm_battle");
                break;
            case SCENE_STORY:
            case SCENE_RESULT:
                sound.playBgm("bgm_story");
                break;
            case SCENE_FINALE:
                sound.playBgm("bgm_ending");
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

        // 전투가 아닐 때 흔든 것은 버립니다. (전투에 들어가자마자 필살기가 나가지 않도록)
        if (scene != SCENE_BATTLE) shake.consumeShake();

        switch (scene) {
            case SCENE_SLOTS: updateSlots(); break;
            case SCENE_CHAPTERS: updateChapters(); break;
            case SCENE_RECORDS: updateRecords(); break;
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
        // 소리 켜기/끄기 (발표장처럼 소리를 꺼야 할 때). 설정은 저장됩니다.
        if (input.consumeTapIn(Screens.TITLE_SOUND[0], Screens.TITLE_SOUND[1],
                Screens.TITLE_SOUND[2], Screens.TITLE_SOUND[3])) {
            sound.toggleSound();
            sound.play("sfx_select");
            toast(sound.isSoundOn() ? "소리를 켰습니다" : "소리를 껐습니다");
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_SHAKE[0], Screens.TITLE_SHAKE[1],
                Screens.TITLE_SHAKE[2], Screens.TITLE_SHAKE[3])) {
            sound.play("sfx_select");
            if (!shake.isAvailable()) {
                toast("이 기기에는 가속도 센서가 없습니다");
            } else {
                shake.toggle();
                VirtualPad.shakeHint = shake.isOn();
                toast(shake.isOn() ? "전투 중 폰을 흔들면 필살기가 나갑니다" : "흔들기 필살기를 껐습니다");
            }
            return;
        }
        if (input.consumeTapIn(Screens.TITLE_VIBRATE[0], Screens.TITLE_VIBRATE[1],
                Screens.TITLE_VIBRATE[2], Screens.TITLE_VIBRATE[3])) {
            sound.play("sfx_select");
            if (!haptics.isAvailable()) {
                toast("이 기기에는 진동 장치가 없습니다");
            } else {
                haptics.toggle();
                toast(haptics.isOn() ? "진동을 켰습니다" : "진동을 껐습니다");
            }
            return;
        }
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
        if (input.consumeTapIn(Screens.TITLE_RECORDS[0], Screens.TITLE_RECORDS[1],
                Screens.TITLE_RECORDS[2], Screens.TITLE_RECORDS[3])) {
            sound.play("sfx_select");
            records.refresh();
            setScene(SCENE_RECORDS);
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
        // 폰을 흔들면 필살기 버튼을 누른 것과 같게 처리합니다.
        // (일시정지 중이거나 "시작!" 전에 흔든 것은 버립니다)
        if (shake.consumeShake() && !paused && world.phase == BattleWorld.PHASE_FIGHT) {
            if (world.player.ultimateReady()) {
                input.injectPress(TouchInput.BTN_ULT);
            } else {
                world.notice("분노 부족", Theme.TEXT_DIM);
            }
        }
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
            saveRecord();
            aiMemory.save(playerModel);     // 이번 전투에서 배운 습관을 저장
            if (world.result == BattleWorld.RESULT_WIN) {
                // 전투 챕터는 이겨야 클리어 — 다음 챕터가 열리고, 슬롯 플레이면 자동 저장됩니다.
                onChapterCleared(stageIndex + 1, Math.min(flowStep + 1, ChapterData.ENDING_STEP));
                if (activeSlot >= 0) toast("슬롯 " + (activeSlot + 1) + " 에 자동 저장했습니다");
            }
            setScene(SCENE_RESULT);
        }
    }

    /** 방금 끝난 전투를 데이터베이스에 한 줄로 남깁니다. */
    private void saveRecord() {
        BattleRecord r = new BattleRecord();
        r.stage = stageIndex;
        r.difficulty = diffIndex;
        r.win = (world.result == BattleWorld.RESULT_WIN) ? 1 : 0;
        r.hpPercent = Math.round(world.player.hpRatio() * 100f);
        r.seconds = (int) world.elapsed;
        r.bestCombo = world.bestCombo;
        r.hero = world.character != null ? world.character.name : "";
        r.playedAt = System.currentTimeMillis();
        records.add(r);
    }

    private void updateRecords() {
        if (input.consumeTapIn(Screens.RECORDS_BACK[0], Screens.RECORDS_BACK[1],
                Screens.RECORDS_BACK[2], Screens.RECORDS_BACK[3])) {
            sound.play("sfx_select");
            setScene(SCENE_TITLE);
            return;
        }
        if (input.consumeTapIn(Screens.RECORDS_RESET_AI[0], Screens.RECORDS_RESET_AI[1],
                Screens.RECORDS_RESET_AI[2], Screens.RECORDS_RESET_AI[3])) {
            sound.play("sfx_select");
            aiMemory.clear(playerModel);
            toast("AI 가 배운 습관을 지웠습니다");
            return;
        }
        input.consumeTap();
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
                Screens.drawTitle(c, d, assets, sceneTime, diffIndex, cleared, hasSave,
                        sound.isSoundOn(), haptics.isOn(), haptics.isAvailable(),
                        shake.isOn(), shake.isAvailable());
                break;
            case SCENE_HOWTO:
                Screens.drawTitle(c, d, assets, sceneTime, diffIndex, cleared, hasSave,
                        sound.isSoundOn(), haptics.isOn(), haptics.isAvailable(),
                        shake.isOn(), shake.isAvailable());
                Screens.drawHowTo(c, d);
                break;
            case SCENE_SLOTS:
                Screens.drawSlots(c, d, sceneTime, slotMode, slots, confirm, confirmSlot);
                break;
            case SCENE_CHAPTERS:
                Screens.drawChapters(c, d, sceneTime, cleared);
                break;
            case SCENE_RECORDS:
                Screens.drawRecords(c, d, sceneTime, records.snapshot(), playerModel);
                break;
            case SCENE_STORY:
                Screens.drawStory(c, d, assets, story, storyShown, storyLineTime, sceneTime);
                break;
            case SCENE_BATTLE:
                BattleRenderer.draw(c, d, world, input, aiDebug, paused);
                break;
            case SCENE_RESULT:
                Screens.drawResult(c, d, world,
                        stageIndex < GameData.STAGES.length - 1, sceneTime, fx);
                break;
            case SCENE_FINALE:
                Screens.drawFinale(c, d, sceneTime);
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
            case SCENE_RECORDS:
                setScene(SCENE_TITLE);
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
        haptics.stop();
        shake.stop();        // 화면이 가려지면 센서를 꺼서 배터리를 아낍니다
    }

    public void onResume() {
        shake.start();
        sound.onResume();
        updateBgm();
    }

    public void release() {
        sound.release();
        assets.clear();
    }
}
