package com.example.trojanprototype.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * 폰 흔들기 감지 (가속도 센서).
 *
 * 담당: 유장원 (시스템)
 *
 * ── 어떻게 "흔들었다"고 판단하나 ──────────────────────────
 *  1) 가속도 센서 값(x, y, z)으로 전체 가속도의 크기를 구하고 중력(9.8)으로 나눕니다.
 *     가만히 있으면 약 1.0g, 세게 흔들면 2~3g 이상이 나옵니다.
 *  2) SHAKE_G 를 넘는 순간을 "한 번 흔듦"으로 셉니다. (값이 내려갔다가 다시 넘어야 다음 한 번)
 *  3) 0.6초 안에 2번 흔들면 흔들기로 인정합니다.
 *     → 버튼을 세게 누르거나 폰이 한 번 툭 부딪히는 정도로는 발동하지 않습니다.
 *  4) 발동한 뒤 1.2초 동안은 다시 감지하지 않습니다.
 *
 * 센서 이벤트는 UI 스레드로 들어오고, 게임 스레드가 consumeShake() 로 한 번만 읽어 갑니다.
 * 배터리를 아끼려고 앱이 화면에 보일 때만 센서를 켭니다 (start / stop).
 * 켜기/끄기 설정은 SharedPreferences 에 저장됩니다.
 */
public class ShakeDetector implements SensorEventListener {

    /** 이 세기(g)를 넘으면 한 번 흔든 것으로 셉니다 */
    private static final float SHAKE_G = 2.2f;
    /** 값이 이 아래로 내려와야 다음 흔들기를 셉니다 */
    private static final float RESET_G = 1.5f;
    /** 이 시간(초) 안에 SHAKE_COUNT 번 흔들어야 인정 */
    private static final float WINDOW = 0.6f;
    private static final int SHAKE_COUNT = 2;
    /** 발동 후 다시 감지하지 않는 시간(초) */
    private static final float COOLDOWN = 1.2f;

    private static final String PREFS = "troy_settings";
    private static final String KEY_ON = "shake_on";

    private final SensorManager sensors;
    private final Sensor accel;
    private final SharedPreferences prefs;
    private boolean enabled;
    private boolean listening = false;

    private boolean above = false;      // 지금 기준값을 넘은 상태인지
    private int count = 0;              // 창 안에서 센 흔들기 횟수
    private long firstNs = 0L;          // 창이 시작된 시각
    private long lastTriggerNs = 0L;
    private volatile boolean shaken = false;
    /** 디버그·발표용: 마지막으로 잰 세기(g) */
    private volatile float lastG = 1f;

    public ShakeDetector(Context ctx) {
        SensorManager sm = null;
        Sensor s = null;
        try {
            sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            if (sm != null) s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } catch (Throwable ignored) {
        }
        sensors = sm;
        accel = s;
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        enabled = prefs.getBoolean(KEY_ON, true);
    }

    /** 이 기기에 가속도 센서가 있는지 */
    public boolean isAvailable() {
        return sensors != null && accel != null;
    }

    public boolean isOn() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        prefs.edit().putBoolean(KEY_ON, enabled).apply();
        if (enabled) start();
        else stop();
    }

    /** 센서 켜기 (앱이 화면에 보일 때) */
    public void start() {
        if (!enabled || !isAvailable() || listening) return;
        try {
            listening = sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        } catch (Throwable ignored) {
            listening = false;
        }
    }

    /** 센서 끄기 (앱이 가려졌을 때) */
    public void stop() {
        if (!listening) return;
        try {
            sensors.unregisterListener(this);
        } catch (Throwable ignored) {
        }
        listening = false;
        above = false;
        count = 0;
        shaken = false;
    }

    /** 흔들었으면 true 를 한 번만 돌려줍니다. */
    public boolean consumeShake() {
        if (shaken) {
            shaken = false;
            return true;
        }
        return false;
    }

    public float lastG() {
        return lastG;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.values == null || e.values.length < 3) return;
        float x = e.values[0], y = e.values[1], z = e.values[2];
        float g = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        lastG = g;
        long now = e.timestamp;

        if (lastTriggerNs != 0L && (now - lastTriggerNs) < (long) (COOLDOWN * 1e9)) return;

        if (!above && g > SHAKE_G) {
            above = true;
            if (count == 0 || (now - firstNs) > (long) (WINDOW * 1e9)) {
                count = 1;
                firstNs = now;
            } else {
                count++;
            }
            if (count >= SHAKE_COUNT) {
                shaken = true;
                count = 0;
                lastTriggerNs = now;
            }
        } else if (above && g < RESET_G) {
            above = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 사용하지 않음
    }
}
