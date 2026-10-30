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
    public static final int SCENE_SELECT = 3;
    public static final int SCENE_BATTLE = 4;
    public static final int SCENE_RESULT = 5;
    public static final int SCENE_FINALE = 6;

    private final Assets assets;
    private final SoundManager sound;
    private final TouchInput input;
    private final Draw d = new Draw();
    private final BattleWorld world = new BattleWorld();

    private int scene = SCENE_TITLE;
    private float sceneTime = 0f;

    private int flowStep = 0;
    private int stageIndex = 0;
    private int charIndex = 0;
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

    /** 다음 장면으로 넘어갑니다. 게임 전체 순서가 여기 한곳에 모여 있습니다. */
    private void advanceFlow() {
        flowStep++;
        switch (flowStep) {
            case 1: showStory(StoryData.PROLOGUE); break;
            case 2: setScene(SCENE_SELECT); break;

            case 3: showStory(StoryData.CH_WAR); break;
            case 4: startBattle(0); break;

            case 5: showStory(StoryData.CH_WALL); break;
            case 6: startBattle(1); break;

            case 7: showStory(StoryData.CH_HEROES); break;
            case 8: startBattle(2); break;

            case 9: showStory(StoryData.CH_FALL); break;
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

        String tip = StoryData.BATTLE_TIP[tipIndex % StoryData.BATTLE_TIP.length];
        tipIndex++;

        world.start(GameData.PLAYABLE[charIndex], GameData.STAGES[stageIndex],
                GameData.DIFFICULTIES[diffIndex], assets, sound, tip);
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
            case SCENE_SELECT: updateSelect(); break;
            case SCENE_BATTLE: updateBattle(dt); break;
            case SCENE_RESULT: updateResult(); break;
            case SCENE_FINALE: updateFinale(); break;
            default: break;
        }
    }

    private void updateTitle() {
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

    private void updateSelect() {
        if (input.consumeTapIn(Screens.SELECT_CHAR_A[0], Screens.SELECT_CHAR_A[1],
                Screens.SELECT_CHAR_A[2], Screens.SELECT_CHAR_A[3])) {
            charIndex = 0;
            sound.play("sfx_select");
            return;
        }
        if (input.consumeTapIn(Screens.SELECT_CHAR_B[0], Screens.SELECT_CHAR_B[1],
                Screens.SELECT_CHAR_B[2], Screens.SELECT_CHAR_B[3])) {
            charIndex = 1;
            sound.play("sfx_select");
            return;
        }
        for (int i = 0; i < Screens.SELECT_DIFF.length
                && i < GameData.DIFFICULTIES.length; i++) {
            float[] r = Screens.SELECT_DIFF[i];
            if (input.consumeTapIn(r[0], r[1], r[2], r[3])) {
                diffIndex = i;
                sound.play("sfx_select");
                return;
            }
        }
        if (input.consumeTapIn(Screens.SELECT_START[0], Screens.SELECT_START[1],
                Screens.SELECT_START[2], Screens.SELECT_START[3])) {
            sound.play("sfx_select");
            advanceFlow();
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
                Screens.drawTitle(c, d, assets, sceneTime);
                break;
            case SCENE_HOWTO:
                Screens.drawTitle(c, d, assets, sceneTime);
                Screens.drawHowTo(c, d);
                break;
            case SCENE_STORY:
                Screens.drawStory(c, d, story, storyShown, storyLineTime, sceneTime);
                break;
            case SCENE_SELECT:
                Screens.drawSelect(c, d, assets, charIndex, diffIndex, sceneTime);
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
