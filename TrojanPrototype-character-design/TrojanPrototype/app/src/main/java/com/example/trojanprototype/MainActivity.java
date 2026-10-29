package com.example.trojanprototype;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512 | 4096);
        setContentView(new GameView());
    }

    class GameView extends View {
        final Paint p = new Paint(3);
        final Paint spritePaint = new Paint();
        final Bitmap achillesSprite, hectorSprite, greekSprite, troySprite;
        final Random random = new Random();
        int scene = 0; // 0 story, 1 battlefield, 2 boss, 3 ending
        int heroHp = 100, enemyHp = 100, energy = 25, defeated = 0, turn = 0;
        boolean guarding = false, flash = false;
        long flashAt = 0;
        float scaleX, scaleY;
        String toast = "전장을 향해 나아가라";
        String[] story = {"트로이의 성벽 아래, 전쟁은 열 번째 해를 맞았다.", "아킬레우스여, 분노를 거두고 전우와 함께 싸워라.", "수많은 병사를 지나, 헥토르와 결판을 내라."};
        int storyIndex = 0;

        GameView() {
            super(MainActivity.this);
            setFocusable(true);
            achillesSprite = BitmapFactory.decodeResource(getResources(), R.drawable.achilles_sprite);
            hectorSprite = BitmapFactory.decodeResource(getResources(), R.drawable.hector_sprite);
            greekSprite = BitmapFactory.decodeResource(getResources(), R.drawable.greek_sprite);
            troySprite = BitmapFactory.decodeResource(getResources(), R.drawable.troy_sprite);
        }
        void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); }
        void text(Canvas c, String s, float x, float y, float size, int col, boolean bold) {
            color(col); p.setTypeface(bold ? Typeface.create("sans-serif", Typeface.BOLD) : Typeface.create("sans-serif", Typeface.NORMAL)); p.setTextSize(size); c.drawText(s, x, y, p);
        }
        void box(Canvas c, float l, float t, float r, float b, int col, float radius) { color(col); c.drawRoundRect(new RectF(l,t,r,b), radius,radius,p); }
        @Override protected void onDraw(Canvas raw) {
            super.onDraw(raw); scaleX=getWidth()/960f; scaleY=getHeight()/540f;
            raw.save(); raw.scale(scaleX,scaleY);
            drawBackdrop(raw);
            if(scene==0 || scene==3) drawStory(raw); else drawBattle(raw);
            raw.restore();
            if(flash && System.currentTimeMillis()-flashAt<180){ postInvalidateDelayed(30); }
        }
        void drawBackdrop(Canvas c) {
            color(0xff111b2a); c.drawColor(0xff111b2a);
            color(0xff263d50); c.drawRect(0,0,960,300,p);
            // dusk glow and distant city
            color(0xffe4a05b); c.drawCircle(770,130,76,p);
            color(0xff18222c);
            for(int i=0;i<9;i++){float x=470+i*58; c.drawRect(x,190-(i%3)*13,x+43,322,p); c.drawRect(x+12,154-(i%3)*13,x+30,200-(i%3)*13,p);}
            color(0xff9b653f); c.drawRect(0,320,960,540,p);
            color(0xffb77b45); c.drawRect(0,326,960,338,p);
            color(0xff333c3e); for(int i=0;i<7;i++) c.drawOval(i*170-30,340+i%2*40,i*170+120,365+i%2*40,p);
            // fires
            for(int i=0;i<4;i++){color(0xffd85b32);c.drawCircle(80+i*260,305,9,p);color(0xffffc05e);c.drawCircle(80+i*260,302,4,p);}
        }
        void drawStory(Canvas c) {
            box(c,52,48,908,492,0xe8171d25,20);
            text(c,scene==3?"전투 기록":"일리아스 · 트로이의 밤",88,103,19,0xffffcb7a,true);
            text(c,scene==3?"제1장 완료":"PROLOGUE",88,145,13,0xff9eb1be,true);
            if(scene==0){
                text(c,story[storyIndex],88,230,27,Color.WHITE,true);
                text(c,"창과 방패를 들고 트로이 전선으로 진입한다.",88,272,17,0xffc8d1d6,false);
                text(c,"스토리 컷신  ·  탭하여 다음 장면",88,405,14,0xffd6aa69,false);
            } else {
                text(c,"헥토르가 쓰러지고, 트로이의 밤은 잠시 고요해졌다.",88,225,24,Color.WHITE,true);
                text(c,"전투 결과  ·  처치한 병사  "+defeated,88,274,17,0xffc8d1d6,false);
                text(c,"화면을 탭해 처음부터 다시 플레이",88,405,14,0xffd6aa69,false);
            }
            box(c,690,430,860,475,0xffd49b55,24); text(c,scene==3?"다시 시작":"계속  →",721,459,17,0xff1d2025,true);
            // hero vignette
            drawSprite(c,achillesSprite,760,318,116,174,false);
        }
        void drawSprite(Canvas c,Bitmap bitmap,float centerX,float baseY,float width,float height,boolean flipped) {
            if(bitmap==null) return;
            float left=centerX-width/2f, top=baseY-height;
            if(flipped) c.save();
            if(flipped) c.scale(-1f,1f,centerX,baseY);
            c.drawBitmap(bitmap,null,new RectF(left,top,left+width,baseY),spritePaint);
            if(flipped) c.restore();
        }
        void drawBattle(Canvas c) {
            box(c,28,20,932,98,0xe8171d25,16);
            text(c,scene==1?"전선 돌파":"보스전 · 헥토르",52,54,21,0xffffce82,true);
            text(c,scene==1?"트로이 병사  "+(3-defeated)+"명 남음":"트로이의 왕자와 일대일 결투",52,78,13,0xffd2d8dc,false);
            bar(c,620,38,220,19,heroHp,0xff62bc91,"아킬레우스");
            bar(c,620,68,220,16,enemyHp,0xffd45d50,scene==1?"전열":"헥토르");
            if(scene==1) drawCrowd(c); else drawBoss(c);
            // lower HUD
            box(c,26,420,934,520,0xe8171d25,16);
            text(c,toast,48,451,15,0xffffd494,true);
            text(c,"집중  "+energy+"%",48,487,14,0xffbbcad2,false);
            box(c,258,438,410,501,0xff40505a,13); text(c,"공격",294,477,18,Color.WHITE,true);
            box(c,426,438,578,501,0xff40505a,13); text(c,"방어",464,477,18,Color.WHITE,true);
            box(c,594,438,746,501,energy>=50?0xffb37d45:0xff46515a,13); text(c,"필살기",626,477,18,Color.WHITE,true);
            box(c,778,438,912,501,0xffd49b55,13); text(c,"다음",817,477,18,0xff1d2025,true);
            if(flash && System.currentTimeMillis()-flashAt>180) flash=false;
        }
        void bar(Canvas c,float x,float y,float w,float h,int value,int col,String label){
            text(c,label,x,y-4,12,0xffe5e8e8,true); box(c,x+76,y-14,x+76+w,y-14+h,0xff35414a,6); box(c,x+76,y-14,x+76+w*Math.max(0,value)/100f,y-14+h,col,6);
        }
        void drawCrowd(Canvas c){
            // opposing ranks give the encounter a many-versus-many battlefield feel
            for(int i=0;i<8;i++){
                float x=520+(i%4)*75, y=310+(i/4)*54;
                drawSprite(c,troySprite,x,y,58,82,true);
            }
            for(int i=0;i<4;i++){
                float x=86+i*66; drawSprite(c,greekSprite,x,372,62,82,false);
            }
            drawSprite(c,achillesSprite,370,390,126,190,false);
            text(c,"아군 진형",78,282,13,0xffe4d6bf,true); text(c,"적군 진형",572,235,13,0xffe4d6bf,true);
        }
        void drawBoss(Canvas c){
            drawSprite(c,achillesSprite,270,395,132,198,false);
            drawSprite(c,hectorSprite,685,395,136,198,true);
            text(c,"헥토르",648,245,14,0xffffdfba,true);
            text(c,"일대일 결투",404,183,16,0xffffdfba,true);
        }
        void nextTurn(int damage) {
            if(scene==1) {
                enemyHp-=damage;
                if(enemyHp<=0){ defeated++; if(defeated>=3){scene=2;enemyHp=100;toast="헥토르가 결투를 청한다";guarding=false;invalidate();return;} enemyHp=100;toast="적 전열 격파! 다음 병사가 온다";energy=Math.min(100,energy+15); }
                else toast="공격 적중! 적의 반격을 대비하라";
            } else {
                enemyHp-=damage;
                if(enemyHp<=0){scene=3;toast="";invalidate();return;}
                toast="헥토르의 반격! 방어로 피해를 줄이자";
            }
            int retaliation=guarding?4:random.nextInt(scene==1?9:13)+8;
            heroHp=Math.max(0,heroHp-retaliation); guarding=false;
            if(heroHp<=0){scene=3;toast="전투 종료 · 다시 시작해 재도전";}
            energy=Math.min(100,energy+12); flash=true;flashAt=System.currentTimeMillis();invalidate();
        }
        void reset(){scene=0;storyIndex=0;heroHp=100;enemyHp=100;energy=25;defeated=0;toast="전장을 향해 나아가라";invalidate();}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX()/scaleX,y=e.getY()/scaleY;
            if(scene==0){ if(storyIndex<story.length-1)storyIndex++;else{scene=1;toast="적 병사들의 진형이 다가온다";} invalidate();return true; }
            if(scene==3){reset();return true;}
            if(y<420){ toast=scene==1?"병사들이 진형을 좁힌다":"헥토르가 방패를 겨눈다";invalidate();return true; }
            if(x>=258&&x<418){nextTurn(24);}
            else if(x<584&&x>=418){guarding=true;toast="방패를 올렸다 · 다음 피해 감소";}
            else if(x<760&&x>=584){if(energy>=50){energy-=50;nextTurn(48);}else toast="집중이 부족하다 · 공격해 집중을 모으자";}
            else if(x>=778){toast="전투 중에는 행동을 선택하라";}
            invalidate();return true;
        }
    }
}
