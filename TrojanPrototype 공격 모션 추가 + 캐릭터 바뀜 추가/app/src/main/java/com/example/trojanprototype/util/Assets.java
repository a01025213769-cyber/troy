package com.example.trojanprototype.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;

/**
 * 스프라이트(그림) 로딩과 보관.
 *
 * 담당: 유장원 (시스템)
 *
 * 그림을 이름으로 찾기 때문에, 심신이 res/drawable-nodpi 에 파일만 추가하고
 * GameData 의 spriteName 을 바꾸면 자바 코드를 고치지 않아도 캐릭터가 바뀝니다.
 * 파일이 없으면 null 을 돌려주고, 그릴 때 그냥 건너뜁니다. (앱이 죽지 않습니다)
 */
public class Assets {

    private final Resources res;
    private final String pkg;
    private final HashMap<String, Bitmap> cache = new HashMap<String, Bitmap>();
    private final HashMap<String, SpriteSet> sets = new HashMap<String, SpriteSet>();
    private final BitmapFactory.Options options = new BitmapFactory.Options();

    public Assets(Context ctx) {
        this.res = ctx.getResources();
        this.pkg = ctx.getPackageName();
        // 픽셀 아트가 기기 해상도에 따라 멋대로 늘어나지 않게 합니다.
        options.inScaled = false;
    }

    /** drawable 이름으로 비트맵을 가져옵니다. 없으면 null. */
    public Bitmap sprite(String name) {
        if (name == null) return null;
        if (cache.containsKey(name)) return cache.get(name);
        Bitmap bmp = null;
        try {
            int id = res.getIdentifier(name, "drawable", pkg);
            if (id != 0) {
                bmp = BitmapFactory.decodeResource(res, id, options);
            }
        } catch (Throwable ignored) {
            bmp = null;
        }
        cache.put(name, bmp);
        return bmp;
    }

    /**
     * 동작 프레임 묶음을 가져옵니다.
     * "achilles_sprite" 를 넘기면 achilles_sprite_f0 ~ _f5 를 찾고,
     * 하나도 없으면 achilles_sprite 한 장만 쓰는 묶음을 돌려줍니다.
     */
    public SpriteSet spriteSet(String name) {
        if (name == null) return new SpriteSet();
        SpriteSet cached = sets.get(name);
        if (cached != null) return cached;

        SpriteSet set = new SpriteSet();
        set.base = sprite(name);

        for (int i = 0; i < SpriteSet.FRAME_COUNT; i++) {
            Bitmap f = sprite(name + "_f" + i);
            set.frames[i] = f;
            if (f != null) set.animated = true;
        }

        // 기준이 될 프레임: 대기(f0) 우선, 없으면 기본 그림
        Bitmap ref = set.frames[SpriteSet.F_IDLE_A];
        if (ref == null) ref = set.base;
        if (ref == null) ref = set.frame(0);

        if (ref != null) {
            set.canvasW = ref.getWidth();
            set.canvasH = ref.getHeight();
            measure(ref, set);
        }

        sets.put(name, set);
        return set;
    }

    /**
     * 기준 프레임에서 "몸이 실제로 차지하는 영역"을 재어
     * 몸 높이와 몸 중심 가로 위치를 구합니다.
     *
     * 캐릭터마다 캔버스 여백이 다르기 때문에 이 값을 미리 재어 두어야
     * 모든 캐릭터의 키가 화면에서 비슷하게 맞습니다.
     * (그림이 바뀌어도 코드를 고칠 필요가 없도록 직접 계산합니다)
     */
    private void measure(Bitmap ref, SpriteSet set) {
        int w = ref.getWidth();
        int h = ref.getHeight();
        set.anchorX = w * 0.5f;
        set.bodyHeight = h;
        if (w <= 0 || h <= 0) return;

        try {
            int[] px = new int[w * h];
            ref.getPixels(px, 0, w, 0, 0, w, h);

            int minX = w, maxX = -1, minY = h, maxY = -1;
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    int alpha = (px[row + x] >>> 24);
                    if (alpha > 16) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            if (maxX < minX || maxY < minY) return;

            set.bodyHeight = (maxY - minY + 1);

            // 몸 중심은 '발' 위치로 잡습니다.
            // 전체 내용의 가운데를 쓰면 손에 든 창·검이 옆으로 튀어나온 만큼
            // 중심이 밀려서, 캐릭터가 서 있는 위치가 어긋나 보입니다.
            int footTop = maxY - (int) (set.bodyHeight * 0.18f);
            if (footTop < minY) footTop = minY;
            int fMinX = w, fMaxX = -1;
            for (int y = footTop; y <= maxY; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    if ((px[row + x] >>> 24) > 16) {
                        if (x < fMinX) fMinX = x;
                        if (x > fMaxX) fMaxX = x;
                    }
                }
            }
            set.anchorX = (fMaxX >= fMinX)
                    ? (fMinX + fMaxX + 1) * 0.5f
                    : (minX + maxX + 1) * 0.5f;
        } catch (Throwable ignored) {
            // 측정에 실패하면 캔버스 기준값을 그대로 씁니다.
        }
    }

    /** raw 리소스(mp3, wav) 의 id 를 이름으로 찾습니다. 없으면 0. */
    public int rawId(String name) {
        if (name == null) return 0;
        try {
            return res.getIdentifier(name, "raw", pkg);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public void clear() {
        cache.clear();
        sets.clear();
    }
}
