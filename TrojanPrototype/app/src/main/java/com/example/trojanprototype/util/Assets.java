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
    }
}
