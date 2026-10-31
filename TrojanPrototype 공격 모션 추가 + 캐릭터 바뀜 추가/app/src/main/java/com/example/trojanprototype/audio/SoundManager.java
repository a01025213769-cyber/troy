package com.example.trojanprototype.audio;

import android.content.Context;
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
 * 따라서 파일이 아직 없어도 앱은 정상 동작하고, 그냥 소리만 나지 않습니다.
 *
 * 소리를 넣으려면 res/raw/ 폴더에 아래 이름으로 파일만 넣으면 끝입니다.
 * (파일 이름은 소문자 + 숫자 + 밑줄만 가능합니다)
 *
 *   효과음 (.wav / .ogg 권장)      배경음 (.mp3 / .ogg 권장)
 *   ├─ sfx_swing   공격 휘두름      ├─ bgm_title   타이틀
 *   ├─ sfx_hit     타격            ├─ bgm_story   컷신
 *   ├─ sfx_heavy   강공격 타격      ├─ bgm_battle  전투
 *   ├─ sfx_guard   방어            └─ bgm_boss    보스전
 *   ├─ sfx_dodge   회피
 *   ├─ sfx_ultimate 필살기
 *   ├─ sfx_win     승리
 *   ├─ sfx_lose    패배
 *   └─ sfx_select  버튼 선택
 */
public class SoundManager {

    /** 미리 불러 둘 효과음 이름 */
    public static final String[] SFX_NAMES = {
            "sfx_swing", "sfx_hit", "sfx_heavy", "sfx_guard",
            "sfx_dodge", "sfx_ultimate", "sfx_win", "sfx_lose", "sfx_select"
    };

    private final Context ctx;
    private final Assets assets;
    private SoundPool pool;
    private final HashMap<String, Integer> soundIds = new HashMap<String, Integer>();

    private MediaPlayer bgm;
    private String currentBgm = "";

    private boolean sfxEnabled = true;
    private boolean bgmEnabled = true;

    public SoundManager(Context ctx, Assets assets) {
        this.ctx = ctx;
        this.assets = assets;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            pool = new SoundPool.Builder()
                    .setMaxStreams(6)
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
        if (!sfxEnabled || pool == null || name == null) return;
        Integer id = soundIds.get(name);
        if (id == null) return;
        try {
            pool.play(id.intValue(), 0.9f, 0.9f, 1, 0, 1f);
        } catch (Throwable ignored) {
        }
    }

    /** 배경음 재생. 같은 곡이 이미 재생 중이면 그대로 둡니다. */
    public void playBgm(String name) {
        if (!bgmEnabled || name == null) return;
        if (name.equals(currentBgm) && bgm != null) return;
        stopBgm();
        try {
            int res = assets.rawId(name);
            if (res == 0) return;
            bgm = MediaPlayer.create(ctx, res);
            if (bgm != null) {
                bgm.setLooping(true);
                bgm.setVolume(0.5f, 0.5f);
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
            if (bgmEnabled && bgm != null) bgm.start();
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

    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    public boolean isBgmEnabled() {
        return bgmEnabled;
    }

    public void toggleSfx() {
        sfxEnabled = !sfxEnabled;
    }

    public void toggleBgm() {
        bgmEnabled = !bgmEnabled;
        if (!bgmEnabled) {
            stopBgm();
        }
    }
}
