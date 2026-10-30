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
        int scene = 0; // story, crowd battle, boss battle, chapter result
        int chapter = 0, heroHp = 100, enemyHp = 100, energy = 25, defeated = 0;
        boolean guarding = false, flash = false, gameWon = true;
        long flashAt = 0, bossActionAt = 0;
        float scaleX, scaleY, heroX = 270, hectorX = 685;
        int bossState = 0; // 0 approach, 1 windup, 2 recovery
        String toast = "이야기를 따라 전장으로";
        int storyIndex = 0;
        final String[][] dialogue = {
                {"나레이션|올림포스의 연회에서 황금 사과 하나가 신들의 다툼을 불렀다.", "파리스|가장 아름다운 여신을 고르라고요? 그 선택이 전쟁으로 번질 줄은 몰랐습니다.", "나레이션|파리스와 헬레네를 둘러싼 선택은 그리스 연합군을 트로이로 이끈다."},
                {"나레이션|전쟁은 열 번째 해. 아킬레우스가 이끄는 그리스군이 트로이 성벽 아래에 섰다.", "헥토르|아킬레우스, 병사들 뒤에 숨지 마라. 오늘은 내가 직접 너와 겨루겠다.", "아킬레우스|전열을 뚫고 네 앞에 서겠다. 이 결투로 전쟁의 분노를 끝내자."},
                {"나레이션|긴 포위 끝에 그리스군은 거대한 목마를 남기고 해안으로 물러났다.", "트로이 병사|적이 떠났다! 목마를 성안으로 들여 승리를 기념하자.", "나레이션|밤이 깊었다. 목마 속 병사들이 성문을 열기 위해 움직이기 시작한다."}
        };
        final String[][] narration = {
                {"황금 사과를 둘러싼 신들의 다툼이 인간 세계의 갈등으로 번진다.", "파리스의 선택과 헬레네의 이주는 전쟁의 불씨가 된다.", "그리스 연합군이 트로이를 향해 출정한다. 적 전열을 돌파하라."},
                {"전쟁의 향방은 영웅들의 선택과 전우애, 분노에 흔들린다.", "헥토르가 결투를 청한다. 그리스군과 함께 트로이 전열을 무너뜨려라.", "병사들을 물리쳤다. 이제 헥토르와 직접 결투한다."},
                {"그리스군은 목마를 남긴 채 물러나는 계략을 썼다.", "트로이군은 목마를 성안으로 옮겨 경계를 풀었다.", "성문을 열고 트로이군의 마지막 저항을 물리쳐라."}
        };
        final String[] chapterTitles = {"불화의 시작과 전쟁의 발발", "아킬레우스와 헥토르", "트로이의 함락과 귀향"};

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
            text(c,scene==3?"전투 결과":"트로이 전쟁 · 제 " +(chapter+1)+"장",88,103,19,0xffffcb7a,true);
            text(c,chapterTitles[chapter],88,145,15,0xff9eb1be,true);
            if(scene==0){
                String[] line=dialogue[chapter][storyIndex].split("\\|",2);
                text(c,line[0],88,204,16,0xffffce82,true);
                text(c,line.length>1?line[1]:"",88,248,22,Color.WHITE,true);
                text(c,"해설  ·  "+narration[chapter][storyIndex],88,302,16,0xffc8d1d6,false);
                text(c,"대화와 나레이션  ·  탭하여 계속",88,405,14,0xffd6aa69,false);
            } else {
                text(c,gameWon?(chapter==1?"헥토르와의 결투에서 승리했다.":chapter==2?"트로이는 함락되고 오디세우스는 귀향길에 오른다.":"그리스 연합군이 첫 전투에서 승리했다."):"전투에서 패배했다. 전열을 가다듬고 다시 도전하자.",88,225,22,Color.WHITE,true);
                text(c,"전투 결과  ·  격파한 적 전열  "+defeated,88,274,17,0xffc8d1d6,false);
                text(c,gameWon&&chapter<2?"탭하여 다음 이야기로": "탭하여 다시 시작",88,405,14,0xffd6aa69,false);
            }
            box(c,690,430,860,475,0xffd49b55,24); text(c,scene==3?(gameWon&&chapter<2?"다음 장":"확인") :"계속  →",721,459,17,0xff1d2025,true);
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
            text(c,scene==1?chapterTitles[chapter]:"보스전 · 헥토르",52,54,21,0xffffce82,true);
            text(c,scene==1?"다대다 전투  ·  남은 적 전열 "+(3-defeated)+" / 3":"보스전  ·  트로이의 왕자와 일대일 결투",52,78,13,0xffd2d8dc,false);
            bar(c,620,38,220,19,heroHp,0xff62bc91,"아킬레우스");
            bar(c,620,68,220,16,enemyHp,0xffd45d50,scene==1?"전열":"헥토르");
            if(scene==1) drawCrowd(c); else drawBoss(c);
            // lower HUD
            box(c,26,420,934,520,0xe8171d25,16);
            text(c,toast,48,451,15,0xffffd494,true);
            text(c,scene==1?"집중  "+energy+"%":"좌우 이동 · 공격 · 방어",48,487,14,0xffbbcad2,false);
            if(scene==1){
                box(c,258,438,410,501,0xff40505a,13); text(c,"공격",294,477,18,Color.WHITE,true);
                box(c,426,438,578,501,0xff40505a,13); text(c,"방어",464,477,18,Color.WHITE,true);
                box(c,594,438,746,501,energy>=50?0xffb37d45:0xff46515a,13); text(c,"필살기",626,477,18,Color.WHITE,true);
                box(c,778,438,912,501,0xffd49b55,13); text(c,"행동",817,477,18,0xff1d2025,true);
            } else {
                box(c,48,438,224,501,0xff40505a,13); text(c,"← 이동",96,477,17,Color.WHITE,true);
                box(c,234,438,410,501,0xff40505a,13); text(c,"이동 →",282,477,17,Color.WHITE,true);
                box(c,426,438,578,501,0xffa34f3f,13); text(c,"공격",470,477,18,Color.WHITE,true);
                box(c,594,438,746,501,0xff40505a,13); text(c,"방어",638,477,18,Color.WHITE,true);
                box(c,778,438,912,501,0xff46515a,13); text(c,"거리",817,477,18,Color.WHITE,true);
            }
            if(flash && System.currentTimeMillis()-flashAt>180) flash=false;
        }
        void bar(Canvas c,float x,float y,float w,float h,int value,int col,String label){
            text(c,label,x,y-4,12,0xffe5e8e8,true); box(c,x+76,y-14,x+76+w,y-14+h,0xff35414a,6); box(c,x+76,y-14,x+76+w*Math.max(0,value)/100f,y-14+h,col,6);
        }
        void drawCrowd(Canvas c){
            // opposing ranks give the encounter a many-versus-many battlefield feel
            int enemyCount=Math.max(2,8-defeated*2);
            for(int i=0;i<enemyCount;i++){
                float x=520+(i%4)*75, y=310+(i/4)*54;
                drawSprite(c,troySprite,x,y,58,82,true);
            }
            for(int i=0;i<Math.max(2,4-defeated/2);i++){
                float x=86+i*66; drawSprite(c,greekSprite,x,372,62,82,false);
            }
            drawSprite(c,achillesSprite,370,390,126,190,false);
            text(c,"그리스군 · 아킬레우스",78,282,13,0xffe4d6bf,true); text(c,"트로이군",572,235,13,0xffe4d6bf,true);
        }
        void drawBoss(Canvas c){
            drawSprite(c,achillesSprite,heroX,395,132,198,false);
            drawSprite(c,hectorSprite,hectorX,395,136,198,true);
            text(c,"헥토르",hectorX-36,245,14,0xffffdfba,true);
            text(c,"일대일 결투 · 직접 조작",356,174,16,0xffffdfba,true);
            String ai=bossState==0?"접근 중":bossState==1?"공격 준비 · 방어 가능":"공격 후 빈틈";
            text(c,"헥토르 AI  ·  "+ai,374,202,13,0xffd2d8dc,false);
        }
        void nextTurn(int damage) {
            if(scene==1) {
                enemyHp-=damage;
                if(enemyHp<=0){ defeated++; if(defeated>=3){guarding=false;if(chapter==1){scene=2;enemyHp=100;heroX=270;hectorX=685;bossState=0;toast="전열 돌파! 이제 헥토르와 직접 겨룬다";invalidate();}else finishChapter(true);return;} enemyHp=100;toast="적 전열 격파! 아군과 함께 진격한다";energy=Math.min(100,energy+15); }
                else toast="공격 적중! 적의 반격을 대비하라";
            }
            int retaliation=guarding?4:random.nextInt(9)+8;
            heroHp=Math.max(0,heroHp-retaliation); guarding=false;
            if(heroHp<=0){finishChapter(false);return;}
            energy=Math.min(100,energy+12); flash=true;flashAt=System.currentTimeMillis();invalidate();
        }
        void bossAction(int action){
            if(action==0) heroX=Math.max(110,heroX-34);
            else if(action==1) heroX=Math.min(520,heroX+34);
            else if(action==2){
                if(Math.abs(hectorX-heroX)<=178){
                    int damage=bossState==2?32:18; enemyHp-=damage;
                    toast=bossState==2?"헥토르의 빈틈을 찔렀다!":"공격 적중! 다음 행동을 선택하라";
                } else toast="사거리가 닿지 않는다 · 가까이 이동하라";
                if(enemyHp<=0){finishChapter(true);return;}
            } else { guarding=true; toast="방패를 올렸다 · 헥토르의 공격에 대비한다"; }
            if(bossState==0){
                hectorX=Math.max(heroX+92,hectorX-28); bossState=1;
                if(action!=3)toast="헥토르가 공격을 준비한다 · 방어하라";
            } else if(bossState==1){
                if(Math.abs(hectorX-heroX)<175){heroHp=Math.max(0,heroHp-(guarding?5:18));toast=guarding?"방패로 헥토르의 공격을 막았다":"헥토르의 공격을 맞았다 · 방어 타이밍을 노려라";}
                else toast="헥토르의 공격이 빗나갔다";
                guarding=false; bossState=2;
            } else { bossState=0; toast="헥토르가 자세를 다시 잡는다"; }
            if(heroHp<=0){finishChapter(false);return;}
            bossActionAt=System.currentTimeMillis(); flash=true;flashAt=bossActionAt;invalidate();
        }
        void finishChapter(boolean won){scene=3;gameWon=won;guarding=false;toast="";invalidate();}
        void beginChapter(int index){chapter=index;storyIndex=0;scene=0;heroHp=100;enemyHp=100;energy=25;defeated=0;heroX=270;hectorX=685;bossState=0;toast="이야기를 따라 전장으로";invalidate();}
        void reset(){beginChapter(0);}
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float x=e.getX()/scaleX,y=e.getY()/scaleY;
            if(scene==0){ if(storyIndex<dialogue[chapter].length-1)storyIndex++;else{scene=1;toast="양쪽 진형이 맞붙는다 · 적 전열을 돌파하라";} invalidate();return true; }
            if(scene==3){if(gameWon&&chapter<2)beginChapter(chapter+1);else if(gameWon)reset();else beginChapter(chapter);return true;}
            if(scene==2){
                if(y>=420){
                    if(x<224)bossAction(0);else if(x<410)bossAction(1);else if(x<584)bossAction(2);else if(x<760)bossAction(3);else{toast="이동과 방어로 헥토르의 빈틈을 노려라";invalidate();}
                } else {toast="헥토르의 움직임을 살펴라 · 하단 버튼으로 직접 조작";invalidate();}
                return true;
            }
            if(y<420){ toast=scene==1?"병사들이 진형을 좁힌다":"헥토르가 방패를 겨눈다";invalidate();return true; }
            if(x>=258&&x<418){nextTurn(24);}
            else if(x<584&&x>=418){guarding=true;toast="방패를 올렸다 · 다음 피해 감소";}
            else if(x<760&&x>=584){if(energy>=50){energy-=50;nextTurn(48);}else toast="집중이 부족하다 · 공격해 집중을 모으자";}
            else if(x>=778){toast="전투 중에는 행동을 선택하라";}
            invalidate();return true;
        }
    }
}
