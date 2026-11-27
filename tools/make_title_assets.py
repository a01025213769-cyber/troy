"""
Cut the faction banners and top-bar icons of the title screen out of the reference
mockup (1536x1024). (Background and card art: see make_title_pixel.py)

usage: python3 -I make_title_assets.py <reference.png> <out_dir>
"""
import os
import sys

import cv2
import numpy as np
from PIL import Image

SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)

img = cv2.imread(SRC, cv2.IMREAD_COLOR)  # BGR
assert img.shape[:2] == (1024, 1536), img.shape
lum = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY).astype(np.float32)


def text_mask(boxes, thr, grow=3):
    """Bright pixels (text) inside the given boxes, grown a little to catch anti-aliasing."""
    m = np.zeros(lum.shape, np.uint8)
    for (x0, y0, x1, y1) in boxes:
        sub = lum[y0:y1, x0:x1]
        m[y0:y1, x0:x1] = np.where(sub > thr, 255, 0).astype(np.uint8)
    if grow:
        k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (grow * 2 + 1, grow * 2 + 1))
        m = cv2.dilate(m, k)
    return m


def inpaint(image, mask, radius=6):
    return cv2.inpaint(image, mask, radius, cv2.INPAINT_TELEA)


clean = img.copy()

# ---- 1. menu cards: title, subtitle and chevron
card_text = [
    (275, 304, 480, 352), (278, 360, 590, 390), (808, 398, 836, 430),   # campaign
    (275, 495, 460, 541), (278, 551, 520, 580), (808, 562, 836, 594),   # multiplayer
    (275, 664, 356, 712), (278, 722, 470, 750), (508, 726, 536, 758),   # heroes
    (581, 664, 735, 712), (585, 722, 765, 750), (808, 726, 836, 758),   # army
]
clean = inpaint(clean, text_mask(card_text, 105, 3), 6)

# ---- 2. objectives panel: everything bright inside the frame (text, circles, rules)
obj_inner = [(1134, 517, 1509, 736)]
clean = inpaint(clean, text_mask(obj_inner, 34, 2), 5)

# ---- 3. news card text
clean = inpaint(clean, text_mask([(1092, 864, 1365, 922)], 78, 3), 6)

# ---- 4. Korean line of the left tagline (it has a typo; the game redraws it)
clean = inpaint(clean, text_mask([(30, 578, 230, 626)], 95, 3), 6)

# Smooth the inpainted card/news patches a little so they read as soft photo areas.
soft = cv2.GaussianBlur(clean, (0, 0), 2.2)
patch = np.zeros(lum.shape, np.uint8)
for b in card_text + [(1092, 864, 1365, 922), (30, 578, 230, 626)]:
    x0, y0, x1, y1 = b
    patch[y0 - 4:y1 + 4, x0 - 4:x1 + 4] = 255
patch = cv2.GaussianBlur(patch, (0, 0), 3).astype(np.float32)[..., None] / 255.0
clean = (clean * (1 - patch) + soft * patch).astype(np.uint8)


def save_jpg(arr_bgr, name, q=90):
    Image.fromarray(cv2.cvtColor(arr_bgr, cv2.COLOR_BGR2RGB)).save(
        os.path.join(OUT, name), quality=q, optimize=True, progressive=False)


def save_png_rgba(arr_bgra, name):
    Image.fromarray(cv2.cvtColor(arr_bgra, cv2.COLOR_BGRA2RGBA)).save(
        os.path.join(OUT, name), optimize=True)


# Background, menu cards, news card and logo are pixel art now (tools/make_title_pixel.py).
# This script only cuts the small pieces that stayed from the mockup: banners and icons.

# ---- banners: rectangular crop with feathered edges
def feathered(crop, f=5):
    h, w = crop.shape[:2]
    a = np.ones((h, w), np.float32)
    ramp = np.linspace(0, 1, f, dtype=np.float32)
    a[:, :f] *= ramp[None, :]
    a[:, w - f:] *= ramp[::-1][None, :]
    a[:f, :] *= ramp[:, None]
    a[h - f:, :] *= ramp[::-1][:, None]
    bgra = cv2.cvtColor(crop, cv2.COLOR_BGR2BGRA)
    bgra[..., 3] = (a * 255).astype(np.uint8)
    return bgra


save_png_rgba(feathered(img[803:945, 22:134]), "title_banner_achaea.png")
save_png_rgba(feathered(img[803:942, 530:644]), "title_banner_troy.png")


# ---- small icons on the dark top bar: alpha from brightness
def lum_alpha(crop, lo, hi):
    g = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY).astype(np.float32)
    a = np.clip((g - lo) / float(hi - lo), 0, 1)
    a = a * a * (3 - 2 * a)
    bgra = cv2.cvtColor(crop, cv2.COLOR_BGR2BGRA)
    bgra[..., 3] = (a * 255).astype(np.uint8)
    return bgra


icons = {
    "title_icon_coin.png": ((1149, 23, 1186, 59), 22, 60),
    "title_icon_silver.png": ((1269, 23, 1306, 59), 22, 60),
    "title_icon_laurel.png": ((1371, 23, 1409, 59), 26, 70),
    "title_icon_gear.png": ((1464, 16, 1515, 66), 20, 52),
    "title_ornament.png": ((750, 974, 810, 1003), 24, 60),
}
for name, ((x0, y0, x1, y1), lo, hi) in icons.items():
    save_png_rgba(lum_alpha(img[y0:y1, x0:x1], lo, hi), name)

print("ok")
