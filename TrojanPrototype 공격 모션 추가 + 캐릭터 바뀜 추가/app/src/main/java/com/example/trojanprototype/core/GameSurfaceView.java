package com.example.trojanprototype.core;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.trojanprototype.ui.Theme;
import com.example.trojanprototype.util.Viewport;

/**
 * SurfaceView + 게임 루프 (Update / Render).
 *
 * 담당: 유장원 (시스템)
 *
 * ── 왜 SurfaceView 인가 ──────────────────────────────────
 * 일반 View 는 화면을 바꿀 때마다 invalidate() 로 다시 그리기를 "요청"만 하므로
 * 1초에 몇 번 그려질지 보장되지 않습니다.
 * SurfaceView 는 별도 스레드에서 직접 캔버스를 잠그고 그릴 수 있어
 * 60프레임 게임 루프를 안정적으로 돌릴 수 있습니다.
 *
 * ── 고정 시간 간격(fixed timestep) ───────────────────────
 * 기기가 느려도 게임 속도가 달라지지 않도록
 * 물리/판정 계산은 항상 1/60초 단위로 처리하고,
 * 밀린 만큼은 한 프레임에 여러 번 계산합니다.
 */
public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final float STEP = 1f / 60f;     // 계산 단위 (초)
    private static final int MAX_STEPS = 5;         // 한 프레임에 최대 계산 횟수
    private static final long TARGET_FRAME_MS = 16; // 목표 프레임 간격

    private final Viewport viewport = new Viewport();
    private final TouchInput input = new TouchInput();
    private final GameEngine engine;

    private GameThread thread;
    private volatile boolean surfaceReady = false;

    public GameSurfaceView(Context ctx) {
        super(ctx);
        engine = new GameEngine(ctx, input);
        getHolder().addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    public GameEngine engine() {
        return engine;
    }

    // ------------------------------------------------------------------
    //  터치 — UI 스레드에서 들어옵니다
    // ------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        input.onTouchEvent(e, viewport);
        return true;
    }

    // ------------------------------------------------------------------
    //  Surface 생명주기
    // ------------------------------------------------------------------
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        viewport.resize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopThread();
    }

    private void startThread() {
        if (thread != null && thread.isAlive()) return;
        thread = new GameThread();
        thread.running = true;
        thread.start();
    }

    private void stopThread() {
        GameThread t = thread;
        thread = null;
        if (t == null) return;
        t.running = false;
        boolean joined = false;
        while (!joined) {
            try {
                t.join(1000);
                joined = true;
            } catch (InterruptedException ignored) {
                // 다시 시도
            }
        }
    }

    // ------------------------------------------------------------------
    //  액티비티 생명주기와 연결
    // ------------------------------------------------------------------
    public void onActivityPause() {
        engine.onPause();
        stopThread();
    }

    public void onActivityResume() {
        engine.onResume();
        if (surfaceReady) startThread();
    }

    public void onActivityDestroy() {
        stopThread();
        engine.release();
    }

    // ------------------------------------------------------------------
    //  한 프레임 그리기
    // ------------------------------------------------------------------
    private void renderFrame(Canvas c) {
        // 화면 비율이 다른 기기에서 남는 부분(검은 띠)
        c.drawColor(0xFF000000);
        int save = c.save();
        viewport.apply(c);
        c.clipRect(0f, 0f, Theme.VW, Theme.VH);
        engine.render(c);
        c.restoreToCount(save);
    }

    // ==================================================================
    //  게임 스레드
    // ==================================================================
    private class GameThread extends Thread {

        volatile boolean running = true;

        @Override
        public void run() {
            long last = System.nanoTime();
            float accumulator = 0f;

            while (running) {
                long frameStart = System.nanoTime();
                float dt = (frameStart - last) / 1000000000f;
                last = frameStart;
                if (dt > 0.25f) dt = 0.25f;     // 앱이 잠깐 멈췄다 돌아온 경우 보정

                accumulator += dt;
                int steps = 0;
                while (accumulator >= STEP && steps < MAX_STEPS) {
                    engine.update(STEP);
                    accumulator -= STEP;
                    steps++;
                }
                if (steps >= MAX_STEPS) accumulator = 0f;   // 너무 밀리면 따라잡기 포기

                SurfaceHolder holder = getHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        synchronized (holder) {
                            renderFrame(canvas);
                        }
                    }
                } catch (Throwable ignored) {
                    // Surface 가 사라지는 순간에는 그리기를 건너뜁니다.
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                long usedMs = (System.nanoTime() - frameStart) / 1000000L;
                long sleepMs = TARGET_FRAME_MS - usedMs;
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
