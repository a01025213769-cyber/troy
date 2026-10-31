package com.example.trojanprototype.util;

import android.graphics.Canvas;

import com.example.trojanprototype.ui.Theme;

/**
 * 가상 화면(960x540)과 실제 기기 화면 사이의 좌표 변환 담당.
 *
 * 담당: 유장원 (시스템)
 * 기기마다 해상도가 다르므로, 게임 코드는 전부 960x540 기준으로 좌표를 쓰고
 * 이 클래스가 비율을 유지한 채 확대/축소 + 가운데 정렬을 해 줍니다.
 * (화면 비율이 다르면 위아래 또는 좌우에 검은 띠가 생깁니다.)
 */
public class Viewport {

    public float scale = 1f;
    public float offX = 0f;
    public float offY = 0f;
    public int screenW = 0;
    public int screenH = 0;

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        screenW = w;
        screenH = h;
        float sx = w / Theme.VW;
        float sy = h / Theme.VH;
        scale = Math.min(sx, sy);
        offX = (w - Theme.VW * scale) * 0.5f;
        offY = (h - Theme.VH * scale) * 0.5f;
    }

    /** 캔버스에 변환을 적용합니다. 호출 전에 canvas.save() 를 해 두세요. */
    public void apply(Canvas c) {
        c.translate(offX, offY);
        c.scale(scale, scale);
    }

    /** 터치한 실제 좌표를 가상 화면 좌표로 바꿉니다. */
    public float toVirtualX(float screenX) {
        return (screenX - offX) / scale;
    }

    public float toVirtualY(float screenY) {
        return (screenY - offY) / scale;
    }
}
