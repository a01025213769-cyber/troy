# -*- coding: utf-8 -*-
"""
승리 연출 Lottie 애니메이션 생성기  →  app/src/main/res/raw/lottie_victory.json

    python tools/make_lottie.py

Lottie(bodymovin) JSON 형식으로 벡터 애니메이션을 만듭니다.
  · 금빛 햇살 16줄 — 튀어나왔다가 천천히 회전
  · 퍼져 나가며 사라지는 고리 2개
  · 바깥으로 흩어지는 반짝이 10개
  · 은은한 후광
  · 가운데에서 튀어나오는 금빛 별

60fps, 0~240 프레임. 게임은 0~60 프레임(등장)을 한 번 재생한 뒤
60~240 구간을 반복합니다. (햇살 16줄이라 67.5도 회전 후 처음과 같은 모양이 되어 이음새가 없습니다)
"""
import json
import os

W = H = 400
OP = 240
GOLD = [0.96, 0.78, 0.33, 1]
PALE = [1.0, 0.95, 0.76, 1]


def static(v):
    return {"a": 0, "k": v}


def anim(keys, dims=1, ease=True):
    """keys = [(프레임, 값), ...]  값은 숫자 또는 리스트"""
    k = []
    for i, (t, v) in enumerate(keys):
        s = v if isinstance(v, list) else [v]
        kf = {"t": t, "s": s}
        if i < len(keys) - 1:
            if ease:
                kf["i"] = {"x": [0.2] * dims, "y": [1] * dims}
                kf["o"] = {"x": [0.4] * dims, "y": [0] * dims}
            else:
                kf["i"] = {"x": [1] * dims, "y": [1] * dims}
                kf["o"] = {"x": [0] * dims, "y": [0] * dims}
        k.append(kf)
    return {"a": 1, "k": k}


def group_tr():
    return {"ty": "tr", "p": static([0, 0]), "a": static([0, 0]), "s": static([100, 100]),
            "r": static(0), "o": static(100), "sk": static(0), "sa": static(0)}


def layer(ind, name, shapes, opacity, scale, rotation=None):
    return {
        "ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1, "ao": 0, "bm": 0,
        "ip": 0, "op": OP, "st": 0,
        "ks": {
            "o": opacity,
            "r": rotation if rotation else static(0),
            "p": static([W / 2, H / 2, 0]),
            "a": static([0, 0, 0]),
            "s": scale,
        },
        "shapes": shapes,
    }


def ring(width):
    return [{"ty": "gr", "nm": "ring", "it": [
        {"ty": "el", "d": 1, "p": static([0, 0]), "s": static([220, 220])},
        {"ty": "st", "c": static(GOLD), "o": static(100), "w": static(width), "lc": 2, "lj": 2,
         "ml": 4},
        group_tr(),
    ]}]


def rays():
    return [{"ty": "gr", "nm": "rays", "it": [
        {"ty": "rc", "d": 1, "p": static([0, -118]), "s": static([9, 64]), "r": static(4.5)},
        {"ty": "fl", "c": static(GOLD), "o": static(100), "r": 1},
        {"ty": "rp", "nm": "repeat", "c": static(16), "o": static(0), "m": 1,
         "tr": {"ty": "tr", "p": static([0, 0]), "a": static([0, 0]), "s": static([100, 100]),
                "r": static(22.5), "so": static(100), "eo": static(100)}},
        group_tr(),
    ]}]


def sparkles():
    return [{"ty": "gr", "nm": "sparkles", "it": [
        {"ty": "el", "d": 1, "p": static([0, -70]), "s": static([10, 10])},
        {"ty": "fl", "c": static(PALE), "o": static(100), "r": 1},
        {"ty": "rp", "nm": "repeat", "c": static(10), "o": static(0), "m": 1,
         "tr": {"ty": "tr", "p": static([0, 0]), "a": static([0, 0]), "s": static([100, 100]),
                "r": static(36), "so": static(100), "eo": static(100)}},
        group_tr(),
    ]}]


def glow():
    return [{"ty": "gr", "nm": "glow", "it": [
        {"ty": "el", "d": 1, "p": static([0, 0]), "s": static([190, 190])},
        {"ty": "fl", "c": static(GOLD), "o": static(100), "r": 1},
        group_tr(),
    ]}]


def star():
    return [{"ty": "gr", "nm": "star", "it": [
        {"ty": "sr", "sy": 1, "d": 1, "pt": static(5), "p": static([0, 4]), "r": static(0),
         "ir": static(24), "is": static(0), "or": static(56), "os": static(0)},
        {"ty": "fl", "c": static(GOLD), "o": static(100), "r": 1},
        {"ty": "st", "c": static([0.45, 0.28, 0.08, 1]), "o": static(100), "w": static(5),
         "lc": 2, "lj": 2, "ml": 4},
        group_tr(),
    ]}, {"ty": "gr", "nm": "shine", "it": [
        {"ty": "sr", "sy": 1, "d": 1, "pt": static(5), "p": static([0, 4]), "r": static(0),
         "ir": static(10), "is": static(0), "or": static(24), "os": static(0)},
        {"ty": "fl", "c": static(PALE), "o": static(85), "r": 1},
        group_tr(),
    ]}]


def build():
    layers = [
        layer(6, "star", star(),
              anim([(4, 0), (9, 100)]),
              anim([(4, [0, 0, 100]), (16, [130, 130, 100]), (26, [100, 100, 100])], dims=3)),
        layer(1, "sparkles", sparkles(),
              anim([(0, 0), (4, 100), (14, 100), (40, 0)]),
              anim([(0, [40, 40, 100]), (40, [230, 230, 100])], dims=3),
              anim([(0, 0), (40, 40)])),
        layer(2, "ring2", ring(3),
              anim([(8, 0), (10, 80), (40, 0)]),
              anim([(8, [30, 30, 100]), (40, [150, 150, 100])], dims=3)),
        layer(3, "ring", ring(7),
              anim([(0, 100), (30, 0)]),
              anim([(0, [25, 25, 100]), (30, [130, 130, 100])], dims=3)),
        layer(4, "rays", rays(),
              anim([(0, 0), (8, 95), (60, 45)]),
              anim([(0, [0, 0, 100]), (12, [115, 115, 100]), (22, [100, 100, 100])], dims=3),
              anim([(0, 0), (OP, 90)], ease=False)),
        layer(5, "glow", glow(),
              anim([(0, 0), (12, 30), (60, 16)]),
              anim([(0, [0, 0, 100]), (15, [100, 100, 100])], dims=3)),
    ]
    return {"v": "5.7.4", "fr": 60, "ip": 0, "op": OP, "w": W, "h": H, "nm": "victory",
            "ddd": 0, "assets": [], "layers": layers}


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, '..', 'app', 'src', 'main', 'res', 'raw', 'lottie_victory.json')
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(build(), f, separators=(',', ':'))
    print(os.path.abspath(out), os.path.getsize(out), 'bytes')


if __name__ == '__main__':
    main()
