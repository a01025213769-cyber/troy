package com.example.trojanprototype.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.example.trojanprototype.util.Assets;

import java.util.HashMap;

/**
 * 효과음(SoundPool)과 배경음(MediaPlayer) 관리.
 *
 * 담당: 심신 (사운드) + 유장원 (연결)
 *
 * ★ 중요 ★
 * 이 클래스는 음원 파일을 "이름"으로 찾습니다. (R.raw.xxx 를 코드에 직접 쓰지 않음)
 * 따라서 파일이 없어도 앱은 정상 동작하고, 그냥 소리만 나지 않습니다.
 *
 * 지금 res/raw 에 들어 있는 음원은 tools/make_sounds.py 가 코드로 합성한 것입니다.
 * (외부 음원을 쓰지 않아 저작권 걱정이 없습니다)
 * 다른 음원으로 바꾸려면 같은 이름의 파일을 res/raw 에 덮어쓰면 됩니다.
 * (파일 이름은 소문자 + 숫자 + 밑줄만 가능합니다)
 *
 *   효과음 (.wav / .ogg)           배경음 (.wav / .ogg / .mp3)
 *   ├─ sfx_swing   공격 휘두름      ├─ bgm_title   타이틀
 *   ├─ sfx_hit     타격            ├─ bgm_story   컷신 · 결과
 *   ├─ sfx_heavy   강공격 타격      ├─ bgm_battle  전투
 *   ├─ sfx_guard   방어            ├─ bgm_boss    보스전
 *   ├─ sfx_dodge   회피            └─ bgm_ending  엔딩 (ODYSSEY)
 *   ├─ sfx_ultimate 필살기
 *   ├─ sfx_bow     활 쏘기
 *   ├─ sfx_win     승리
 *   ├─ sfx_lose    패배
 *   └─ sfx_select  버튼 선택
 *
 * 소리 켜기/끄기 설정은 SharedPreferences 에 저장되어 앱을 다시 켜도 유지됩니다.
 */
public class SoundManager {

    /** 미리 불러 둘 효과음 이름 */
    public static final String[] SFX_NAMES = {
            "sfx_swing", "sfx_hit", "sfx_heavy", "sfx_guard", "sfx_dodge",
            "sfx_ultimate", "sfx_bow", "sfx_win", "sfx_lose", "sfx_select"
    };

    /** 배경음 기본 음량 (효과음이 묻히지 않게 절반 정도) */
    private static final float BGM_VOLUME = 0.5f;
    /** 효과음 기본 음량 */
    private static final float SFX_VOLUME = 0.9f;

    private static final String PREFS = "troy_settings";
    private static final String KEY_SOUND = "sound_on";

    private final Context ctx;
    private final Assets assets;
    private final SharedPreferences prefs;
    private SoundPool pool;
    private final HashMap<String, Integer> soundIds = new HashMap<String, Integer>();

    private MediaPlayer bgm;
    private String currentBgm = "";
    /** 소리를 끈 동안 바뀐 장면의 배경음 — 다시 켜면 이 곡부터 */
    private String wantedBgm = "";

    private boolean soundOn = true;

    public SoundManager(Context ctx, Assets assets) {
        this.ctx = ctx;
        this.assets = assets;
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.soundOn = prefs.getBoolean(KEY_SOUND, true);
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            // 군대 전투에서는 여러 소리가 겹치므로 동시 재생 수를 넉넉히 둡니다.
            pool = new SoundPool.Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(attrs)
                    .build();
        } catch (Throwable ignored) {
            pool = null;
        }
        preloadAll();
    }

    private void preloadAll() {
        if (pool == null) return;
        for (int i = 0; i < SFX_NAMES.length; i++) {
            String name = SFX_NAMES[i];
            try {
                int res = assets.rawId(name);
                if (res != 0) {
                    soundIds.put(name, Integer.valueOf(pool.load(ctx, res, 1)));
                }
            } catch (Throwable ignored) {
                // 파일이 없으면 그냥 넘어갑니다.
            }
        }
    }

    /** 효과음 재생. 파일이 없으면 아무 일도 일어나지 않습니다. */
    public void play(String name) {
        play(name, 1f);
    }

    /**
     * 효과음을 음량을 정해 재생합니다.
     * @param volume 0~1. 병사끼리의 타격처럼 덜 중요한 소리는 작게 냅니다.
     */
    public void play(String name, float volume) {
        if (!soundOn || pool == null || name == null || volume <= 0f) return;
        Integer id = soundIds.get(name);
        if (id == null) return;
        float v = Math.min(1f, SFX_VOLUME * volume);
        // 작은 소리는 우선순위를 낮춰, 동시 재생 수가 넘칠 때 먼저 밀려나게 합니다.
        int priority = volume >= 0.6f ? 2 : 1;
        try {
            pool.play(id.intValue(), v, v, priority, 0, 1f);
        } catch (Throwable ignored) {
        }
    }

    /** 배경음 재생. 같은 곡이 이미 재생 중이면 그대로 둡니다. */
    public void playBgm(String name) {
        if (name == null) return;
        wantedBgm = name;
        if (!soundOn) return;
        if (name.equals(currentBgm) && bgm != null) return;
        stopBgm();
        try {
            int res = assets.rawId(name);
            if (res == 0) return;
            bgm = MediaPlayer.create(ctx, res);
            if (bgm != null) {
                bgm.setLooping(true);
                bgm.setVolume(BGM_VOLUME, BGM_VOLUME);
                bgm.start();
                currentBgm = name;
            }
        } catch (Throwable ignored) {
            bgm = null;
        }
    }

    public void stopBgm() {
        if (bgm == null) return;
        try {
            bgm.stop();
            bgm.release();
        } catch (Throwable ignored) {
        }
        bgm = null;
        currentBgm = "";
    }

    public void onPause() {
        try {
            if (bgm != null && bgm.isPlaying()) bgm.pause();
            if (pool != null) pool.autoPause();
        } catch (Throwable ignored) {
        }
    }

    public void onResume() {
        try {
            if (soundOn && bgm != null) bgm.start();
            if (pool != null) pool.autoResume();
        } catch (Throwable ignored) {
        }
    }

    public void release() {
        stopBgm();
        try {
            if (pool != null) pool.release();
        } catch (Throwable ignored) {
        }
        pool = null;
        soundIds.clear();
    }

    public boolean isSoundOn() {
        return soundOn;
    }

    /** 소리 전체(배경음 + 효과음)를 켜고 끕니다. 설정은 저장됩니다. */
    public void toggleSound() {
        soundOn = !soundOn;
        prefs.edit().putBoolean(KEY_SOUND, soundOn).apply();
        if (!soundOn) {
            stopBgm();
        } else {
            String name = wantedBgm;
            wantedBgm = "";
            playBgm(name);
        }
    }
}
