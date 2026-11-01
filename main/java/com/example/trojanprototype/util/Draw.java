package com.example.trojanprototype.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Canvas 그리기 도우미.
 *
 * 담당: 유장원 (시스템)
 * 게임 루프는 1초에 60번 돌기 때문에, 그릴 때마다 new Paint() 를 만들면
 * 가비지 컬렉션이 자주 일어나 화면이 끊깁니다.
 * 그래서 Paint 와 RectF 를 하나씩만 만들어 두고 계속 재사용합니다.
 */
public class Draw {

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmpPaint = new Paint();
    private final RectF r = new RectF();
    private final Path path = new Path();

    private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    private static final Typeface NORMAL = Typeface.create("sans-serif", Typeface.NORMAL);

    public Draw() {
        bmpPaint.setFilterBitmap(false); // 픽셀 아트가 흐려지지 않게
        bmpPaint.setAntiAlias(false);
    }

    // ---------------------------------------------------------- 도형
    public void rect(Canvas c, float l, float t, float rt, float b, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawRect(l, t, rt, b, p);
    }

    public void round(Canvas c, float l, float t, float rt, float b, float radius, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        r.set(l, t, rt, b);
        c.drawRoundRect(r, radius, radius, p);
    }

    public void roundStroke(Canvas c, float l, float t, float rt, float b,
                            float radius, float width, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        r.set(l, t, rt, b);
        c.drawRoundRect(r, radius, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    public void circle(Canvas c, float x, float y, float radius, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawCircle(x, y, radius, p);
    }

    public void circleStroke(Canvas c, float x, float y, float radius, float width, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        c.drawCircle(x, y, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    public void oval(Canvas c, float l, float t, float rt, float b, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        r.set(l, t, rt, b);
        c.drawOval(r, p);
    }

    public void line(Canvas c, float x0, float y0, float x1, float y1, float width, int color) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        c.drawLine(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.FILL);
    }

    public void triangle(Canvas c, float x1, float y1, float x2, float y2,
                         float x3, float y3, int color) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(path, p);
    }

    /** 게이지용 — 왼쪽에서 오른쪽으로 차오르는 막대 */
    public void gauge(Canvas c, float l, float t, float r, float b,
                      float ratio, int backColor, int fillColor, float radius) {
        round(c, l, t, r, b, radius, backColor);
        float w = (r - l) * clamp(ratio, 0f, 1f);
        if (w > 1f) round(c, l, t, l + w, b, radius, fillColor);
    }

    // ---------------------------------------------------------- 글자
    public void text(Canvas c, String s, float x, float y, float size, int color,
                     int align, boolean bold) {
        if (s == null) return;
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(bold ? BOLD : NORMAL);
        p.setTextAlign(align == CENTER ? Paint.Align.CENTER
                : align == RIGHT ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(s, x, y, p);
    }

    /** 어두운 배경 위에서도 잘 보이도록 테두리를 넣어 그립니다. */
    public void textOutlined(Canvas c, String s, float x, float y, float size, int color,
                             int align, boolean bold) {
        // 그림자도 본문과 같은 투명도를 따라가야 서서히 나타나는 연출이 자연스럽습니다.
        int shadowAlpha = (int) ((color >>> 24) * 0.78f);
        text(c, s, x + 1.5f, y + 1.5f, size, withAlpha(0xFF000000, shadowAlpha), align, bold);
        text(c, s, x, y, size, color, align, bold);
    }

    public float textWidth(String s, float size, boolean bold) {
        if (s == null) return 0f;
        p.setTextSize(size);
        p.setTypeface(bold ? BOLD : NORMAL);
        return p.measureText(s);
    }


    // ---------------------------------------------------------- 카드 / 패널
    /**
     * 어두운 면 + 얇은 금테 + 안쪽 보조선으로 이루어진 패널.
     * 모서리는 거의 각지게(반지름 4) 두어 석조·청동 느낌을 냅니다.
     */
    public void panel(Canvas c, float l, float t, float rt, float b, int fill) {
        round(c, l, t, rt, b, 4f, fill);
        roundStroke(c, l, t, rt, b, 4f, 1.4f, 0xCCB8914F);
        roundStroke(c, l + 4f, t + 4f, rt - 4f, b - 4f, 2f, 1f, 0x33E6C98D);
    }

    /**
     * 메뉴 카드. 왼쪽이 어둡고 오른쪽으로 갈수록 투명해지는 면에
     * 금테를 두릅니다. accent 가 true 면 왼쪽에 굵은 금빛 띠를 붙여 강조합니다.
     */
    public void card(Canvas c, float l, float t, float rt, float b, boolean accent) {
        int steps = 10;
        float w = (rt - l) / steps;
        for (int i = 0; i < steps; i++) {
            int a = 232 - i * 12;
            rect(c, l + i * w, t, l + (i + 1) * w + 1f, b, withAlpha(0xFF0C0806, a));
        }
        roundStroke(c, l, t, rt, b, 3f, 1.3f, accent ? 0xFFD8B570 : 0x99B8914F);
        if (accent) {
            rect(c, l, t, l + 4f, b, 0xFFD8B570);
        }
    }

    /** 오른쪽을 가리키는 작은 화살표 ( > ) */
    public void chevron(Canvas c, float x, float y, float size, int color) {
        line(c, x - size * 0.5f, y - size, x + size * 0.5f, y, 2.2f, color);
        line(c, x + size * 0.5f, y, x - size * 0.5f, y + size, 2.2f, color);
    }

    /** 가장자리를 어둡게 눌러 주는 비네트 (화면 전체) */
    public void vignette(Canvas c, float w, float h) {
        int steps = 9;
        for (int i = 0; i < steps; i++) {
            int a = (int) (120 * (1f - i / (float) steps) * (1f - i / (float) steps));
            float e = i * 16f;
            int col = withAlpha(0xFF000000, a);
            rect(c, 0f, e, w, e + 16f, col);
            rect(c, 0f, h - e - 16f, w, h - e, col);
            rect(c, e, 0f, e + 16f, h, withAlpha(0xFF000000, a * 3 / 4));
            rect(c, w - e - 16f, 0f, w - e, h, withAlpha(0xFF000000, a * 3 / 4));
        }
    }

    /** 글자 사이를 벌려서 그립니다 (로고·소제목용) */
    public void spaced(Canvas c, String s, float x, float y, float size, int color,
                       int align, boolean bold, float gap) {
        if (s == null || s.length() == 0) return;
        float total = 0f;
        float[] ws = new float[s.length()];
        for (int i = 0; i < s.length(); i++) {
            ws[i] = textWidth(String.valueOf(s.charAt(i)), size, bold);
            total += ws[i] + (i < s.length() - 1 ? gap : 0f);
        }
        float cx = align == CENTER ? x - total * 0.5f : align == RIGHT ? x - total : x;
        for (int i = 0; i < s.length(); i++) {
            text(c, String.valueOf(s.charAt(i)), cx, y, size, color, LEFT, bold);
            cx += ws[i] + gap;
        }
    }

    // ---------------------------------------------------------- 비트맵
    /** 스프라이트를 지정한 사각형에 맞춰 그립니다. alpha 는 0~255. */
    public void bitmap(Canvas c, Bitmap b, float l, float t, float rt, float bt, int alpha) {
        if (b == null) return;
        bmpPaint.setAlpha(alpha);
        r.set(l, t, rt, bt);
        c.drawBitmap(b, null, r, bmpPaint);
    }

    // ---------------------------------------------------------- 색 도우미
    /** 색의 투명도만 바꿉니다. alpha 는 0~255. */
    public static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** 두 색을 t(0~1) 비율로 섞습니다. */
    public static int mix(int c0, int c1, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int a = (int) (Color.alpha(c0) + (Color.alpha(c1) - Color.alpha(c0)) * t);
        int r0 = (int) (Color.red(c0) + (Color.red(c1) - Color.red(c0)) * t);
        int g0 = (int) (Color.green(c0) + (Color.green(c1) - Color.green(c0)) * t);
        int b0 = (int) (Color.blue(c0) + (Color.blue(c1) - Color.blue(c0)) * t);
        return (a << 24) | (r0 << 16) | (g0 << 8) | b0;
    }

    /** 0→1 로 부드럽게 변하는 값 (연출용) */
    public static float smooth(float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
