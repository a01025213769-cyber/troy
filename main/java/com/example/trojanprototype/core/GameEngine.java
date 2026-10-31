package com.example.trojanprototype.core;

import android.content.Context;
import android.graphics.Canvas;

import com.example.trojanprototype.audio.SoundManager;
import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.data.StoryData;
import com.example.trojanprototype.ui.BattleRenderer;
import com.example.trojanprototype.ui.Screens;
import com.example.trojanprototype.ui.Theme;
import com.example.trojanprototype.util.Assets;
import com.example.trojanprototype.util.Draw;

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
 */
public class GameEngine {

    public static final int SCENE_TITLE = 0;
    public static final int SCENE_HOWTO = 1;
    public static final int SCENE_STORY = 2;
    public static final int SCENE_BATTLE = 3;
    public static final int SCENE_RESULT = 4;
    public static final int SCENE_FINALE = 5;

    private final Assets assets;
    private final SoundManager sound;
    private final TouchInput input;
    private final Draw d = new Draw();
    private final BattleWorld world = new BattleWorld();

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

    public GameEngine(Context ctx, TouchInput input) {
        this.input = input;
        this.assets = new Assets(ctx);
        this.sound = new SoundManager(ctx, assets);
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
            case SCENE_FINALE:
                sound.playBgm("bgm_story");
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
        setScene(SCENE_TITLE);
    }

    // ==================================================================
    //  갱신
    // ==================================================================
    public void update(float dt) {
        sceneTime += dt;
        input.update(dt);

        switch (scene) {
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
        if (input.consumeTapIn(Screens.TITLE_START[0], Screens.TITLE_START[1],
                Screens.TITLE_START[2], Screens.TITLE_START[3])) {
            sound.play("sfx_select");
            flowStep = 0;
            advanceFlow();
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
            if (input.consumeTapIn(BattleRenderer.PAUSE_TITLE[0], BattleRenderer.PAUSE_TITLE[1],
                    BattleRenderer.PAUSE_TITLE[2], BattleRenderer.PAUSE_TITLE[3])) {
                backToTitle();
            }
            return;
        }

        world.update(dt, input);

        if (world.finished) {
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
        if (input.consumeTapIn(Screens.RESULT_SECONDARY[0], Screens.RESULT_SECONDARY[1],
                Screens.RESULT_SECONDARY[2], Screens.RESULT_SECONDARY[3])) {
            sound.play("sfx_select");
            backToTitle();
        }
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
                Screens.drawTitle(c, d, sceneTime, diffIndex);
                break;
            case SCENE_HOWTO:
                Screens.drawTitle(c, d, sceneTime, diffIndex);
                Screens.drawHowTo(c, d);
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
            default:
                d.rect(c, 0f, 0f, Theme.VW, Theme.VH, Theme.BG_DEEP);
                break;
        }
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
        sound.release();
        assets.clear();
    }
}
