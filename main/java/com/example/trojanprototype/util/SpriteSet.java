package com.example.trojanprototype.util;

import android.graphics.Bitmap;

/**
 * 캐릭터 한 명의 동작 프레임 묶음.
 *
 * 담당: 유장원 (시스템) · 그림은 심신
 *
 * ── 프레임 구성 (res/drawable-nodpi/<이름>_f0 ~ _f5.png) ─────
 *   f0  대기 1
 *   f1  대기 2 (숨쉬기 / 발 바꾸기)
 *   f2  걷기
 *   f3  달리기 (회피·돌진에도 사용)
 *   f4  공격   ← 무기가 오른쪽으로 길게 뻗은 프레임
 *   f5  방어   ← 방패를 든 프레임
 *   f6~  (선택) 기술 전용 동작. 검 휘두르기처럼 여러 장이 필요할 때 씁니다.
 *
 * ── 그림을 어디에 맞춰 그리는가 ──────────────────────────
 * 프레임 6장은 모두 같은 크기의 캔버스를 쓰고, 아래 두 가지가 지켜져 있습니다.
 *   · 캔버스 아래쪽 끝 = 발이 닿는 바닥
 *   · 캔버스 가로 중심 ≒ 몸의 중심
 * 그래서 "캔버스 아래를 바닥선에, 몸 중심을 캐릭터 좌표에" 맞춰 통째로 그리면
 * 프레임이 바뀌어도 캐릭터가 덜컹거리지 않습니다.
 *
 * 공격 프레임만 무기 때문에 캔버스 오른쪽 끝까지 내용이 차 있는데,
 * 캔버스를 통째로 그리므로 창·화살이 자연스럽게 앞으로 뻗어 나갑니다.
 *
 * ── 크기 맞추기 ──────────────────────────────────────────
 * 캐릭터마다 캔버스 여백이 달라서, 캔버스 높이를 그대로 쓰면
 * 어떤 캐릭터는 작게 보입니다. 그래서 f0(대기)에서 실제 몸이 차지하는
 * 높이를 재어 두고, 그 몸 높이가 원하는 키가 되도록 배율을 정합니다.
 */
public class SpriteSet {

    /** 프레임 번호 (위 설명 참고) */
    public static final int F_IDLE_A = 0;
    public static final int F_IDLE_B = 1;
    public static final int F_WALK = 2;
    public static final int F_RUN = 3;
    public static final int F_ATTACK = 4;
    public static final int F_GUARD = 5;
    /**
     * 6번부터는 "추가 동작" 칸입니다.
     * 기술마다 다른 그림이 필요할 때(예: 오디세우스의 검 휘두르기) 여기에 넣고
     * GameData 의 Move 에서 프레임 번호로 지정합니다.
     */
    public static final int FRAME_COUNT = 16;

    /** 동작 프레임. 없는 칸은 null 입니다. */
    public final Bitmap[] frames = new Bitmap[FRAME_COUNT];
    /** 프레임이 없을 때 쓰는 한 장짜리 기본 그림 */
    public Bitmap base;

    /** 프레임 캔버스 크기 */
    public int canvasW = 1;
    public int canvasH = 1;
    /** 캔버스 안에서 몸의 중심이 되는 가로 위치 */
    public float anchorX = 0.5f;
    /** 캔버스 안에서 몸이 실제로 차지하는 높이 */
    public float bodyHeight = 1f;

    /** 동작 프레임을 하나라도 불러왔는지 */
    public boolean animated = false;

    /** 몸 높이를 targetHeight 로 만들기 위한 배율 */
    public float scaleFor(float targetHeight) {
        if (bodyHeight <= 0f) return 1f;
        return targetHeight / bodyHeight;
    }

    /** 그려질 캔버스 전체 높이 */
    public float drawHeight(float targetHeight) {
        return canvasH * scaleFor(targetHeight);
    }

    /** 그려질 캔버스 전체 너비 */
    public float drawWidth(float targetHeight) {
        return canvasW * scaleFor(targetHeight);
    }

    /** 캐릭터 좌표(x)에서 캔버스 왼쪽 끝까지의 거리 */
    public float leftOffset(float targetHeight) {
        return anchorX * scaleFor(targetHeight);
    }

    /**
     * 번호에 해당하는 프레임. 없으면 가까운 프레임이나 기본 그림으로 대체합니다.
     * (심신이 아직 프레임을 안 만든 캐릭터가 있어도 게임이 멈추지 않도록)
     */
    public Bitmap frame(int index) {
        if (index < 0 || index >= FRAME_COUNT) index = F_IDLE_A;
        if (frames[index] != null) return frames[index];
        for (int i = 0; i < FRAME_COUNT; i++) {
            if (frames[i] != null) return frames[i];
        }
        return base;
    }
}
