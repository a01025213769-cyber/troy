# -*- coding: utf-8 -*-
"""
트로이 전쟁 — 배경 그림 생성기
================================

전투 · 컷신 배경 5장을 픽셀 아트 느낌으로 코드에서 그립니다.

    python tools/make_backdrops.py                 # app/src/main/res/drawable-nodpi 에 저장
    python tools/make_backdrops.py <저장할 폴더>

필요한 것: Python 3 + numpy + Pillow

· 그림 크기는 520 x 270 (게임 가상 화면 960 x 540 의 절반 해상도 + 좌우 여백).
  게임에서 2배로 늘려 그리므로 픽셀 하나가 2x2 로 보여 캐릭터 스프라이트와 어울립니다.
  좌우 여백(가상 좌표 40px씩)은 카메라가 살짝 따라가는 시차(parallax) 연출에 씁니다.
· 움직이는 부분(파도 반짝임, 깃발, 횃불, 별 깜빡임, 구름, 황금 사과)은 그림에 넣지 않고
  게임 코드(ui/Backdrop.java)가 그 위에 그립니다. 위치는 아래 좌표와 맞춰 두었습니다.
· 마지막에 순서 디더링(Bayer)으로 색 단계를 줄여 레트로 픽셀 느낌을 냅니다.

가상 좌표 x 는 그림 좌표로 hx = (x + 40) / 2, y 는 hy = y / 2 입니다.
바닥선(캐릭터 발) Theme.GROUND = 428 → 그림에서 y = 214.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

W, H = 520, 270
GY = 214           # 바닥선 (가상 428)

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0


# ======================================================================
#  기본 도구
# ======================================================================
def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64) / 255.0


def canvas():
    return np.zeros((H, W, 3))


def vgrad(img, y0, y1, stops):
    """y0~y1 구간을 위→아래 색 띠로 칠합니다. stops = [(0~1, '#색'), ...]"""
    ts = np.array([s[0] for s in stops])
    cs = np.array([hexc(s[1]) for s in stops])
    for y in range(max(0, y0), min(H, y1)):
        t = (y - y0) / max(1, (y1 - y0 - 1))
        col = np.array([np.interp(t, ts, cs[:, k]) for k in range(3)])
        img[y, :, :] = col


def blend(img, mask, color, alpha=1.0):
    """mask(0~1, HxW) 영역을 color 로 alpha 만큼 덮습니다."""
    a = (np.clip(mask, 0, 1) * alpha)[..., None]
    img[:] = img * (1 - a) + hexc(color) * a if isinstance(color, str) else img * (1 - a) + color * a


def add_light(img, mask, color, strength=1.0):
    img[:] = np.clip(img + (np.clip(mask, 0, 1) * strength)[..., None] * hexc(color), 0, 1)


def mask_from(draw_fn):
    m = Image.new('L', (W, H), 0)
    draw_fn(ImageDraw.Draw(m))
    return np.asarray(m, dtype=np.float64) / 255.0


def rect(img, x0, y0, x1, y1, color, alpha=1.0):
    x0, x1 = int(max(0, x0)), int(min(W, x1))
    y0, y1 = int(max(0, y0)), int(min(H, y1))
    if x1 <= x0 or y1 <= y0:
        return
    c = hexc(color)
    img[y0:y1, x0:x1] = img[y0:y1, x0:x1] * (1 - alpha) + c * alpha


def poly(img, pts, color, alpha=1.0):
    blend(img, mask_from(lambda d: d.polygon([tuple(p) for p in pts], fill=255)), color, alpha)


def ellipse(img, box, color, alpha=1.0):
    blend(img, mask_from(lambda d: d.ellipse(box, fill=255)), color, alpha)


def radial(cx, cy, r, power=2.0):
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
    return np.clip(1 - d, 0, 1) ** power


def hline(img, x0, x1, y, color, alpha=1.0):
    rect(img, x0, y, x1, y + 1, color, alpha)


def vline(img, x, y0, y1, color, alpha=1.0):
    rect(img, x, y0, x + 1, y1, color, alpha)


def ridge(seed, base, amps, freqs, x0=0, x1=W):
    """산·언덕 윤곽선 높이 (x 마다 y)"""
    rng = np.random.default_rng(seed)
    xs = np.arange(W)
    y = np.full(W, float(base))
    for a, f in zip(amps, freqs):
        y += a * np.sin(xs * f + rng.uniform(0, 6.28))
    return y


def fill_below(img, ys, y_bottom, color, x0=0, x1=W, alpha=1.0):
    yy = np.arange(H)[:, None]
    m = (yy >= ys[None, :]) & (yy < y_bottom)
    m[:, :x0] = False
    m[:, x1:] = False
    blend(img, m.astype(np.float64), color, alpha)


def speckle(img, y0, y1, color, density, seed, alpha=1.0, x0=0, x1=W):
    rng = np.random.default_rng(seed)
    n = int((y1 - y0) * (x1 - x0) * density)
    xs = rng.integers(x0, x1, n)
    ys = rng.integers(y0, y1, n)
    c = hexc(color)
    for x, y in zip(xs, ys):
        img[y, x] = img[y, x] * (1 - alpha) + c * alpha


def bricks(img, x0, y0, x1, y1, bw, bh, base, mortar, seed, var=0.07, light=None, shade=None):
    """돌벽 — 벽돌마다 밝기를 조금씩 다르게, 위쪽 모서리는 밝게 아래는 어둡게"""
    rng = np.random.default_rng(seed)
    x0, x1 = max(0, x0), min(W, x1)
    y0, y1 = max(0, y0), min(H, y1)
    if x1 <= x0 or y1 <= y0:
        return
    b = hexc(base)
    mo = hexc(mortar)
    lt = hexc(light) if light else np.clip(b * 1.18, 0, 1)
    sh = hexc(shade) if shade else b * 0.78
    row = 0
    y = y0
    while y < y1:
        off = (bw // 2) if row % 2 else 0
        x = x0 - off
        while x < x1:
            k = 1.0 + rng.uniform(-var, var)
            xa, xb = max(x0, x), min(x1, x + bw - 1)
            ya, yb = y, min(y1, y + bh - 1)
            if xb > xa and yb > ya:
                img[ya:yb, xa:xb] = np.clip(b * k, 0, 1)
                img[ya, xa:xb] = np.clip(lt * k, 0, 1)
                img[yb - 1, xa:xb] = np.clip(sh * k, 0, 1)
                img[ya:yb, xb - 1] = np.clip(sh * k * 1.05, 0, 1)
            if xb < x1:
                img[ya:min(y1, y + bh), xb:min(x1, xb + 1)] = mo
            x += bw
        if y + bh - 1 < y1:
            img[y + bh - 1:y + bh, x0:x1] = mo
        y += bh
        row += 1


def finalize(img, path, levels=26):
    """Bayer 순서 디더링으로 색 단계를 줄여 픽셀 아트 느낌으로 저장합니다."""
    step = 1.0 / (levels - 1)
    th = np.tile(BAYER, (H // 4 + 1, W // 4 + 1))[:H, :W][..., None]
    q = np.floor(np.clip(img, 0, 1) / step + th) * step
    out = (np.clip(q, 0, 1) * 255).round().astype(np.uint8)
    Image.fromarray(out, 'RGB').save(path, optimize=True)


def vx(x):
    """가상 좌표 x → 그림 좌표"""
    return (x + 40) / 2.0


# ======================================================================
#  0. 트로이 해안 — 새벽 상륙
# ======================================================================
def beach():
    img = canvas()
    vgrad(img, 0, 132, [(0, '#121c33'), (0.42, '#283a57'), (0.68, '#5e4c63'),
                        (0.86, '#b06c4c'), (1, '#e3954f')])
    # 해와 빛무리
    add_light(img, radial(418, 113, 90, 2.2), '#f0a050', 0.55)
    add_light(img, radial(418, 113, 38, 1.4), '#ffd08a', 0.6)
    ellipse(img, (396, 91, 440, 135), '#f7c56c')
    ellipse(img, (404, 97, 422, 109), '#ffe6ac', 0.7)
    # 길게 늘어진 새벽 구름 — 아래쪽 가장자리가 햇빛에 물듭니다
    for (cx, cy, w, h, seed) in ((120, 58, 120, 9, 1), (300, 40, 150, 7, 2), (470, 76, 90, 6, 3),
                                 (40, 92, 80, 5, 4), (250, 88, 110, 5, 5)):
        rng = np.random.default_rng(seed)
        for k in range(4):
            dx = rng.uniform(-w * 0.3, w * 0.3)
            ww = w * rng.uniform(0.35, 0.6)
            ellipse(img, (cx + dx - ww, cy - h * 0.6 + k, cx + dx + ww, cy + h * 0.6 + k), '#3c3d5c', 0.9)
        hline(img, int(cx - w * 0.55), int(cx + w * 0.55), int(cy + h * 0.6 + 3), '#c07862', 0.8)
        hline(img, int(cx - w * 0.4), int(cx + w * 0.4), int(cy + h * 0.6 + 4), '#e0956a', 0.5)
    # 먼 곶과 섬
    fill_below(img, ridge(10, 117, (6, 3), (0.025, 0.09)), 133, '#232c45', 0, 190)
    fill_below(img, ridge(11, 125, (3, 1.5), (0.05, 0.13)), 133, '#2e3852', 300, 372)
    # 바다
    vgrad(img, 132, 166, [(0, '#3d5f76'), (1, '#1c2d3c')])
    rng = np.random.default_rng(20)
    for _ in range(170):
        y = rng.integers(133, 165)
        x = rng.integers(0, W)
        ln = rng.integers(3, 14)
        hline(img, x, x + ln, y, '#7d9cb0' if y < 150 else '#557489', 0.6)
    for y in range(133, 166):                     # 해 아래 반사광 기둥
        half = 6 + (y - 132) * 0.7
        if y % 2 == 0:
            hline(img, int(418 - half), int(418 + half), y, '#e7a462', 0.45 - (y - 132) * 0.008)
    # 그리스 함대 (실루엣)
    for i in range(5):
        sx = vx(90 + i * 170)
        sy = 143 + (i % 2) * 5
        s = 0.42 + (i % 3) * 0.05
        ship(img, sx, sy, s)
    # 파도가 닿는 젖은 모래
    vgrad(img, 165, 207, [(0, '#4b3c2b'), (1, '#8a6b44')])
    for y in (166, 168):
        for x in range(0, W, 2):
            if rng.random() < 0.55:
                img[y, x] = img[y, x] * 0.4 + hexc('#cfc7ae') * 0.6
    for _ in range(14):                              # 물이 고인 자리 — 하늘빛이 비칩니다
        x = rng.integers(0, W - 40)
        y = rng.integers(172, 200)
        w = rng.integers(14, 40)
        ellipse(img, (x, y, x + w, y + 3), '#6e6a72', 0.55)
        hline(img, x + 3, x + w - 3, y + 1, '#c58a5e', 0.5)
    speckle(img, 170, 205, '#3e3124', 0.03, 23)
    # 마른 모래 (바닥)
    vgrad(img, 207, H, [(0, '#b8915c'), (0.5, '#a98250'), (1, '#8e6c43')])
    speckle(img, 207, H, '#c9a46c', 0.05, 21)
    speckle(img, 207, H, '#8a6840', 0.05, 22)
    for i in range(16):                              # 바람 자국
        x = (i * 37 + 13) % W
        y = 222 + (i * 11) % 44
        hline(img, x, x + 16, y, '#93714a', 0.8)
        hline(img, x + 2, x + 14, y - 1, '#c8a26a', 0.6)
    hline(img, 0, W, GY - 1, '#7a5c3a', 0.35)
    # 모래에 꽂힌 창과 부서진 방패
    for i, x in enumerate((50, 128, 226, 312, 400, 486)):
        tilt = (-4, 3, -2, 5, -3, 2)[i]
        for k in range(32):
            yy = 210 - k
            xx = x + tilt * k / 32.0
            img[int(yy), int(xx)] = hexc('#5f4a30')
        poly(img, [(x + tilt, 176), (x + tilt - 2, 181), (x + tilt + 2, 181)], '#b9b2a0')
    ellipse(img, (160, 204, 180, 214), '#6d4a2a')
    ellipse(img, (164, 206, 176, 212), '#8a6236')
    return img


def ship(img, x, y, s):
    col = '#111b26'
    hull = [(x - 60 * s, y - 8 * s), (x + 52 * s, y - 8 * s), (x + 74 * s, y - 22 * s),
            (x + 62 * s, y + 4 * s), (x - 52 * s, y + 6 * s), (x - 66 * s, y - 10 * s)]
    poly(img, hull, col)
    for k in range(7):                               # 노
        ox = x - 40 * s + k * 13 * s
        poly(img, [(ox, y), (ox + 1, y), (ox - 5 * s, y + 12 * s), (ox - 6 * s, y + 12 * s)], col)
    rect(img, x - 1, y - 62 * s, x + 1, y - 6 * s, col)
    rect(img, x - 28 * s, y - 58 * s, x + 28 * s, y - 24 * s, '#18232f')
    rect(img, x + 26 * s, y - 58 * s, x + 28 * s, y - 24 * s, '#7a4a3a', 0.7)   # 햇빛 받는 돛 가장자리


# ======================================================================
#  1. 트로이 성벽 — 10년째 포위전
# ======================================================================
WALL_TOP = 75          # 가상 150


def wall():
    img = canvas()
    vgrad(img, 0, 150, [(0, '#1f2533'), (0.55, '#454a5b'), (1, '#7d7064')])
    rng = np.random.default_rng(30)
    for k in range(7):                               # 흐린 구름 띠
        y = 12 + k * 12 + rng.integers(-3, 4)
        x = rng.integers(-60, W)
        ellipse(img, (x, y, x + rng.integers(120, 260), y + rng.integers(6, 12)), '#5b6070', 0.35)
    # 멀리 보이는 도시
    for i in range(14):
        x = i * 38 + rng.integers(-6, 6)
        h = 20 + (i * 7) % 26
        rect(img, x, 152 - h, x + 30, 153, '#454150')
        rect(img, x, 152 - h, x + 30, 152 - h + 1, '#5a5562')
        for wy in range(152 - h + 5, 150, 7):
            for wx in range(x + 4, x + 27, 8):
                if rng.random() < 0.35:
                    rect(img, wx, wy, wx + 2, wy + 3, '#2c2934')
    # 거대한 성벽
    bricks(img, 0, WALL_TOP, W, 209, 22, 10, '#6b5e4f', '#463c31', 31, var=0.08)
    sh = np.linspace(0, 1, 209 - WALL_TOP)
    img[WALL_TOP:209] *= (1 - 0.25 * sh)[:, None, None]
    for i in range(30):                              # 빗물 자국
        x = rng.integers(0, W)
        rect(img, x, WALL_TOP + 4, x + 1, WALL_TOP + rng.integers(20, 70), '#4f4539', 0.5)
    # 성가퀴와 통로 그림자
    for i in range(16):
        x = 20 + i * 35
        bricks(img, x, 61, x + 21, WALL_TOP + 1, 11, 7, '#7a6b59', '#4e4335', 40 + i, var=0.06)
    rect(img, 0, WALL_TOP, W, WALL_TOP + 3, '#3e352b', 0.8)
    # 탑 두 개
    for tx in (vx(130), vx(820)):
        tx = int(tx)
        bricks(img, tx - 27, 48, tx + 27, 209, 18, 10, '#766656', '#4a3f33', int(tx), var=0.07)
        img[48:209, tx - 27:tx + 27] *= (1 - 0.18 * np.linspace(0, 1, 161))[:, None, None]
        for k in range(5):
            mx = tx - 27 + k * 12
            bricks(img, mx, 35, mx + 8, 49, 8, 7, '#83735f', '#4e4335', tx + k, var=0.05)
        rect(img, tx - 7, 75, tx + 7, 98, '#2a221b')                 # 창
        rect(img, tx - 5, 77, tx + 5, 98, '#1c1712')
        rect(img, tx - 2, 120, tx + 2, 136, '#2a221b')               # 화살 구멍
        rect(img, tx - 27, 48, tx - 25, 209, '#8e7d68', 0.6)          # 빛 받는 모서리
    # 깃대 (깃발은 게임 코드가 그 위에 펄럭이게 그립니다)
    for x in (250, 490, 730):
        hx = int(vx(x))
        rect(img, hx - 1, 31, hx + 1, 63, '#4a3f33')
    # 바닥 — 다져진 흙
    vgrad(img, 209, H, [(0, '#5c5040'), (0.15, '#6e6049'), (1, '#544836')])
    rect(img, 0, 205, W, 210, '#3a3127', 0.8)
    speckle(img, 210, H, '#82735a', 0.04, 32)
    speckle(img, 210, H, '#4a4032', 0.05, 33)
    for i in range(12):                              # 돌멩이
        x = (i * 47 + 11) % W
        y = 220 + (i * 13) % 44
        ellipse(img, (x, y, x + 6, y + 3), '#4c4235')
        hline(img, x + 1, x + 5, y, '#8a7a62', 0.8)
    return img


# ======================================================================
#  2. 성문 앞 야간 — 결투 / 함락
# ======================================================================
def night_gate():
    img = canvas()
    vgrad(img, 0, 165, [(0, '#060a14'), (0.5, '#161424'), (0.85, '#2a1822'), (1, '#45201e')])
    rng = np.random.default_rng(50)
    for _ in range(90):                              # 흐린 별 (밝은 별은 게임 코드가 깜빡이게)
        x, y = rng.integers(0, W), rng.integers(0, 105)
        img[y, x] = np.maximum(img[y, x], hexc('#6d6c82') * rng.uniform(0.6, 1.0))
    ellipse(img, (64, 18, 86, 40), '#d9d3c0')        # 초승달
    ellipse(img, (70, 15, 92, 37), '#070b15')
    # 연기 기둥
    for sx, seed in ((120, 51), (400, 52), (250, 53)):
        r2 = np.random.default_rng(seed)
        for k in range(70):
            u = k / 70.0
            y = 120 - u * 115 + r2.uniform(-3, 3)
            x = sx + u * 40 * (1 if seed % 2 else -1) + r2.uniform(-6, 6) * (0.4 + u)
            r = 5 + u * 18 + r2.uniform(0, 4)
            ellipse(img, (x - r, y - r * 0.7, x + r, y + r * 0.7), '#2b2532', 0.10 * (1 - u) + 0.03)
    # 불타는 도시의 붉은 하늘빛
    add_light(img, radial(130, 160, 120, 1.6), '#c0482a', 0.35)
    add_light(img, radial(400, 160, 140, 1.6), '#c0482a', 0.35)
    # 도시 실루엣과 불 켜진 창
    for i in range(17):
        x = i * 32 + rng.integers(-6, 6)
        h = 62 + (i * 13) % 44
        rect(img, x, 160 - h, x + 28, 166, '#141020')
        rect(img, x + 8, 160 - h - 9, x + 18, 160 - h + 2, '#141020')
        for wy in range(160 - h + 5, 158, 7):
            for wx in range(x + 4, x + 25, 7):
                if rng.random() < 0.22:
                    rect(img, wx, wy, wx + 2, wy + 3, '#e0702e' if rng.random() < 0.7 else '#ffc070')
    # 양옆으로 뻗은 성벽
    bricks(img, 0, 100, W, 209, 20, 9, '#2e2624', '#1b1615', 54, var=0.08)
    for i in range(16):
        x = i * 34
        bricks(img, x, 90, x + 20, 101, 10, 6, '#352c29', '#1b1615', 60 + i, var=0.05)
    # 성문 누각
    gx = int(vx(480))
    bricks(img, gx - 75, 60, gx + 75, 209, 18, 10, '#3d322d', '#201a17', 55, var=0.07)
    for k in range(7):
        mx = gx - 75 + k * 22
        bricks(img, mx, 48, mx + 14, 61, 7, 7, '#463a33', '#201a17', 70 + k, var=0.05)
    # 아치형 문
    arch = mask_from(lambda d: (d.ellipse((gx - 55, 78, gx + 55, 130), fill=255),
                                d.rectangle((gx - 55, 104, gx + 55, 209), fill=255)))
    blend(img, arch, '#2b1c14')
    inner = mask_from(lambda d: (d.ellipse((gx - 50, 83, gx + 50, 132), fill=255),
                                 d.rectangle((gx - 50, 108, gx + 50, 209), fill=255)))
    blend(img, inner, '#3b2618')
    for k in range(-50, 51, 10):                     # 나무 판자
        vline(img, gx + k, 84, 209, '#24170f', 0.9)
    for y in (122, 160, 192):                        # 쇠띠
        rect(img, gx - 50, y, gx + 50, y + 3, '#2a2a2e')
        for k in range(-46, 47, 12):
            rect(img, gx + k, y + 1, gx + k + 1, y + 2, '#6a6a70')
    vline(img, gx, 84, 209, '#120c08')
    rect(img, gx - 1, 110, gx + 1, 209, '#d8682a', 0.35)              # 문틈으로 새는 불빛
    # 아치 테두리 돌
    for a in np.linspace(np.pi, 2 * np.pi, 15):
        x = gx + np.cos(a) * 58
        y = 104 + np.sin(a) * 28
        ellipse(img, (x - 4, y - 3, x + 4, y + 3), '#4e4036')
    # 횃대 (불꽃은 게임 코드가 그립니다)
    for tx in (vx(290), vx(670)):
        tx = int(tx)
        rect(img, tx - 2, 150, tx + 2, 209, '#3a2e24')
        rect(img, tx - 4, 148, tx + 4, 152, '#2a2219')
        add_light(img, radial(tx, 146, 46, 1.8), '#ff8a3c', 0.3)
    # 바닥 — 불빛이 비치는 돌길
    vgrad(img, 209, H, [(0, '#2a221c'), (0.2, '#3a3129'), (1, '#211b16')])
    r3 = np.random.default_rng(56)
    for row in range(8):
        y = 211 + row * 7
        off = (row % 2) * 9
        for x in range(-off, W, 18):
            w = 15 + r3.integers(-2, 3)
            ellipse(img, (x, y, x + w, y + 5), '#463a30', 0.9)
            hline(img, x + 3, x + w - 3, y, '#7a5a3c', 0.6)
    add_light(img, radial(gx, 215, 160, 1.3), '#d8642c', 0.16)
    return img


# ======================================================================
#  3. 신들의 산 — 프롤로그 (황금 사과)
# ======================================================================
def olympus():
    img = canvas()
    vgrad(img, 0, 214, [(0, '#1d1534'), (0.37, '#4e3558'), (0.66, '#a2645a'), (0.82, '#c98458'),
                        (1, '#8a5a5a')])
    # 먼 산맥 (올림포스) + 눈 덮인 봉우리
    ys = ridge(70, 120, (16, 9, 4), (0.018, 0.047, 0.13))
    fill_below(img, ys, 200, '#4c3654')
    snow = np.zeros((H, W))
    for x in range(W):
        y0 = int(ys[x])
        if y0 < 116:
            snow[y0:min(H, y0 + int((116 - y0) * 0.6) + 1), x] = 1
    blend(img, snow, '#d7c6d6', 0.85)
    ys2 = ridge(71, 150, (8, 4), (0.03, 0.09))
    fill_below(img, ys2, 200, '#3a2843')
    # 구름 바다
    rng = np.random.default_rng(72)
    for k in range(44):
        x = rng.integers(-40, W)
        y = 150 + rng.integers(-6, 46)
        w = rng.integers(30, 80)
        ellipse(img, (x - w, y - 7, x + w, y + 7), '#e0a090', 0.35)
        ellipse(img, (x - w * 0.7, y - 11, x + w * 0.6, y + 3), '#f0c0a0', 0.25)
    # 신전 — 기단
    rect(img, 0, 198, W, 214, '#cbb8a0')
    for k, y in enumerate((198, 203, 208)):
        hline(img, 0, W, y, '#ece0cc', 0.9)
        hline(img, 0, W, y + 4, '#9c8a76', 0.9)
    # 박공과 처마
    poly(img, [(35, 56), (260, 26), (485, 56)], '#d9c8ae')
    poly(img, [(55, 54), (260, 32), (465, 54)], '#bfa98e')
    for k in range(9):                               # 박공 부조
        x = 130 + k * 30
        rect(img, x + 3, 46 - abs(k - 4) * 2, x + 7, 53, '#a8927a', 0.6)
    rect(img, 30, 56, 490, 72, '#e3d3b8')
    hline(img, 30, 490, 56, '#f6ecda')
    hline(img, 30, 490, 63, '#b8a58a')
    for x in range(34, 490, 12):                     # 트리글리프
        rect(img, x, 64, x + 6, 71, '#c7b498')
    hline(img, 30, 490, 71, '#8f7d66')
    # 기둥 6개
    for i in range(6):
        cx = 55 + i * 82
        rect(img, cx - 4, 72, cx + 21, 78, '#eadcc4')                 # 기둥머리
        rect(img, cx - 2, 78, cx + 19, 80, '#b9a68c')
        rect(img, cx, 80, cx + 17, 198, '#dccbb0')
        rect(img, cx, 80, cx + 4, 198, '#f1e5d0')                     # 빛 받는 쪽
        rect(img, cx + 13, 80, cx + 17, 198, '#a8957c')               # 그늘
        for fx in (cx + 6, cx + 9, cx + 12):                         # 세로 홈
            vline(img, fx, 82, 196, '#c2ae92', 0.8)
        rect(img, cx - 3, 193, cx + 20, 198, '#e6d8c0')               # 주춧돌
    # 대리석 바닥
    vgrad(img, 214, H, [(0, '#7d6a74'), (1, '#4a3a44')])
    for y in (222, 234, 250):
        hline(img, 0, W, y, '#9a8790', 0.7)
    for x in range(-200, W + 200, 40):
        for y in range(214, H):
            xx = int(260 + (x - 260) * (1 + (y - 214) / 40.0))
            if 0 <= xx < W:
                img[y, xx] = img[y, xx] * 0.5 + hexc('#5e4c56') * 0.5
    add_light(img, radial(260, 125, 130, 1.5), '#ffd36b', 0.22)      # 황금 사과의 빛
    return img


# ======================================================================
#  4. 귀향의 바다 — 엔딩
# ======================================================================
def sea():
    img = canvas()
    vgrad(img, 0, 124, [(0, '#1a1636'), (0.5, '#5a3456'), (0.85, '#c25a42'), (1, '#e27a44')])
    add_light(img, radial(260, 122, 150, 2.0), '#f08a4a', 0.45)
    rng = np.random.default_rng(90)
    for k in range(9):                               # 노을에 물든 구름 줄기
        y = 30 + k * 9 + rng.integers(-3, 3)
        x = rng.integers(-40, W - 60)
        w = rng.integers(60, 170)
        ellipse(img, (x, y, x + w, y + 4), '#2c2142', 0.8)
        hline(img, x + 6, x + w - 6, y + 4, '#d8785a', 0.7)
    sun = mask_from(lambda d: d.ellipse((233, 95, 287, 149), fill=255))
    sun[124:, :] = 0
    blend(img, sun, '#f8b86a')
    blend(img, mask_from(lambda d: d.ellipse((243, 103, 266, 116), fill=255)) * (sun > 0),
          '#ffe2a8', 0.6)
    # 수평선 너머 이타카 섬
    fill_below(img, ridge(91, 118, (4, 2), (0.04, 0.11)), 125, '#3a2442', 380, 480)
    fill_below(img, ridge(92, 121, (2, 1), (0.06, 0.15)), 125, '#40284a', 20, 90)
    # 바다
    vgrad(img, 124, H, [(0, '#3a4a66'), (0.25, '#2a3d57'), (1, '#0e1828')])
    for y in range(125, H):
        n = int(5 + (y - 124) * 0.35)
        for _ in range(n):
            x = rng.integers(0, W)
            ln = rng.integers(3, 6 + (y - 124) // 8)
            hline(img, x, x + ln, y, '#4c6482' if rng.random() < 0.6 else '#0b1422', 0.6)
    for y in range(125, H, 2):                       # 해 아래 반사광 (흔들리는 빛은 게임 코드가)
        half = 10 + (y - 124) * 0.45
        hline(img, int(260 - half), int(260 + half), y, '#e8a070', max(0.0, 0.42 - (y - 124) * 0.0026))
    return img


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        here, '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in (('bg_beach', beach), ('bg_wall', wall), ('bg_gate', night_gate),
                     ('bg_olympus', olympus), ('bg_sea', sea)):
        path = os.path.join(out_dir, name + '.png')
        finalize(fn(), path)
        print(name, os.path.getsize(path) // 1024, 'KB')


if __name__ == '__main__':
    main()
