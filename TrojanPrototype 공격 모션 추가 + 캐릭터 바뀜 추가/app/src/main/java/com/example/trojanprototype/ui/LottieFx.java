package com.example.trojanprototype.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieResult;
import com.example.trojanprototype.util.Assets;

/**
 * Lottie 벡터 애니메이션 재생 (외부 라이브러리: com.airbnb.android:lottie).
 *
 * 담당: 유장원 (시스템) · 연출 심신
 *
 * 보통 Lottie 는 LottieAnimationView(View)에 붙여 쓰지만, 이 게임은 SurfaceView 에
 * Canvas 로 직접 그리므로 LottieDrawable 을 만들어 두고
 * 매 프레임 "진행도(progress)"를 정한 뒤 캔버스에 그립니다.
 *
 *   res/raw/lottie_victory.json  승리 연출 (금빛 햇살 · 퍼지는 고리 · 반짝이)
 *                                tools/make_lottie.py 로 만들었고, After Effects 나
 *                                LottieFiles 에서 만든 json 으로 바꿔 넣어도 됩니다.
 *
 * 파일이 없거나 읽기에 실패하면 아무것도 그리지 않고 게임은 그대로 진행됩니다.
 */
public class LottieFx {

    private static final String TAG = "LottieFx";

    /** 등장 구간이 끝나는 프레임 — 그 뒤로는 [LOOP_START, END] 를 반복합니다. */
    private static final float LOOP_START = 60f;

    private LottieDrawable victory;
    private float victoryEnd = 240f;
    private float victoryFps = 60f;

    public LottieFx(Context ctx, Assets assets) {
        try {
            int id = assets.rawId("lottie_victory");
            if (id == 0) return;
            LottieResult<LottieComposition> r = LottieCompositionFactory.fromRawResSync(ctx, id);
            LottieComposition comp = r.getValue();
            if (comp == null) {
                Log.w(TAG, "lottie_victory 를 읽지 못했습니다", r.getException());
                return;
            }
            victory = new LottieDrawable();
            victory.setComposition(comp);
            victoryEnd = comp.getEndFrame();
            victoryFps = comp.getFrameRate();
        } catch (Throwable t) {
            Log.w(TAG, "Lottie 초기화 실패", t);
            victory = null;
        }
    }

    /**
     * 승리 연출을 (cx, cy) 를 중심으로 size 크기에 맞춰 그립니다.
     * @param time 연출이 시작된 뒤 흐른 시간(초)
     */
    public void drawVictory(Canvas c, float cx, float cy, float size, float time) {
        if (victory == null) return;
        float frame = time * victoryFps;
        if (frame > victoryEnd) {
            float span = victoryEnd - LOOP_START;
            frame = LOOP_START + (frame - LOOP_START) % span;
        }
        // LottieDrawable 은 bounds 의 왼쪽 위 좌표까지 함께 배율을 곱해 버리므로,
        // bounds 는 (0,0) 에서 시작하게 두고 캔버스를 직접 옮겨서 그립니다.
        int save = c.save();
        try {
            victory.setProgress(frame / victoryEnd);
            int s = (int) size;
            victory.setBounds(0, 0, s, s);
            c.translate(cx - size * 0.5f, cy - size * 0.5f);
            victory.draw(c);
        } catch (Throwable ignored) {
            // 그리기에 실패해도 결과 화면은 그대로 보여 줍니다.
        } finally {
            c.restoreToCount(save);
        }
    }
}
