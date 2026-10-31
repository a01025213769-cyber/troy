package com.example.trojanprototype;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.example.trojanprototype.core.GameSurfaceView;

/**
 * 앱 시작점.
 *
 * 담당: 유장원 (시스템)
 *
 * 이 파일은 "전체 화면으로 만들고 게임 화면을 띄우는" 역할만 합니다.
 * 게임 내용은 전부 core / battle / ai / ui 패키지에 있습니다.
 *
 * ★ 2인 개발 시 주의 ★
 *  두 사람이 같은 파일을 동시에 고치면 Git 충돌이 납니다.
 *  아래처럼 담당 폴더를 나눠서 작업하세요.
 *
 *   유장원(시스템)                       심신(콘텐츠/연출)
 *   ├─ MainActivity.java                ├─ data/StoryData.java   대사·컷신
 *   ├─ core/      게임 루프·씬·입력      ├─ data/GameData.java    캐릭터·밸런스
 *   ├─ battle/    전투 규칙·판정         ├─ ui/Theme.java         색·배치
 *   ├─ ai/        적 AI                 ├─ ui/Backdrop.java      배경
 *   └─ util/      그리기 도구           ├─ ui/Screens.java       화면 문구
 *                                       ├─ ui/VirtualPad.java    버튼 모양
 *                                       ├─ res/drawable-nodpi/   캐릭터 그림
 *                                       └─ res/raw/              음악·효과음
 */
public class MainActivity extends Activity {

    private GameSurfaceView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameView = new GameSurfaceView(this);
        setContentView(gameView);
        hideSystemBars();
    }

    /** 상단 상태바와 하단 내비게이션 바를 숨겨 몰입형 전체 화면으로 만듭니다. */
    private void hideSystemBars() {
        if (gameView == null) return;
        final int flags =
                0x00000100   // LAYOUT_STABLE
                        | 0x00000200   // LAYOUT_HIDE_NAVIGATION
                        | 0x00000400   // LAYOUT_FULLSCREEN
                        | 0x00000002   // HIDE_NAVIGATION
                        | 0x00000004   // FULLSCREEN
                        | 0x00001000;  // IMMERSIVE_STICKY
        gameView.setSystemUiVisibility(flags);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (gameView != null) gameView.onActivityResume();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) gameView.onActivityDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // 전투 중에는 일시정지, 그 외에는 타이틀로. 타이틀에서 누르면 앱 종료.
        if (gameView != null && gameView.engine().onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
