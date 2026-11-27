# -*- coding: utf-8 -*-
"""
트로이 전쟁 — 타이틀(홈) 화면 픽셀 그림 생성기
=============================================

배경과 메뉴 카드 그림을 게임 스프라이트(res/drawable-nodpi/*_sprite_f*.png)와
전투 배경(make_backdrops.py)과 같은 방식 — 반해상도 + Bayer 순서 디더링 — 으로 그립니다.

    python tools/make_title_pixel.py <res/drawable-nodpi 폴더>

필요한 것: Python 3 + numpy + Pillow

· 모든 그림은 가상 화면(960x540)의 절반 해상도로 그리고, 게임에서 2배로 늘려
  필터 없이(픽셀 그대로) 그립니다. 픽셀 하나가 2x2 로 보여 전투 화면과 어울립니다.
· 캐릭터는 실제 게임 스프라이트를 그대로 가져와 붙입니다.
  스프라이트를 바꾸고 이 스크립트를 다시 돌리면 타이틀 그림도 같이 바뀝니다.
· 글자는 그림에 넣지 않습니다 (ui/Screens.java 가 직접 씁니다).

만드는 파일
    title_bg.png             480x270  전체 배경 — 성벽 · 불타는 트로이 · 트로이 목마 · 병사들 · 아킬레우스
    title_card_campaign.png  186x58   캠페인 시작 — 돌격하는 그리스군
    title_card_multi.png     186x48   멀티플레이 — 바다 위 함대
    title_card_heroes.png     92x49   영웅 — 헥토르
    title_card_army.png       90x49   군대 관리 — 방패를 든 그리스 중장보병 대열
    title_news.png           127x36   소식 카드 — 불타는 도시
    title_logo_helmet.png             로고 옆 투구 (헥토르 스프라이트의 머리)
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0
RES = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')


# ======================================================================
#  기본 도구 (make_backdrops.py 와 같은 방식, 크기만 그림마다 다름)
# ======================================================================
def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64) / 255.0


class Pic:
    def __init__(self, w, h):
        self.W, self.H = w, h
        self.img = np.zeros((h, w, 3))

    # -- 칠하기
    def vgrad(self, y0, y1, stops, x0=0, x1=None):
        x1 = self.W if x1 is None else x1
        ts = np.array([s[0] for s in stops])
        cs = np.array([hexc(s[1]) for s in stops])
        for y in range(max(0, y0), min(self.H, y1)):
            t = (y - y0) / max(1, (y1 - y0 - 1))
            self.img[y, x0:x1, :] = [np.interp(t, ts, cs[:, k]) for k in range(3)]

    def blend(self, mask, color, alpha=1.0):
        a = (np.clip(mask, 0, 1) * alpha)[..., None]
        c = hexc(color) if isinstance(color, str) else color
        self.img[:] = self.img * (1 - a) + c * a

    def light(self, mask, color, strength=1.0):
        self.img[:] = np.clip(self.img + (np.clip(mask, 0, 1) * strength)[..., None] * hexc(color), 0, 1)

    def mask(self, fn):
        m = Image.new('L', (self.W, self.H), 0)
        fn(ImageDraw.Draw(m))
        return np.asarray(m, dtype=np.float64) / 255.0

    def rect(self, x0, y0, x1, y1, color, alpha=1.0):
        x0, x1 = int(max(0, x0)), int(min(self.W, x1))
        y0, y1 = int(max(0, y0)), int(min(self.H, y1))
        if x1 <= x0 or y1 <= y0:
            return
        self.img[y0:y1, x0:x1] = self.img[y0:y1, x0:x1] * (1 - alpha) + hexc(color) * alpha

    def poly(self, pts, color, alpha=1.0):
        self.blend(self.mask(lambda d: d.polygon([tuple(p) for p in pts], fill=255)), color, alpha)

    def ellipse(self, box, color, alpha=1.0):
        self.blend(self.mask(lambda d: d.ellipse(box, fill=255)), color, alpha)

    def radial(self, cx, cy, r, power=2.0, sy=1.0):
        yy, xx = np.mgrid[0:self.H, 0:self.W]
        d = np.sqrt((xx - cx) ** 2 + ((yy - cy) / sy) ** 2) / r
        return np.clip(1 - d, 0, 1) ** power

    def hline(self, x0, x1, y, color, alpha=1.0):
        self.rect(x0, y, x1, y + 1, color, alpha)

    def vline(self, x, y0, y1, color, alpha=1.0):
        self.rect(x, y0, x + 1, y1, color, alpha)

    def px(self, x, y, color, alpha=1.0):
        x, y = int(x), int(y)
        if 0 <= x < self.W and 0 <= y < self.H:
            self.img[y, x] = self.img[y, x] * (1 - alpha) + hexc(color) * alpha

    def ridge(self, seed, base, amps, freqs):
        rng = np.random.default_rng(seed)
        xs = np.arange(self.W)
        y = np.full(self.W, float(base))
        for a, f in zip(amps, freqs):
            y += a * np.sin(xs * f + rng.uniform(0, 6.28))
        return y

    def fill_below(self, ys, y_bottom, color, x0=0, x1=None, alpha=1.0):
        x1 = self.W if x1 is None else x1
        yy = np.arange(self.H)[:, None]
        m = (yy >= ys[None, :]) & (yy < y_bottom)
        m[:, :x0] = False
        m[:, x1:] = False
        self.blend(m.astype(np.float64), color, alpha)

    def speckle(self, y0, y1, color, density, seed, alpha=1.0, x0=0, x1=None):
        x1 = self.W if x1 is None else x1
        rng = np.random.default_rng(seed)
        n = int((y1 - y0) * (x1 - x0) * density)
        for x, y in zip(rng.integers(x0, x1, n), rng.integers(y0, y1, n)):
            self.px(x, y, color, alpha)

    def bricks(self, x0, y0, x1, y1, bw, bh, base, mortar, seed, var=0.07):
        rng = np.random.default_rng(seed)
        x0, x1 = max(0, int(x0)), min(self.W, int(x1))
        y0, y1 = max(0, int(y0)), min(self.H, int(y1))
        if x1 <= x0 or y1 <= y0:
            return
        b, mo = hexc(base), hexc(mortar)
        lt, sh = np.clip(b * 1.18, 0, 1), b * 0.78
        row, y = 0, y0
        while y < y1:
            x = x0 - ((bw // 2) if row % 2 else 0)
            while x < x1:
                k = 1.0 + rng.uniform(-var, var)
                xa, xb = max(x0, x), min(x1, x + bw - 1)
                ya, yb = y, min(y1, y + bh - 1)
                if xb > xa and yb > ya:
                    self.img[ya:yb, xa:xb] = np.clip(b * k, 0, 1)
                    self.img[ya, xa:xb] = np.clip(lt * k, 0, 1)
                    self.img[yb - 1, xa:xb] = np.clip(sh * k, 0, 1)
                if xb < x1:
                    self.img[ya:min(y1, y + bh), xb:min(x1, xb + 1)] = mo
                x += bw
            if y + bh - 1 < y1:
                self.img[y + bh - 1:y + bh, x0:x1] = mo
            y += bh
            row += 1

    def shade_cols(self, x0, x1, y0, y1, top, bottom):
        """y0~y1 구간을 위 top → 아래 bottom 배율로 어둡게"""
        y0, y1 = max(0, int(y0)), min(self.H, int(y1))
        k = np.linspace(top, bottom, y1 - y0)[:, None, None]
        self.img[y0:y1, int(x0):int(x1)] *= k

    def darken_left(self, w, strength=0.88, power=1.3):
        """카드 왼쪽 글자 자리를 어둡게 (시안의 카드처럼 왼쪽 → 오른쪽으로 옅어짐)"""
        xs = np.arange(self.W)
        k = np.clip(1 - xs / float(w), 0, 1) ** power * strength
        self.img *= (1 - k)[None, :, None]

    def darken_text_area(self, x1, y0, strength=0.8):
        """카드 글자 자리(왼쪽 아래)를 어둡게 — 오른쪽으로, 위로 갈수록 옅어짐"""
        xs = np.clip(1 - np.arange(self.W) / float(x1), 0, 1) ** 0.8
        ys = np.clip((np.arange(self.H) - y0) / float(max(1, self.H - y0)) * 1.6, 0, 1)
        k = ys[:, None] * (0.35 + 0.65 * xs[None, :]) * strength
        self.img *= (1 - k)[..., None]

    # -- 스프라이트 붙이기
    def sprite(self, spr, x, base, scale=1.0, flip=False, alpha=1.0, tint=None, tint_a=0.0):
        """spr 의 발 가운데(anchor)가 (x, base) 에 오도록 붙입니다. scale 은 스프라이트 픽셀 배율."""
        im = spr.image(scale, flip)
        a = np.asarray(im, dtype=np.float64) / 255.0
        rgb, al = a[..., :3], a[..., 3] * alpha
        if tint is not None:
            rgb = rgb * (1 - tint_a) + hexc(tint) * tint_a
        ax = spr.anchor(scale, flip)
        left = int(round(x - ax))
        top = int(round(base - im.height))
        self._paste(rgb, al, left, top)

    def sprite_part(self, spr, sx0, sy0, sx1, sy1, left, top, scale=1.0, flip=False):
        """스프라이트의 일부(원본 픽셀 좌표)를 잘라 붙입니다 (클로즈업용)."""
        im = spr.full.crop((sx0, sy0, sx1, sy1))
        if flip:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        if scale != 1.0:
            im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.NEAREST)
        a = np.asarray(im, dtype=np.float64) / 255.0
        self._paste(a[..., :3], a[..., 3], int(left), int(top))

    def _paste(self, rgb, al, left, top):
        h, w = al.shape
        x0, y0 = max(0, left), max(0, top)
        x1, y1 = min(self.W, left + w), min(self.H, top + h)
        if x1 <= x0 or y1 <= y0:
            return
        sa = al[y0 - top:y1 - top, x0 - left:x1 - left, None]
        self.img[y0:y1, x0:x1] = self.img[y0:y1, x0:x1] * (1 - sa) + rgb[y0 - top:y1 - top, x0 - left:x1 - left] * sa

    # -- 저장
    def save(self, name, levels=26):
        th = np.tile(BAYER, (self.H // 4 + 1, self.W // 4 + 1))[:self.H, :self.W][..., None]
        step = 1.0 / (levels - 1)
        q = np.floor(np.clip(self.img, 0, 1) / step + th) * step
        out = (np.clip(q, 0, 1) * 255).round().astype(np.uint8)
        Image.fromarray(out, 'RGB').save(os.path.join(RES, name), optimize=True)
        print('saved', name, self.W, 'x', self.H)


class Sprite:
    """게임 스프라이트 한 장. 배율을 바꿔도 픽셀이 뭉개지지 않게 줄입니다."""

    def __init__(self, name):
        self.full = Image.open(os.path.join(RES, name + '.png')).convert('RGBA')
        a = np.asarray(self.full)[..., 3]
        ys, xs = np.nonzero(a > 16)
        self.top, self.bottom = ys.min(), ys.max()
        h = self.bottom - self.top + 1
        foot = ys >= self.bottom - int(h * 0.18)
        self.ax = (xs[foot].min() + xs[foot].max() + 1) * 0.5   # 발 가운데 (게임 SpriteSet 과 같은 기준)
        self.cache = {}

    def image(self, scale, flip):
        key = (round(scale, 3), flip)
        if key in self.cache:
            return self.cache[key]
        im = self.full.crop((0, 0, self.full.width, self.bottom + 1))
        if scale != 1.0:
            w, h = max(1, int(round(im.width * scale))), max(1, int(round(im.height * scale)))
            # 색은 BOX 로 평균내고, 투명도는 반 넘게 찬 칸만 남겨 외곽이 또렷한 픽셀이 되게 합니다.
            arr = np.asarray(im, dtype=np.float64)
            pre = arr.copy()
            pre[..., :3] *= arr[..., 3:4] / 255.0
            small = np.asarray(Image.fromarray(pre.clip(0, 255).astype(np.uint8)).resize((w, h), Image.BOX),
                               dtype=np.float64)
            alpha = np.asarray(Image.fromarray(arr[..., 3].astype(np.uint8)).resize((w, h), Image.BOX),
                               dtype=np.float64)
            rgb = small[..., :3] / np.maximum(alpha[..., None] / 255.0, 1e-3)
            out = np.dstack([rgb.clip(0, 255), np.where(alpha > 110, 255, 0)]).astype(np.uint8)
            im = Image.fromarray(out, 'RGBA')
        if flip:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        self.cache[key] = im
        return im

    def anchor(self, scale, flip):
        w = self.image(scale, flip).width
        ax = self.ax * w / float(self.full.width)
        return w - ax if flip else ax


SPR = {}


def spr(name):
    if name not in SPR:
        SPR[name] = Sprite(name)
    return SPR[name]


# ======================================================================
#  공통 그림 조각
# ======================================================================
def dusk_sky(p, y1, glow_x, glow_y, seed=1):
    """해 질 녘 + 불타는 도시로 붉게 물든 하늘, 연기 띠"""
    p.vgrad(0, y1, [(0, '#1c1420'), (0.3, '#3a2226'), (0.6, '#7a3c26'), (0.85, '#c8662e'), (1, '#f09a48')])
    p.light(p.radial(glow_x, glow_y, p.W * 0.45, 1.8), '#e0702e', 0.45)
    p.light(p.radial(glow_x, glow_y, p.W * 0.18, 1.5), '#ffb060', 0.35)
    rng = np.random.default_rng(seed)
    for _ in range(int(p.W / 18)):                    # 길게 늘어진 구름/연기 띠
        cx = rng.uniform(-20, p.W + 20)
        cy = rng.uniform(4, y1 * 0.75)
        w = rng.uniform(30, 90)
        h = rng.uniform(3, 7)
        p.ellipse((cx - w, cy - h, cx + w, cy + h), '#2e1c1e', rng.uniform(0.3, 0.6))
        p.hline(int(cx - w * 0.6), int(cx + w * 0.6), int(cy + h), '#c06a38', 0.5)


def smoke(p, sx, sy, height, drift, seed, alpha=0.1):
    r2 = np.random.default_rng(seed)
    for k in range(60):
        u = k / 60.0
        y = sy - u * height + r2.uniform(-2, 2)
        x = sx + u * drift + r2.uniform(-4, 4) * (0.4 + u)
        r = 3 + u * height * 0.22 + r2.uniform(0, 3)
        p.ellipse((x - r, y - r * 0.7, x + r, y + r * 0.7), '#1e1518', alpha * (1 - u) + 0.02)


def fire(p, x, y, w, h, seed):
    """불길 — 아래가 넓고 위로 갈수록 가늘어지는 픽셀 불꽃"""
    rng = np.random.default_rng(seed)
    p.light(p.radial(x, y - h * 0.4, max(w, h) * 2.2, 1.6), '#e0602a', 0.55)
    for k in range(int(w * h * 0.9)):
        u = rng.random() ** 1.6                       # 아래쪽에 더 많이
        yy = y - u * h
        spread = (1 - u) * w * 0.5 + 0.5
        xx = x + rng.uniform(-spread, spread)
        col = '#ffe07a' if u < 0.25 and abs(xx - x) < spread * 0.4 else ('#ff9a2e' if u < 0.6 else '#d8441e')
        p.px(xx, yy, col, 0.95)


def tower(p, x, y_top, w, y_bottom, base, mortar, seed, merlon=True):
    p.bricks(x, y_top, x + w, y_bottom, 7, 4, base, mortar, seed, var=0.06)
    if merlon:
        for k in range(0, w, 4):
            p.rect(x + k, y_top - 3, x + k + 2, y_top, base)
            p.px(x + k, y_top - 3, '#000000', 0.0)
    p.rect(x, y_top, x + 1, y_bottom, '#000000', 0.25)


def troy_city(p, x0, x1, ground, scale=1.0, seed=7, burning=True):
    """언덕 위 트로이 — 성벽 · 탑 · 꼭대기 궁전, 곳곳에 불"""
    rng = np.random.default_rng(seed)
    s = scale
    hill = p.ridge(seed, ground - 18 * s, (5 * s, 2 * s), (0.03, 0.11))
    hill[:x0] = ground
    hill[x1:] = ground
    p.fill_below(hill, ground + 2, '#2a1c16', x0, x1)
    # 안쪽 건물들
    for i in range(int((x1 - x0) / (9 * s))):
        bx = x0 + 6 * s + i * 9 * s + rng.uniform(-2, 2)
        bh = rng.uniform(10, 26) * s
        by = hill[int(min(x1 - 1, max(x0, bx)))] - 4 * s
        p.rect(bx, by - bh, bx + 8 * s, by + 2, '#4a3a30')
        p.rect(bx, by - bh, bx + 8 * s, by - bh + 1, '#6e5644')
        p.rect(bx + 6 * s, by - bh, bx + 8 * s, by + 2, '#33271f')
        for wy in np.arange(by - bh + 3, by - 1, 4 * s):
            if rng.random() < 0.5:
                p.rect(bx + 3 * s, wy, bx + 4.5 * s, wy + 1.5 * s, '#ffa040' if burning and rng.random() < 0.5 else '#1d1612')
    # 꼭대기 궁전
    cx = (x0 + x1) * 0.5 + 6 * s
    top = hill[int(cx)] - 34 * s
    # 신전 모양 궁전 — 기단 · 기둥 · 삼각 지붕
    p.rect(cx - 18 * s, top + 20 * s, cx + 18 * s, top + 25 * s, '#8a7258')
    p.hline(cx - 18 * s, cx + 18 * s, top + 20 * s, '#c09a70')
    p.rect(cx - 16 * s, top + 4 * s, cx + 16 * s, top + 20 * s, '#3a2a20')
    for k in range(-15, 16, 5):
        p.rect(cx + k * s, top + 4 * s, cx + (k + 2.4) * s, top + 20 * s, '#b0906a')
        p.rect(cx + (k + 1.6) * s, top + 4 * s, cx + (k + 2.4) * s, top + 20 * s, '#7a6048')
    p.rect(cx - 17 * s, top + 1 * s, cx + 17 * s, top + 4 * s, '#9a7e5e')
    p.poly([(cx - 18 * s, top + 1 * s), (cx + 18 * s, top + 1 * s), (cx, top - 9 * s)], '#a88866')
    p.poly([(cx - 13 * s, top), (cx + 13 * s, top), (cx, top - 6 * s)], '#6e5640')
    # 바깥 성벽과 탑
    wall_y = ground - 14 * s
    p.bricks(x0, wall_y, x1, ground + 1, 8, 4, '#6a5442', '#3e3026', seed + 3, var=0.08)
    for k in range(x0, x1, int(5 * s) or 1):
        p.rect(k, wall_y - 2 * s, k + 2.5 * s, wall_y, '#6a5442')
    for tx in np.linspace(x0 + 4, x1 - 14 * s, 4):
        tower(p, tx, wall_y - 14 * s, int(10 * s), ground + 1, '#76604c', '#3e3026', int(tx) + seed)
        p.rect(tx + 4 * s, wall_y - 9 * s, tx + 6 * s, wall_y - 5 * s, '#1a1310')
        # 붉은 깃발
        p.rect(tx + 5 * s, wall_y - 26 * s, tx + 5.6 * s, wall_y - 16 * s, '#3a2a20')
        p.rect(tx + 5.6 * s, wall_y - 26 * s, tx + 10 * s, wall_y - 22 * s, '#a22a20')
    p.shade_cols(x0, x1, wall_y - 30 * s, ground + 1, 1.0, 0.75)
    if burning:
        for i in range(5):
            fx = x0 + (x1 - x0) * rng.uniform(0.1, 0.9)
            fire(p, fx, hill[int(fx)] - rng.uniform(4, 16) * s, rng.uniform(6, 12) * s, rng.uniform(10, 20) * s,
                 seed * 10 + i)
            smoke(p, fx, hill[int(fx)] - 20 * s, 70 * s, rng.uniform(-30, 30), seed * 20 + i, 0.12)


def trojan_horse(p, x, y, s=1.0, face_left=True):
    """트로이 목마 — (x, y) 는 받침대 왼쪽 위. 크기 기준 약 104 x 118 (s=1)."""
    def P(pts):
        out = []
        for (px_, py_) in pts:
            if not face_left:
                px_ = 104 - px_
            out.append((x + px_ * s, y + py_ * s))
        return out

    body_m = p.mask(lambda d: (
        d.polygon(P([(26, 4), (30, 0), (33, 6), (36, 10), (44, 40), (40, 52), (24, 52), (20, 30),
                     (10, 32), (2, 30), (0, 24), (8, 16), (18, 10)]), fill=255),          # 머리 · 목
        d.ellipse((min(P([(18, 40), (100, 82)])[0][0], P([(18, 40), (100, 82)])[1][0]), y + 40 * s,
                   max(P([(18, 40), (100, 82)])[0][0], P([(18, 40), (100, 82)])[1][0]), y + 82 * s), fill=255),  # 몸통
        d.polygon(P([(96, 50), (104, 54), (103, 76), (98, 74)]), fill=255),                 # 꼬리
        d.polygon(P([(26, 74), (34, 74), (33, 104), (27, 104)]), fill=255),                 # 앞다리
        d.polygon(P([(38, 76), (46, 76), (45, 104), (39, 104)]), fill=255),
        d.polygon(P([(72, 76), (80, 76), (79, 104), (73, 104)]), fill=255),                 # 뒷다리
        d.polygon(P([(84, 74), (92, 74), (91, 104), (85, 104)]), fill=255),
    ))
    # 나무 판자 결 — 가로 줄무늬 + 이음매
    H, W = p.H, p.W
    yy, xx = np.mgrid[0:H, 0:W]
    plank = ((yy - int(y)) % max(2, int(4 * s)) == 0)
    seam = ((xx * 7 + (yy // max(2, int(4 * s))) * 13) % max(5, int(17 * s)) == 0)
    base = hexc('#8a5a32')
    p.blend(body_m, base)
    p.blend(body_m * plank, '#5a371c', 0.9)
    p.blend(body_m * seam, '#5a371c', 0.7)
    # 왼쪽 위에서 불빛을 받는 밝은 면, 아래는 그림자
    shade = np.clip((yy - y) / (104 * s), 0, 1)
    p.img *= (1 - body_m * shade * 0.45)[..., None]
    edge = body_m - np.roll(body_m, 1, axis=0) * body_m
    p.blend(np.clip(edge, 0, 1), '#d89a5a', 0.8)
    edge_l = body_m - np.roll(body_m, 1 if face_left else -1, axis=1) * body_m
    p.blend(np.clip(edge_l, 0, 1), '#c88a4a', 0.6)
    # 갈기 · 눈 · 몸통 옆 비밀 문
    p.poly(P([(30, 1), (35, 8), (44, 38), (40, 40), (32, 12)]), '#4a2c16')
    p.rect(*[P([(12, 18)])[0][0] - s, y + 17 * s, P([(12, 18)])[0][0] + s, y + 19 * s], '#1a0e08')
    door = P([(54, 54), (70, 70)])
    p.rect(min(door[0][0], door[1][0]), y + 54 * s, max(door[0][0], door[1][0]), y + 70 * s, '#4a2c16', 0.8)
    p.rect(min(door[0][0], door[1][0]) + s, y + 55 * s, max(door[0][0], door[1][0]) - s, y + 69 * s, '#6b4424', 0.8)
    # 받침대와 바퀴
    p.rect(x + 2 * s, y + 104 * s, x + 102 * s, y + 110 * s, '#5a3a20')
    p.hline(x + 2 * s, x + 102 * s, y + 104 * s, '#a87444')
    for wx in (12, 36, 68, 92):
        cx = x + wx * s
        p.ellipse((cx - 6 * s, y + 106 * s, cx + 6 * s, y + 118 * s), '#3a2412')
        p.ellipse((cx - 4 * s, y + 108 * s, cx + 4 * s, y + 116 * s), '#6b4424')
        p.ellipse((cx - 1.5 * s, y + 110.5 * s, cx + 1.5 * s, y + 113.5 * s), '#2a1a0c')


def ship(p, x, y, s, col='#151a22', sail='#2a2420', lit='#c0703a'):
    hull = [(x - 60 * s, y - 8 * s), (x + 52 * s, y - 8 * s), (x + 74 * s, y - 22 * s),
            (x + 62 * s, y + 4 * s), (x - 52 * s, y + 6 * s), (x - 66 * s, y - 10 * s)]
    p.poly(hull, col)
    p.hline(x - 58 * s, x + 54 * s, y - 8 * s, lit, 0.5)
    for k in range(7):
        ox = x - 40 * s + k * 13 * s
        p.poly([(ox, y), (ox + 1, y), (ox - 5 * s, y + 12 * s), (ox - 6 * s, y + 12 * s)], col)
    p.rect(x - 1, y - 62 * s, x + 1, y - 6 * s, col)
    p.rect(x - 28 * s, y - 58 * s, x + 28 * s, y - 24 * s, sail)
    for k in range(3):
        p.hline(x - 28 * s, x + 28 * s, y - 50 * s + k * 9 * s, '#000000', 0.25)
    p.rect(x + 25 * s, y - 58 * s, x + 28 * s, y - 24 * s, lit, 0.7)


def ground_band(p, y0, y1, seed):
    p.vgrad(y0, y1, [(0, '#5a4030'), (0.35, '#40301f'), (1, '#221810')])
    p.speckle(y0, y1, '#7a5a3c', 0.022, seed)
    p.speckle(y0, y1, '#140e0a', 0.03, seed + 1)


def spears(p, x0, x1, y_base, n, seed, length=(12, 20), col='#2a1e16', tip='#b8b0a0'):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x = rng.uniform(x0, x1)
        ln = rng.uniform(*length)
        tilt = rng.uniform(-2, 2)
        yb = y_base + rng.uniform(-3, 3)
        for k in range(int(ln)):
            p.px(x + tilt * k / ln, yb - k, col)
        p.px(x + tilt, yb - ln, tip)
        p.px(x + tilt, yb - ln - 1, tip, 0.7)


# ======================================================================
#  1. 전체 배경 480x270  (가상 좌표 = 그림 좌표 x 2)
# ======================================================================
def title_bg():
    p = Pic(480, 270)
    dusk_sky(p, 160, 330, 120, seed=3)
    for sx, dr, sd in ((300, 40, 11), (350, -30, 12), (410, 50, 13), (250, 20, 14)):
        smoke(p, sx, 110, 110, dr, sd, 0.14)

    # 먼 산
    p.fill_below(p.ridge(21, 128, (6, 3), (0.02, 0.07)), 170, '#2a1a1c')

    # 왼쪽 앞 성벽과 탑 (시안의 왼쪽 성벽 자리)
    p.bricks(0, 70, 172, 240, 12, 6, '#4a3c34', '#2a201b', 31, var=0.09)
    for k in range(0, 172, 10):
        p.bricks(k, 62, k + 6, 71, 6, 4, '#54453b', '#2a201b', 40 + k, var=0.05)
    tower(p, 100, 34, 40, 240, '#55463b', '#2a201b', 33)
    p.rect(114, 60, 124, 76, '#140e0b')
    p.rect(116, 62, 122, 76, '#0a0706')
    p.rect(118, 110, 120, 124, '#140e0b')
    p.shade_cols(0, 172, 34, 240, 0.85, 0.45)
    # 성벽에 비치는 불빛
    p.light(p.radial(172, 110, 90, 1.8) * (np.arange(480)[None, :] < 172), '#c0582a', 0.25)

    # 언덕 위 불타는 트로이 (가운데 ~ 오른쪽)
    troy_city(p, 210, 390, 150, scale=1.15, seed=7)

    # 전장 — 흙바닥
    ground_band(p, 150, 270, 51)
    p.light(p.radial(330, 150, 160, 1.6, sy=0.4), '#c0602a', 0.25)
    for i in range(30):                                   # 바퀴 자국 · 웅덩이
        rng = np.random.default_rng(60 + i)
        x, y = rng.integers(0, 480), rng.integers(160, 260)
        p.hline(x, x + rng.integers(6, 20), y, '#4a3626', 0.7)

    # 트로이 목마 (시안의 오른쪽 위치, 왼쪽을 바라봄)
    trojan_horse(p, 372, 30, 1.0, face_left=True)
    p.light(p.radial(372, 80, 60, 1.6), '#e07030', 0.18)

    # 멀리 있는 병사 무리 — 실제 병사 스프라이트를 작게
    rng = np.random.default_rng(70)
    names = ['troy_soldier_sprite_f0', 'troy_soldier_sprite_f2', 'troy_sprite_f0', 'troy_soldier_sprite_f1']
    gnames = ['greek_soldier_sprite_f2', 'greek_soldier_sprite_f3', 'greek_sprite_f0', 'greek_soldier_sprite_f0']
    for row, (yb, sc, n) in enumerate(((168, 0.15, 34), (182, 0.18, 26), (200, 0.22, 18), (222, 0.27, 12))):
        for i in range(n):
            x = rng.uniform(180, 480)
            greek = x < 330
            nm = (gnames if greek else names)[rng.integers(0, 4)]
            p.sprite(spr(nm), x, yb + rng.uniform(-3, 3), sc, flip=not greek,
                     tint='#1a0f0c', tint_a=0.45 - row * 0.1)
        spears(p, 180, 480, yb - 4, 10 + row * 3, 80 + row, (8 + row * 3, 14 + row * 4))
    # 앞쪽 큰 병사 몇 명 (등을 보이며 걸어가는 느낌 — 오른쪽을 향함)
    for x, sc, nm in ((276, 0.42, 'greek_soldier_sprite_f2'), (322, 0.38, 'greek_sprite_f0'),
                      (300, 0.34, 'greek_soldier_sprite_f3')):
        p.sprite(spr(nm), x, 252, sc, tint='#120a08', tint_a=0.3)

    # 왼쪽 앞: 아킬레우스 (오른쪽을 바라봄) — 시안의 전사 자리
    # 문구(HEROES WAGE WAR ...)가 올라갈 왼쪽 위는 살짝 어둡게
    k = np.clip(1 - np.abs(np.arange(270) - 62) / 40.0, 0, 1)[:, None] * np.clip(1 - np.arange(480) / 100.0, 0, 1)[None, :]
    p.img *= (1 - 0.55 * k)[..., None]
    p.light(p.radial(48, 170, 70, 1.5), '#a04a24', 0.25)
    p.sprite(spr('achilles_sprite_f0'), 46, 234, 1.0)

    # 화면 가장자리 비네트
    yy, xx = np.mgrid[0:270, 0:480]
    v = np.clip(((xx - 240) / 300.0) ** 2 + ((yy - 135) / 190.0) ** 2, 0, 1)
    p.img *= (1 - 0.45 * v)[..., None]
    p.save('title_bg.png')


# ======================================================================
#  2. 메뉴 카드
# ======================================================================
def card_campaign():
    p = Pic(186, 58)
    dusk_sky(p, 40, 150, 34, seed=5)
    troy_city(p, 108, 186, 38, scale=0.7, seed=9)
    ground_band(p, 38, 58, 41)
    p.light(p.radial(150, 38, 70, 1.6, sy=0.5), '#d0682e', 0.3)
    # 돌격하는 그리스군 (오른쪽으로 달림) — 뒤쪽은 작고 어둡게
    rng = np.random.default_rng(15)
    for yb, sc, n, ta in ((44, 0.16, 16, 0.5), (50, 0.21, 10, 0.3), (58, 0.3, 6, 0.1)):
        for i in range(n):
            x = rng.uniform(96, 182)
            nm = ['greek_soldier_sprite_f2', 'greek_soldier_sprite_f3', 'greek_sprite_f0'][rng.integers(0, 3)]
            p.sprite(spr(nm), x, yb + rng.uniform(-1, 2), sc, tint='#1a0f0c', tint_a=ta)
        spears(p, 96, 182, yb - 6, 8, int(yb), (6, 12))
    # 깃발 든 병사
    p.vline(150, 6, 54, '#2a1a10')
    p.rect(151, 7, 163, 16, '#9a2620')
    p.rect(151, 7, 163, 8, '#c8463a')
    p.rect(155, 10, 158, 13, '#e0a050')
    p.sprite(spr('greek_soldier_sprite_f3'), 148, 60, 0.36)
    p.darken_left(140, 0.92)
    p.darken_text_area(170, 30, 0.55)
    p.save('title_card_campaign.png')


def card_multi():
    p = Pic(186, 48)
    p.vgrad(0, 24, [(0, '#1a1a26'), (0.55, '#4a3a40'), (0.85, '#a8643c'), (1, '#e0904a')])
    p.light(p.radial(150, 22, 50, 1.5), '#ffb060', 0.35)
    p.ellipse((140, 12, 158, 30), '#f2b866')
    p.fill_below(p.ridge(4, 21, (2, 1), (0.05, 0.13)), 25, '#2a2430', 0, 120)
    # 멀리 해안 도시
    rng = np.random.default_rng(3)
    for i in range(10):
        bx = 120 + i * 7
        bh = rng.integers(4, 10)
        p.rect(bx, 24 - bh, bx + 6, 25, '#2e2428')
    p.vgrad(24, 48, [(0, '#3a4a5c'), (1, '#121a24')])
    for _ in range(160):
        y = rng.integers(25, 48)
        x = rng.integers(0, 186)
        p.hline(x, x + rng.integers(2, 8), y, '#7a8ea0' if y < 34 else '#3e5266', 0.55)
    for y in range(25, 48, 2):
        half = 4 + (y - 24) * 0.6
        p.hline(int(150 - half), int(150 + half), y, '#e0a060', 0.4)
    for (x, y, s) in ((96, 31, 0.2), (176, 33, 0.22), (40, 30, 0.14), (70, 41, 0.34), (138, 45, 0.4)):
        ship(p, x, y, s, sail='#4a3426', lit='#e09048')
    p.darken_left(118, 0.9)
    p.darken_text_area(150, 26, 0.45)
    p.save('title_card_multi.png')


def card_heroes():
    p = Pic(92, 49)
    p.vgrad(0, 49, [(0, '#1e1410'), (1, '#0c0806')])
    p.light(p.radial(62, 18, 46, 1.6), '#a0502a', 0.45)
    smoke(p, 70, 49, 50, 10, 5, 0.1)
    # 헥토르 클로즈업 — 스프라이트를 2배로 키워 투구와 어깨만 보이게
    # 헥토르 상반신 — 발은 카드 아래로 내려 머리~허리만 보이게, 왼쪽(글자 쪽)을 바라보게 뒤집음
    p.sprite(spr('hector_sprite_f0'), 64, 98, 0.66, flip=True)
    p.darken_left(56, 0.85)
    p.darken_text_area(92, 24, 0.75)
    p.save('title_card_heroes.png')


def card_army():
    p = Pic(90, 49)
    p.vgrad(0, 49, [(0, '#2a1a14'), (0.6, '#3a2418'), (1, '#140c08')])
    p.light(p.radial(70, 10, 60, 1.6), '#c0602e', 0.35)
    spears(p, 20, 90, 30, 18, 4, (20, 30), tip='#d0c8b0')
    # 방패를 든 그리스 중장보병 대열 (뒤 → 앞)
    for row, (yb, sc, xs, ta) in enumerate(((44, 0.36, (34, 50, 66, 82, 98), 0.45),
                                            (54, 0.44, (24, 44, 64, 84), 0.2),
                                            (66, 0.52, (36, 60, 84), 0.0))):
        for x in xs:
            p.sprite(spr('greek_sprite_f0'), x, yb, sc, tint='#140c08', tint_a=ta)
    p.darken_left(52, 0.85)
    p.darken_text_area(90, 22, 0.8)
    p.save('title_card_army.png')


def news():
    p = Pic(127, 36)
    dusk_sky(p, 30, 80, 24, seed=8)
    troy_city(p, 0, 127, 30, scale=0.75, seed=12)
    p.rect(0, 30, 127, 36, '#140c08')
    # 아래쪽은 글자 자리 — 어둡게
    k = np.clip((np.arange(36) - 14) / 12.0, 0, 1)
    p.img *= (1 - 0.82 * k)[:, None, None]
    p.save('title_news.png')


def logo_helmet():
    """로고 옆 투구 — 헥토르 스프라이트의 머리 부분 (픽셀 그대로, 배경은 투명)"""
    h = spr('hector_sprite_f0')
    ax = int(h.ax)
    im = h.full.crop((ax - 22, h.top, ax + 20, h.top + 30))
    im.save(os.path.join(RES, 'title_logo_helmet.png'), optimize=True)
    print('saved title_logo_helmet.png', im.size)


if __name__ == '__main__':
    title_bg()
    card_campaign()
    card_multi()
    card_heroes()
    card_army()
    news()
    logo_helmet()
