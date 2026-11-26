# -*- coding: utf-8 -*-
"""
트로이 전쟁 — 효과음 · 배경음 합성기
=====================================

외부 음원을 쓰지 않고, 효과음 10개와 배경음 5곡을 전부 코드로 합성합니다.
(저작권 걱정 없이 발표·배포할 수 있습니다)

    python tools/make_sounds.py                  # app/src/main/res/raw 에 저장
    python tools/make_sounds.py <저장할 폴더>

필요한 것: Python 3 + numpy + scipy

악기 만드는 법
  · 리라(현악기)   Karplus-Strong 현 진동 합성 — 노이즈를 짧은 지연선에 돌려 줄을 튕긴 소리
  · 아울로스(피리) 배음 합성 + 비브라토 + 숨소리 — 고대 그리스의 이중 리드 피리
  · 호른           배음 합성, 소리가 시작될 때 밝아지는 금관 느낌
  · 북             음높이가 빠르게 떨어지는 사인파 + 걸러 낸 노이즈
  · 공간감         지수적으로 줄어드는 노이즈를 잔향(리버브) 응답으로 써서 합성곱

배경음은 끝과 처음이 자연스럽게 이어지도록(무한 반복) 음의 꼬리와 잔향을
곡의 앞부분으로 감아 넣습니다.
"""
import os
import sys
import wave

import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SFX_SR = 44100   # 효과음 샘플레이트
BGM_SR = 22050   # 배경음 샘플레이트 (용량을 줄이기 위해 절반)


# ======================================================================
#  공통 도구
# ======================================================================
def write_wav(path, x, sr):
    x = np.clip(np.asarray(x, dtype=np.float64), -1.0, 1.0)
    data = (x * 32767.0).astype('<i2').tobytes()
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(data)


def tt(dur, sr):
    return np.arange(int(round(dur * sr))) / float(sr)


def lowpass(x, fc, sr, order=2):
    fc = min(fc, sr * 0.45)
    return sosfilt(butter(order, fc, 'low', fs=sr, output='sos'), x)


def highpass(x, fc, sr, order=2):
    return sosfilt(butter(order, fc, 'high', fs=sr, output='sos'), x)


def bandpass(x, lo, hi, sr, order=2):
    hi = min(hi, sr * 0.45)
    return sosfilt(butter(order, [lo, hi], 'band', fs=sr, output='sos'), x)


def svf(x, fc, sr, q=0.7, mode='band'):
    """시간에 따라 차단 주파수(fc 배열)가 바뀌는 상태변수 필터. 휘두르는 소리에 씁니다."""
    fc = np.broadcast_to(np.asarray(fc, dtype=np.float64), x.shape)
    f = 2.0 * np.sin(np.pi * np.minimum(fc, sr * 0.22) / sr)
    low = band = 0.0
    out = np.empty_like(x)
    damp = 1.0 / q
    for i in range(len(x)):
        low += f[i] * band
        high = x[i] - low - damp * band
        band += f[i] * high
        out[i] = band if mode == 'band' else (low if mode == 'low' else high)
    return out


def norm_peak(x, peak):
    m = np.max(np.abs(x))
    return x if m < 1e-9 else x * (peak / m)


def rms(x):
    return float(np.sqrt(np.mean(x * x)) + 1e-12)


def db(v):
    return 10.0 ** (v / 20.0)


def fade(x, sr, a=0.002, r=0.01):
    n = len(x)
    na, nr = min(n, int(a * sr)), min(n, int(r * sr))
    if na > 0:
        x[:na] *= np.linspace(0, 1, na)
    if nr > 0:
        x[n - nr:] *= np.linspace(1, 0, nr)
    return x


def midi(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


def reverb_ir(sr, length=1.8, decay=0.45, seed=7, bright=4200):
    rng = np.random.default_rng(seed)
    t = tt(length, sr)
    ir = rng.standard_normal(len(t)) * np.exp(-t / decay)
    ir = lowpass(ir, bright, sr)
    ir[:int(0.012 * sr)] = 0.0          # 초기 반사 전의 짧은 공백
    return ir / np.sqrt(np.sum(ir * ir))


# ======================================================================
#  악기
# ======================================================================
def pluck(freq, dur, sr, bright=0.55, sustain=0.996, seed=1):
    """Karplus-Strong 현 진동 — 리라 / 저음 현"""
    rng = np.random.default_rng(seed)
    total = int(dur * sr)
    period = max(2, int(round(sr / freq - 0.5)))
    y = np.zeros(total + 1)
    burst = rng.uniform(-1, 1, period)
    burst = lowpass(burst, 600 + 7000 * bright, sr, order=1)
    y[1:period + 1] = burst                       # y[0] 은 이전 표본(0) 자리
    start = period + 1
    while start <= total:
        end = min(start + period, total + 1)
        y[start:end] = sustain * 0.5 * (y[start - period:end - period]
                                        + y[start - period - 1:end - period - 1])
        start = end
    out = y[1:]
    out = out - np.mean(out)
    return fade(out, sr, 0.001, 0.03)


def harmonic_tone(freq, dur, sr, amps_fn, vib_rate=5.2, vib_depth=0.0, env=None,
                  breath=0.0, seed=3):
    """배음 합성 공용 함수. amps_fn(k, t) 가 k 번째 배음의 세기를 돌려줍니다."""
    t = tt(dur, sr)
    f = freq * (1.0 + vib_depth * np.sin(2 * np.pi * vib_rate * t)
                * np.minimum(1.0, t / 0.25))
    phase = 2 * np.pi * np.cumsum(f) / sr
    out = np.zeros_like(t)
    kmax = int(min(24, (sr * 0.45) / freq))
    for k in range(1, kmax + 1):
        out += amps_fn(k, t) * np.sin(k * phase + 0.3 * k)
    if breath > 0:
        rng = np.random.default_rng(seed)
        nz = bandpass(rng.standard_normal(len(t)), 1200, 4200, sr)
        out += breath * nz / (rms(nz) * 6.0)
    if env is not None:
        out *= env
    return out


def adsr(dur, sr, a, d, s, r):
    n = int(round(dur * sr))
    t = np.arange(n) / sr
    env = np.ones(n) * s
    env = np.where(t < a, t / max(a, 1e-6), env)
    dm = (t >= a) & (t < a + d)
    env[dm] = 1.0 - (1.0 - s) * (t[dm] - a) / max(d, 1e-6)
    rn = int(r * sr)
    if rn > 0 and rn < n:
        env[n - rn:] *= np.linspace(1, 0, rn)
    return env


def aulos(freq, dur, sr, vel=1.0):
    """아울로스 — 콧소리가 섞인 이중 리드 피리"""
    tail = 0.12
    total = dur + tail
    env = adsr(total, sr, 0.045, 0.12, 0.82, tail + 0.04)

    def amps(k, t):
        hz = k * freq
        formant = 1.0 + 1.8 * np.exp(-((hz - 1350.0) / 520.0) ** 2)
        return (k ** -0.85) * formant

    return vel * harmonic_tone(freq, total, sr, amps, vib_depth=0.006, env=env, breath=0.05)


def flute(freq, dur, sr, vel=1.0):
    """부드러운 피리 (컷신 · 엔딩)"""
    tail = 0.15
    total = dur + tail
    env = adsr(total, sr, 0.08, 0.2, 0.85, tail + 0.05)
    table = [1.0, 0.32, 0.12, 0.05, 0.02]

    def amps(k, t):
        return table[k - 1] if k <= len(table) else 0.0

    return vel * harmonic_tone(freq, total, sr, amps, vib_depth=0.005, env=env, breath=0.09)


def horn(freq, dur, sr, vel=1.0):
    """호른 — 처음 순간에 소리가 밝아지는 금관"""
    tail = 0.18
    total = dur + tail
    env = adsr(total, sr, 0.05, 0.15, 0.78, tail + 0.05)

    def amps(k, t):
        bright = 0.55 + 0.9 * np.exp(-t / 0.05)       # 시작은 어둡다가 금방 밝아짐
        return (k ** -1.05) * np.exp(-(k - 1) * bright * 0.6)

    return vel * harmonic_tone(freq, total, sr, amps, vib_depth=0.002, env=env)


def pad(freqs, dur, sr, vel=1.0):
    """현악 패드 — 화음을 깔아 주는 배경"""
    total = dur + 0.8
    env = adsr(total, sr, 0.6, 0.4, 0.9, 0.9)
    out = np.zeros(int(round(total * sr)))
    for f0 in freqs:
        for det in (-0.004, 0.004):
            f = f0 * (1.0 + det)
            out += harmonic_tone(f, total, sr, lambda k, t: k ** -1.7, vib_rate=4.3,
                                 vib_depth=0.0015)[:len(out)]
    out = lowpass(out, 2400, sr)
    return vel * out * env


def doum(sr, vel=1.0):
    """프레임 드럼 낮은 소리"""
    t = tt(0.55, sr)
    f = 58.0 + 55.0 * np.exp(-t / 0.028)
    body = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / 0.2)
    rng = np.random.default_rng(11)
    skin = lowpass(rng.standard_normal(len(t)), 700, sr) * np.exp(-t / 0.035) * 0.35
    return vel * fade(np.tanh(1.6 * (body + skin)), sr, 0.0005, 0.05)


def tek(sr, vel=1.0, seed=12):
    """프레임 드럼 가장자리 소리"""
    t = tt(0.16, sr)
    rng = np.random.default_rng(seed)
    nz = bandpass(rng.standard_normal(len(t)), 1700, 6500, sr) * np.exp(-t / 0.022)
    ring = np.sin(2 * np.pi * 1150 * t) * np.exp(-t / 0.012) * 0.6
    return vel * fade(norm_peak(nz, 0.8) + ring, sr, 0.0003, 0.02)


def taiko(sr, vel=1.0):
    """큰 북 (보스전)"""
    t = tt(0.9, sr)
    f = 44.0 + 48.0 * np.exp(-t / 0.05)
    body = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / 0.38)
    rng = np.random.default_rng(21)
    skin = lowpass(rng.standard_normal(len(t)), 420, sr) * np.exp(-t / 0.07) * 0.5
    return vel * fade(np.tanh(1.8 * (body + skin)), sr, 0.0005, 0.08)


def shaker(sr, vel=1.0, seed=31):
    t = tt(0.09, sr)
    rng = np.random.default_rng(seed)
    nz = highpass(rng.standard_normal(len(t)), 5500, sr)
    env = np.minimum(1.0, t / 0.006) * np.exp(-t / 0.03)
    return vel * norm_peak(nz * env, 0.5)


def cymbal(sr, vel=1.0):
    t = tt(1.6, sr)
    rng = np.random.default_rng(41)
    nz = highpass(rng.standard_normal(len(t)), 3200, sr) * np.exp(-t / 0.5)
    return vel * norm_peak(nz, 0.5)


# ======================================================================
#  배경음 — 악보를 받아 반복 가능한 한 곡으로
# ======================================================================
class Song:
    def __init__(self, bpm, bars, sr=BGM_SR, beats_per_bar=4):
        self.sr = sr
        self.beat = 60.0 / bpm
        self.bar = self.beat * beats_per_bar
        self.length = int(round(self.bar * bars * sr))
        self.stems = {}

    def _stem(self, name):
        if name not in self.stems:
            self.stems[name] = np.zeros(self.length)
        return self.stems[name]

    def put(self, stem, beat_pos, sig):
        """beat_pos(박) 위치에 소리를 얹습니다. 곡 끝을 넘는 꼬리는 앞으로 감깁니다."""
        buf = self._stem(stem)
        start = int(round(beat_pos * self.beat * self.sr)) % self.length
        n = len(sig)
        pos = 0
        while pos < n:
            idx = (start + pos) % self.length
            chunk = min(n - pos, self.length - idx)
            buf[idx:idx + chunk] += sig[pos:pos + chunk]
            pos += chunk

    def mix(self, levels, verb_send, out_peak_db=-1.0):
        """각 악기 줄을 목표 음량(dBFS RMS)에 맞춘 뒤 섞고, 잔향을 감아 넣습니다."""
        sr = self.sr
        dry = np.zeros(self.length)
        wet_in = np.zeros(self.length)
        for name, sig in self.stems.items():
            lvl = levels.get(name, -24.0)
            s = sig * (db(lvl) / rms(sig))
            dry += s
            wet_in += s * verb_send.get(name, 0.15)
        ir = reverb_ir(sr)
        wet = fftconvolve(wet_in, ir)
        loop = wet[:self.length].copy()
        rest = wet[self.length:]
        while len(rest) > 0:                      # 잔향 꼬리를 곡 앞으로 감기
            k = min(len(rest), self.length)
            loop[:k] += rest[:k]
            rest = rest[k:]
        mixed = dry + loop * 0.9
        # 두 번 이어 붙여 필터를 통과시키고 뒤쪽 절반만 써서, 반복 이음새에서 튀지 않게 합니다.
        twice = highpass(np.concatenate([mixed, mixed]), 32, sr)
        out = twice[self.length:]
        out = out / np.max(np.abs(out)) * 1.25
        out = np.tanh(out)                         # 부드러운 리미터
        return norm_peak(out, db(out_peak_db))


def arp(song, stem, bar, tones, pattern, step_beats, vel=0.8, bright=0.5, seed0=0):
    for i, p in enumerate(pattern):
        m = tones[p]
        v = vel * (1.0 if i % 4 == 0 else 0.78)
        song.put(stem, bar * 4 + i * step_beats,
                 pluck(midi(m), 1.6, song.sr, bright=bright, seed=seed0 + i + bar * 17))


def melody(song, stem, inst, notes, vel=1.0):
    """notes = [(마디, 마디 안의 박, 길이(박), midi)]"""
    for bar, beat, length, m in notes:
        if m is None:
            continue
        song.put(stem, bar * 4 + beat, inst(midi(m), length * song.beat * 0.95, song.sr, vel))


# ---------------------------------------------------------------- 타이틀
def make_title():
    s = Song(bpm=80, bars=16)
    # 2마디씩: Dm C Bb A | Dm F G A   (D 도리안)
    chords = [
        ('Dm', [50, 53, 57], [62, 65, 69, 74, 77, 81]),
        ('C', [48, 52, 55], [60, 64, 67, 72, 76, 79]),
        ('Bb', [46, 50, 53], [58, 62, 65, 70, 74, 77]),
        ('A', [45, 49, 52], [57, 61, 64, 69, 73, 76]),
        ('Dm', [50, 53, 57], [62, 65, 69, 74, 77, 81]),
        ('F', [41, 48, 53], [60, 65, 69, 72, 77, 81]),
        ('G', [43, 50, 55], [62, 67, 71, 74, 79, 83]),
        ('A', [45, 49, 52], [57, 61, 64, 69, 73, 76]),
    ]
    for ci, (_, pad_notes, arp_tones) in enumerate(chords):
        bar0 = ci * 2
        s.put('pad', bar0 * 4, pad([midi(m) for m in pad_notes], 2 * s.bar, s.sr, 1.0))
        s.put('bass', bar0 * 4, pluck(midi(pad_notes[0] - 12), 2.6, s.sr, bright=0.3,
                                      sustain=0.998, seed=ci))
        s.put('bass', bar0 * 4 + 4, pluck(midi(pad_notes[0] - 12), 2.6, s.sr, bright=0.3,
                                          sustain=0.998, seed=ci + 50))
        for b in range(2):
            arp(s, 'lyre', bar0 + b, arp_tones, [0, 2, 1, 3, 2, 4, 3, 2], 0.5,
                bright=0.45, seed0=ci * 7 + b)
    for bar in range(16):
        s.put('drum', bar * 4, doum(s.sr, 1.0))
        s.put('drum', bar * 4 + 2, tek(s.sr, 0.5, seed=bar))
        if bar >= 4:
            s.put('drum', bar * 4 + 2.5, doum(s.sr, 0.55))
            s.put('drum', bar * 4 + 3.5, tek(s.sr, 0.3, seed=bar + 40))
    melody(s, 'lead', aulos, [
        (2, 0, 2, 76), (2, 2, 1, 74), (2, 3, 1, 72),
        (3, 0, 3, 67),
        (4, 0, 1, 65), (4, 1, 1, 67), (4, 2, 2, 69),
        (5, 0, 2, 70), (5, 2, 1, 69), (5, 3, 1, 67),
        (6, 0, 4, 69),
        (7, 0, 2, 73), (7, 2, 2, 76),
        (8, 0, 3, 74), (8, 3, 1, 76),
        (9, 0, 2, 77), (9, 2, 1, 76), (9, 3, 1, 74),
        (10, 0, 2, 72), (10, 2, 2, 69),
        (11, 0, 2, 77), (11, 2, 1, 76), (11, 3, 1, 72),
        (12, 0, 2, 74), (12, 2, 1, 71), (12, 3, 1, 67),
        (13, 0, 2, 71), (13, 2, 1, 72), (13, 3, 1, 74),
        (14, 0, 2, 76), (14, 2, 1, 73), (14, 3, 1, 71),
        (15, 0, 3, 69),
    ])
    return s.mix({'pad': -27, 'bass': -26, 'lyre': -24, 'drum': -25, 'lead': -21},
                 {'pad': 0.3, 'bass': 0.1, 'lyre': 0.35, 'drum': 0.15, 'lead': 0.35})


# ---------------------------------------------------------------- 컷신
def make_story():
    s = Song(bpm=64, bars=12)
    # Am Am F F C C G G Am F E E  (A 에올리안)
    plan = [('Am', [57, 60, 64]), ('Am', [57, 60, 64]), ('F', [53, 57, 60]), ('F', [53, 57, 60]),
            ('C', [55, 60, 64]), ('C', [55, 60, 64]), ('G', [55, 59, 62]), ('G', [55, 59, 62]),
            ('Am', [57, 60, 64]), ('F', [53, 57, 60]), ('E', [56, 59, 64]), ('E', [56, 59, 64])]
    for bar, (_, tones) in enumerate(plan):
        if bar == 0 or plan[bar - 1][1] != tones:
            span = 2 if bar + 1 < len(plan) and plan[bar + 1][1] == tones else 1
            s.put('pad', bar * 4, pad([midi(m) for m in tones], span * s.bar, s.sr, 1.0))
            s.put('bass', bar * 4, pluck(midi(tones[0] - 24), 3.5, s.sr, bright=0.25,
                                         sustain=0.9985, seed=bar))
        lyre_tones = [tones[0] - 12, tones[1] - 12, tones[2] - 12, tones[0], tones[1], tones[2]]
        arp(s, 'lyre', bar, lyre_tones, [0, 2, 3, 4, 5, 4, 3, 2], 0.5, vel=0.7, bright=0.35,
            seed0=bar * 5)
    melody(s, 'lead', flute, [
        (1, 0, 2, 76), (1, 2, 1, 74), (1, 3, 1, 72),
        (2, 0, 4, 69),
        (3, 0, 1, 72), (3, 1, 1, 74), (3, 2, 2, 76),
        (4, 0, 3, 79), (4, 3, 1, 76),
        (5, 0, 2, 74), (5, 2, 2, 72),
        (6, 0, 2, 71), (6, 2, 2, 74),
        (7, 0, 4, 67),
        (8, 0, 2, 69), (8, 2, 1, 72), (8, 3, 1, 76),
        (9, 0, 2, 77), (9, 2, 1, 76), (9, 3, 1, 74),
        (10, 0, 2, 76), (10, 2, 2, 68),
        (11, 0, 3, 71),
    ])
    return s.mix({'pad': -25, 'bass': -27, 'lyre': -25, 'lead': -22},
                 {'pad': 0.35, 'bass': 0.15, 'lyre': 0.45, 'lead': 0.45})


# ---------------------------------------------------------------- 전투
def make_battle():
    s = Song(bpm=140, bars=16)
    riff = {
        'E': [40, 40, 52, 40, 41, 40, 43, 41],
        'E2': [40, 40, 52, 40, 43, 41, 40, 38],
        'D': [38, 38, 50, 38, 40, 38, 41, 40],
        'C': [36, 36, 48, 36, 38, 36, 40, 38],
    }
    lyre_of = {
        'E': [64, 71, 76, 71, 77, 71, 76, 71], 'E2': [64, 71, 76, 71, 77, 71, 76, 71],
        'D': [62, 69, 74, 69, 76, 69, 74, 69], 'C': [60, 67, 72, 67, 74, 67, 72, 67],
    }
    plan = ['E', 'E', 'E', 'E2', 'E', 'E', 'C', 'D', 'E', 'E', 'C', 'D', 'C', 'D', 'E', 'E2']
    for bar, key in enumerate(plan):
        for i, m in enumerate(riff[key]):
            s.put('bass', bar * 4 + i * 0.5,
                  pluck(midi(m), 0.5, s.sr, bright=0.55, sustain=0.993, seed=bar * 8 + i))
        lv = 0.85 if bar < 4 else 0.55
        for i, m in enumerate(lyre_of[key]):
            s.put('lyre', bar * 4 + i * 0.5,
                  pluck(midi(m), 0.9, s.sr, bright=0.6, sustain=0.994, seed=300 + bar * 8 + i)
                  * (lv if i % 2 == 0 else lv * 0.7))
        # 북: 둠-텍-.-텍-둠-.-텍-.  (중동 계열 리듬)
        s.put('drum', bar * 4 + 0.0, doum(s.sr, 1.0))
        s.put('drum', bar * 4 + 0.5, tek(s.sr, 0.6, seed=bar))
        s.put('drum', bar * 4 + 1.5, tek(s.sr, 0.7, seed=bar + 100))
        s.put('drum', bar * 4 + 2.0, doum(s.sr, 0.9))
        s.put('drum', bar * 4 + 3.0, tek(s.sr, 0.75, seed=bar + 200))
        if bar % 2 == 1:
            s.put('drum', bar * 4 + 3.5, doum(s.sr, 0.6))
        for k in range(16):
            s.put('perc', bar * 4 + k * 0.25, shaker(s.sr, 0.9 if k % 4 == 2 else 0.5,
                                                    seed=bar * 16 + k))
        if bar % 4 == 0:
            s.put('drum', bar * 4, taiko(s.sr, 0.9))
        if bar >= 8:
            root = {'E': 52, 'E2': 52, 'D': 50, 'C': 48}[key]
            s.put('horn', bar * 4, horn(midi(root), s.bar * 0.9, s.sr, 0.8))
            s.put('horn', bar * 4, horn(midi(root + 7), s.bar * 0.9, s.sr, 0.6))
    for bar in (3, 7, 11, 15):
        s.put('horn', bar * 4 + 3, horn(midi(52), s.beat * 0.45, s.sr, 1.0))
        s.put('horn', bar * 4 + 3.5, horn(midi(53), s.beat * 0.45, s.sr, 1.0))
    melody(s, 'lead', aulos, [
        (4, 0, 1, 76), (4, 1, 1, 77), (4, 2, 1, 79), (4, 3, 1, 77),
        (5, 0, 2, 76), (5, 2, 1, 71), (5, 3, 1, 72),
        (6, 0, 1, 74), (6, 1, 1, 72), (6, 2, 1, 71), (6, 3, 1, 72),
        (7, 0, 3, 74), (7, 3, 1, 69),
        (8, 0, 1, 76), (8, 1, 0.5, 77), (8, 1.5, 0.5, 76), (8, 2, 1, 74), (8, 3, 1, 76),
        (9, 0, 2, 79), (9, 2, 1, 77), (9, 3, 1, 76),
        (10, 0, 1, 76), (10, 1, 1, 74), (10, 2, 2, 72),
        (11, 0, 2, 74), (11, 2, 2, 77),
        (12, 0, 2, 79), (12, 2, 1, 76), (12, 3, 1, 72),
        (13, 0, 2, 77), (13, 2, 1, 74), (13, 3, 1, 69),
        (14, 0, 1, 71), (14, 1, 1, 72), (14, 2, 1, 74), (14, 3, 1, 77),
        (15, 0, 3, 76),
    ])
    return s.mix({'bass': -21, 'lyre': -26, 'drum': -20, 'perc': -32, 'horn': -26, 'lead': -21},
                 {'bass': 0.05, 'lyre': 0.25, 'drum': 0.12, 'perc': 0.05, 'horn': 0.3,
                  'lead': 0.3})


# ---------------------------------------------------------------- 보스전
def make_boss():
    s = Song(bpm=152, bars=16)
    # D 히자즈 (D Eb F# G A Bb C) — 긴장감 있는 증2도 음정
    riff = {
        'D': [38, 38, 39, 38, 38, 42, 39, 38],
        'D2': [38, 38, 39, 38, 42, 43, 42, 39],
        'Bb': [34, 34, 36, 34, 34, 38, 36, 34],
        'C': [36, 36, 38, 36, 36, 39, 38, 36],
    }
    plan = ['D', 'D', 'D', 'D2', 'Bb', 'C', 'D', 'D2', 'D', 'D', 'Bb', 'C', 'Bb', 'C', 'D', 'D2']
    motif = [74, 75, 78, 75, 74, 72, 70, 72]
    for bar, key in enumerate(plan):
        for i, m in enumerate(riff[key]):
            s.put('bass', bar * 4 + i * 0.5,
                  pluck(midi(m), 0.45, s.sr, bright=0.65, sustain=0.992, seed=bar * 8 + i))
            s.put('bass', bar * 4 + i * 0.5,
                  pluck(midi(m + 12), 0.4, s.sr, bright=0.5, sustain=0.99, seed=900 + bar * 8 + i)
                  * 0.5)
        lv = 0.8 if bar < 4 else 0.45
        for i, m in enumerate(motif):
            s.put('lyre', bar * 4 + i * 0.5,
                  pluck(midi(m), 0.7, s.sr, bright=0.7, sustain=0.993, seed=500 + bar * 8 + i) * lv)
        for p in (0.0, 1.5, 2.0, 3.0):
            s.put('drum', bar * 4 + p, taiko(s.sr, 1.0 if p == 0.0 else 0.75))
        for p in (1.0, 2.5, 3.5):
            s.put('drum', bar * 4 + p, tek(s.sr, 0.8, seed=bar * 3 + int(p * 2)))
        for k in range(8):
            s.put('perc', bar * 4 + k * 0.5 + 0.25, shaker(s.sr, 0.7, seed=bar * 8 + k))
        if bar in (0, 8):
            s.put('perc', bar * 4, cymbal(s.sr, 1.0))
        root = {'D': 50, 'D2': 50, 'Bb': 46, 'C': 48}[key]
        s.put('horn', bar * 4, horn(midi(root), s.beat * 0.8, s.sr, 1.0))
        s.put('horn', bar * 4, horn(midi(root + 7), s.beat * 0.8, s.sr, 0.7))
        if bar >= 8:
            s.put('horn', bar * 4 + 2, horn(midi(root), s.beat * 1.8, s.sr, 0.8))
    melody(s, 'lead', aulos, [
        (4, 0, 2, 74), (4, 2, 1, 75), (4, 3, 1, 74),
        (5, 0, 1, 72), (5, 1, 1, 70), (5, 2, 2, 72),
        (6, 0, 1, 74), (6, 1, 1, 75), (6, 2, 1, 78), (6, 3, 1, 79),
        (7, 0, 3, 81), (7, 3, 1, 79),
        (8, 0, 1, 78), (8, 1, 1, 75), (8, 2, 2, 74),
        (9, 0, 2, 69), (9, 2, 2, 74),
        (10, 0, 1, 70), (10, 1, 1, 72), (10, 2, 1, 74), (10, 3, 1, 75),
        (11, 0, 2, 75), (11, 2, 1, 74), (11, 3, 1, 72),
        (12, 0, 2, 74), (12, 2, 2, 70),
        (13, 0, 1, 72), (13, 1, 1, 74), (13, 2, 1, 75), (13, 3, 1, 78),
        (14, 0, 2, 79), (14, 2, 1, 78), (14, 3, 1, 75),
        (15, 0, 3, 74),
    ])
    return s.mix({'bass': -20, 'lyre': -27, 'drum': -19, 'perc': -31, 'horn': -24, 'lead': -21},
                 {'bass': 0.05, 'lyre': 0.25, 'drum': 0.15, 'perc': 0.1, 'horn': 0.3, 'lead': 0.3})


# ---------------------------------------------------------------- 엔딩 (귀향)
def make_ending():
    s = Song(bpm=72, bars=12)
    plan = [('D', [50, 54, 57]), ('G', [55, 59, 62]), ('A', [57, 61, 64]), ('D', [50, 54, 57]),
            ('G', [55, 59, 62]), ('Em', [52, 55, 59]), ('A', [57, 61, 64]), ('A', [57, 61, 64]),
            ('D', [50, 54, 57]), ('G', [55, 59, 62]), ('A', [57, 61, 64]), ('D', [50, 54, 57])]
    for bar, (_, tones) in enumerate(plan):
        s.put('pad', bar * 4, pad([midi(m) for m in tones], s.bar, s.sr, 1.0))
        s.put('bass', bar * 4, pluck(midi(tones[0] - 12), 3.0, s.sr, bright=0.3,
                                     sustain=0.998, seed=bar))
        up = [tones[0], tones[1], tones[2], tones[0] + 12, tones[1] + 12, tones[2] + 12]
        arp(s, 'lyre', bar, up, [0, 1, 2, 3, 4, 3, 2, 1], 0.5, vel=0.75, bright=0.45,
            seed0=bar * 9)
        if bar % 2 == 0:
            s.put('drum', bar * 4, doum(s.sr, 0.8))
        s.put('drum', bar * 4 + 2, tek(s.sr, 0.25, seed=bar))
    melody(s, 'lead', flute, [
        (0, 0, 2, 78), (0, 2, 1, 76), (0, 3, 1, 74),
        (1, 0, 2, 71), (1, 2, 2, 74),
        (2, 0, 2, 73), (2, 2, 1, 76), (2, 3, 1, 69),
        (3, 0, 4, 74),
        (4, 0, 2, 79), (4, 2, 1, 78), (4, 3, 1, 76),
        (5, 0, 2, 76), (5, 2, 2, 71),
        (6, 0, 1, 73), (6, 1, 1, 74), (6, 2, 2, 76),
        (7, 0, 2, 81), (7, 2, 1, 79), (7, 3, 1, 76),
        (8, 0, 3, 78), (8, 3, 1, 76),
        (9, 0, 2, 74), (9, 2, 2, 71),
        (10, 0, 2, 73), (10, 2, 1, 76), (10, 3, 1, 73),
        (11, 0, 3, 74),
    ])
    for bar in (3, 7, 11):
        s.put('horn', bar * 4, horn(midi(50 if bar != 7 else 57), s.bar * 0.9, s.sr, 0.7))
    return s.mix({'pad': -25, 'bass': -27, 'lyre': -24, 'drum': -29, 'lead': -21, 'horn': -29},
                 {'pad': 0.35, 'bass': 0.15, 'lyre': 0.4, 'drum': 0.2, 'lead': 0.4, 'horn': 0.4})


# ======================================================================
#  효과음
# ======================================================================
def sfx_swing():
    sr = SFX_SR
    t = tt(0.24, sr)
    rng = np.random.default_rng(101)
    nz = rng.standard_normal(len(t))
    p = t / t[-1]
    fc = 500 + 2600 * np.sin(np.pi * np.minimum(1.0, p * 1.2)) ** 2
    out = svf(nz, fc, sr, q=1.6)
    env = np.sin(np.pi * np.minimum(1.0, p * 1.15)) ** 1.5
    return norm_peak(fade(out * env, sr), 0.75)


def sfx_hit():
    sr = SFX_SR
    t = tt(0.26, sr)
    rng = np.random.default_rng(102)
    f = 52 + 120 * np.exp(-t / 0.025)
    thump = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / 0.07)
    crack = highpass(rng.standard_normal(len(t)), 1800, sr) * np.exp(-t / 0.012)
    body = bandpass(rng.standard_normal(len(t)), 250, 1100, sr) * np.exp(-t / 0.045)
    out = np.tanh(2.2 * (thump + 0.35 * norm_peak(crack, 1) + 0.5 * norm_peak(body, 1)))
    return norm_peak(fade(out, sr, 0.0005, 0.03), 0.92)


def sfx_heavy():
    sr = SFX_SR
    t = tt(0.5, sr)
    rng = np.random.default_rng(103)
    f = 38 + 120 * np.exp(-t / 0.04)
    thump = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / 0.16)
    crack = highpass(rng.standard_normal(len(t)), 1500, sr) * np.exp(-t / 0.02)
    rumble = lowpass(rng.standard_normal(len(t)), 220, sr) * np.exp(-t / 0.18)
    metal = sum(np.sin(2 * np.pi * fr * t) * np.exp(-t / dcy) * a
                for fr, dcy, a in ((710, 0.09, 0.5), (1130, 0.07, 0.35), (1790, 0.05, 0.25)))
    out = np.tanh(3.0 * (thump + 0.45 * norm_peak(crack, 1) + 0.6 * norm_peak(rumble, 1)
                         + 0.25 * metal))
    return norm_peak(fade(out, sr, 0.0005, 0.05), 0.95)


def sfx_guard():
    sr = SFX_SR
    t = tt(0.45, sr)
    rng = np.random.default_rng(104)
    base = 640.0
    ratios = (1.0, 1.59, 2.14, 2.3, 2.65, 2.92, 3.16)
    decays = (0.22, 0.16, 0.12, 0.1, 0.08, 0.07, 0.06)
    clang = sum(np.sin(2 * np.pi * base * r * t + i) * np.exp(-t / d) / (1 + 0.4 * i)
                for i, (r, d) in enumerate(zip(ratios, decays)))
    tick = highpass(rng.standard_normal(len(t)), 3000, sr) * np.exp(-t / 0.008)
    f = 70 + 80 * np.exp(-t / 0.02)
    thud = np.sin(2 * np.pi * np.cumsum(f) / sr) * np.exp(-t / 0.05)
    out = 0.6 * norm_peak(clang, 1) + 0.35 * norm_peak(tick, 1) + 0.6 * thud
    return norm_peak(fade(out, sr, 0.0005, 0.05), 0.85)


def sfx_dodge():
    sr = SFX_SR
    t = tt(0.18, sr)
    rng = np.random.default_rng(105)
    p = t / t[-1]
    out = svf(rng.standard_normal(len(t)), 1400 + 4200 * p, sr, q=1.2)
    env = np.minimum(1.0, p / 0.25) * (1.0 - p) ** 1.4
    return norm_peak(fade(out * env, sr), 0.6)


def sfx_ultimate():
    sr = SFX_SR
    rise_d, total = 0.62, 1.25
    t = tt(total, sr)
    rng = np.random.default_rng(106)
    out = np.zeros(len(t))
    # 1) 기를 모으는 소리 — 음높이와 밝기가 함께 올라감
    n_rise = int(rise_d * sr)
    tr = t[:n_rise]
    pr = tr / rise_d
    f = 70 * (1 + 1.3 * pr ** 1.6)
    rise = np.zeros(n_rise)
    for det in (-0.012, 0.0, 0.012):
        ph = 2 * np.pi * np.cumsum(f * (1 + det)) / sr
        for k in range(1, 18):
            rise += np.sin(k * ph) / k * np.exp(-(k - 1) * (1.4 - 1.1 * pr))
    swell = svf(rng.standard_normal(n_rise), 400 + 5000 * pr ** 2, sr, q=1.0)
    rise = norm_peak(rise, 1) * 0.6 + norm_peak(swell, 1) * 0.4
    rise *= pr ** 1.3
    out[:n_rise] += rise
    # 2) 폭발 — 큰 타격 + 호른 화음
    boom = sfx_heavy()
    boom = np.interp(np.arange(0, len(boom), 0.85), np.arange(len(boom)), boom)  # 살짝 낮게
    nb = min(len(boom), len(t) - n_rise)
    out[n_rise:n_rise + nb] += boom[:nb] * 1.0
    chord = sum(horn(midi(m), 0.45, sr, 1.0) for m in (50, 57, 62, 66))
    nc = min(len(chord), len(t) - n_rise)
    out[n_rise:n_rise + nc] += norm_peak(chord, 1)[:nc] * 0.5
    out = np.tanh(1.3 * out)
    return norm_peak(fade(out, sr, 0.005, 0.12), 0.95)


def sfx_win():
    sr = SFX_SR
    total = 2.0
    out = np.zeros(int(total * sr))

    def put(at, sig, g=1.0):
        i = int(at * sr)
        n = min(len(sig), len(out) - i)
        out[i:i + n] += sig[:n] * g

    notes = [(0.0, 0.16, 62), (0.18, 0.16, 66), (0.36, 0.16, 69), (0.54, 1.1, 74)]
    for at, d, m in notes:
        put(at, horn(midi(m), d, sr, 1.0))
        put(at, horn(midi(m - 12), d, sr, 0.5))
    for i, m in enumerate((62, 66, 69, 74, 78, 81)):
        put(0.54 + i * 0.05, pluck(midi(m), 1.3, sr, bright=0.6, seed=600 + i), 0.5)
    put(0.54, doum(sr, 1.0) * 0.8)
    return norm_peak(fade(out, sr, 0.002, 0.25), 0.9)


def sfx_lose():
    sr = SFX_SR
    total = 2.1
    out = np.zeros(int(total * sr))

    def put(at, sig, g=1.0):
        i = int(at * sr)
        n = min(len(sig), len(out) - i)
        out[i:i + n] += sig[:n] * g

    for at, d, m in [(0.0, 0.34, 57), (0.38, 0.34, 55), (0.76, 0.34, 53), (1.14, 0.8, 52)]:
        put(at, lowpass(horn(midi(m), d, sr, 1.0), 1400, sr))
        put(at, lowpass(horn(midi(m - 12), d, sr, 0.6), 900, sr))
    put(0.0, doum(sr, 1.0) * 0.7)
    put(1.14, doum(sr, 1.0) * 0.7)
    return norm_peak(fade(out, sr, 0.002, 0.3), 0.85)


def sfx_select():
    sr = SFX_SR
    a = pluck(midi(81), 0.32, sr, bright=0.7, sustain=0.993, seed=700)
    b = pluck(midi(88), 0.32, sr, bright=0.6, sustain=0.993, seed=701)
    out = a + 0.5 * np.concatenate([np.zeros(int(0.035 * sr)), b])[:len(a)]
    return norm_peak(fade(out, sr, 0.0005, 0.08), 0.55)


def sfx_bow():
    sr = SFX_SR
    t = tt(0.3, sr)
    twang = pluck(98.0, 0.3, sr, bright=0.8, sustain=0.985, seed=800)
    twang *= np.exp(-t / 0.08)
    rng = np.random.default_rng(801)
    p = t / t[-1]
    whoosh = svf(rng.standard_normal(len(t)), 3000 - 1800 * p, sr, q=1.4)
    whoosh *= np.minimum(1.0, p / 0.1) * np.exp(-t / 0.07)
    out = 0.8 * norm_peak(twang, 1) + 0.45 * norm_peak(whoosh, 1)
    return norm_peak(fade(out, sr, 0.0005, 0.04), 0.75)


# ======================================================================
def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        here, '..', 'app', 'src', 'main', 'res', 'raw')
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    sfx = {
        'sfx_swing': sfx_swing, 'sfx_hit': sfx_hit, 'sfx_heavy': sfx_heavy,
        'sfx_guard': sfx_guard, 'sfx_dodge': sfx_dodge, 'sfx_ultimate': sfx_ultimate,
        'sfx_win': sfx_win, 'sfx_lose': sfx_lose, 'sfx_select': sfx_select, 'sfx_bow': sfx_bow,
    }
    for name, fn in sfx.items():
        x = fn()
        write_wav(os.path.join(out_dir, name + '.wav'), x, SFX_SR)
        print('%-14s %5.2fs  peak %.2f' % (name, len(x) / SFX_SR, np.max(np.abs(x))))

    bgm = {
        'bgm_title': make_title, 'bgm_story': make_story, 'bgm_battle': make_battle,
        'bgm_boss': make_boss, 'bgm_ending': make_ending,
    }
    for name, fn in bgm.items():
        x = fn()
        write_wav(os.path.join(out_dir, name + '.wav'), x, BGM_SR)
        print('%-14s %5.1fs  rms %.1f dBFS' % (name, len(x) / BGM_SR, 20 * np.log10(rms(x))))
    print('저장 위치:', out_dir)


if __name__ == '__main__':
    main()
