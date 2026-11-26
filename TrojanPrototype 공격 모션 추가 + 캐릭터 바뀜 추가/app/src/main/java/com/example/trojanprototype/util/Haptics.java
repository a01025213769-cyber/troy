package com.example.trojanprototype.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * 진동(햅틱) 피드백.
 *
 * 담당: 유장원 (시스템)
 *
 * 전투에서 플레이어가 맞거나 필살기를 쓸 때 기기를 짧게 진동시켜 타격감을 줍니다.
 *   · Android 8.0(API 26) 이상: VibrationEffect 로 세기(1~255)까지 조절
 *   · 그 아래: 길이만 지정하는 예전 방식
 * AndroidManifest.xml 의 VIBRATE 권한이 필요합니다. (설치할 때 자동 허용되는 일반 권한)
 * 켜기/끄기 설정은 SharedPreferences 에 저장되어 앱을 다시 켜도 유지됩니다.
 */
public class Haptics {

    private static final String PREFS = "troy_settings";
    private static final String KEY_ON = "vibration_on";

    private final Vibrator vibrator;
    private final SharedPreferences prefs;
    private boolean enabled;

    public Haptics(Context ctx) {
        Vibrator v = null;
        try {
            v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Throwable ignored) {
        }
        vibrator = v;
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        enabled = prefs.getBoolean(KEY_ON, true);
    }

    /** 이 기기에 진동 장치가 있는지 */
    public boolean isAvailable() {
        try {
            return vibrator != null && vibrator.hasVibrator();
        } catch (Throwable e) {
            return false;
        }
    }

    public boolean isOn() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        prefs.edit().putBoolean(KEY_ON, enabled).apply();
        if (enabled) pulse(40, 160);
    }

    /**
     * 한 번 진동합니다.
     * @param ms        길이 (밀리초)
     * @param amplitude 세기 1~255 (API 26 미만에서는 무시)
     */
    public void pulse(int ms, int amplitude) {
        if (!enabled || vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                int a = Math.max(1, Math.min(255, amplitude));
                vibrator.vibrate(VibrationEffect.createOneShot(ms, a));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 여러 번 끊어서 진동합니다. timings = {쉬기, 울림, 쉬기, 울림, ...} (밀리초)
     */
    public void pattern(long[] timings) {
        if (!enabled || vibrator == null || timings == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1));
            } else {
                vibrator.vibrate(timings, -1);
            }
        } catch (Throwable ignored) {
        }
    }

    public void stop() {
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Throwable ignored) {
        }
    }
}
