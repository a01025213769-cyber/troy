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

    /** 한 캐릭터의 프레임 수 (대기 · 걷기A · 걷기B · 달리기 · 공격 · 방어/자세) */
    public static final int FRAME_COUNT = 6;

    private final HashMap<String, Bitmap[]> frameCache = new HashMap<String, Bitmap[]>();

    /**
     * 프레임 애니메이션용 그림 묶음을 가져옵니다.
     * 파일 이름은 "<spriteName>_f0" ~ "<spriteName>_f5" 입니다. (예: achilles_sprite_f4)
     * 모든 프레임은 같은 크기의 칸이고, 발 위치와 몸통 중심이 같아서 그대로 겹쳐 그리면 됩니다.
     * 프레임 파일이 하나라도 없으면 null 을 돌려주고, 그러면 한 장짜리 sprite() 로 그립니다.
     */
    public Bitmap[] frames(String name) {
        if (name == null) return null;
        if (frameCache.containsKey(name)) return frameCache.get(name);
        Bitmap[] out = new Bitmap[FRAME_COUNT];
        boolean ok = true;
        for (int i = 0; i < FRAME_COUNT; i++) {
            out[i] = sprite(name + "_f" + i);
            if (out[i] == null) ok = false;
        }
        if (!ok) out = null;
        frameCache.put(name, out);
        return out;
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
        frameCache.clear();
    }
}
