package com.example.trojanprototype.data;

/**
 * 챕터 목록 (챕터 선택 화면 · 저장 슬롯 표시 · 해금 규칙에 쓰입니다).
 *
 * 담당: 심신 (콘텐츠) + 유장원 (진행 규칙)
 *
 * GameEngine 의 flowStep 번호와 챕터의 대응표가 이 파일에 모여 있습니다.
 * 장면을 추가하려면 GameEngine.advanceFlow() 와 아래 LIST 를 같이 고치세요.
 *
 *   flowStep  장면
 *   --------  ---------------------------
 *      1      프롤로그 컷신
 *      2      1장 컷신      3  1장 전투
 *      4      2장 컷신      5  2장 전투
 *      6      3장 컷신      7  3장 전투 (보스)
 *      8      4장 컷신      9  4장 전투 (최종전)
 *     10      5장 컷신     11  엔딩
 *
 * ★ 해금 규칙 ★
 *   '클리어한 챕터 수'(cleared)가 n 이면 0 ~ n 번 챕터를 고를 수 있습니다.
 *   프롤로그는 처음부터 열려 있고, 챕터를 클리어하면 바로 다음 챕터가 열립니다.
 */
public final class ChapterData {

    private ChapterData() {}

    public static final class Entry {
        /** "CHAPTER 1" 같은 작은 표시 */
        public final String tag;
        /** 챕터 이름 */
        public final String title;
        /** 한 줄 소개 (장소 / 상대) */
        public final String place;
        /** 이 챕터가 시작되는 flowStep (컷신) */
        public final int startStep;
        /** 이 챕터의 전투 번호 (GameData.STAGES 의 인덱스). 전투가 없으면 -1 */
        public final int stage;
        /** 카드 배경색 계열 — Backdrop 번호 */
        public final int backdrop;

        public Entry(String tag, String title, String place, int startStep, int stage, int backdrop) {
            this.tag = tag;
            this.title = title;
            this.place = place;
            this.startStep = startStep;
            this.stage = stage;
            this.backdrop = backdrop;
        }

        public boolean hasBattle() {
            return stage >= 0;
        }
    }

    public static final Entry[] LIST = {
            new Entry("PROLOGUE", "불화의 시작", "신들의 연회 · 컷신", 1, -1, 3),
            new Entry("CHAPTER 1", "전쟁의 발발", "트로이 해안 · 아킬레우스 vs 트로이 병사", 2, 0, 0),
            new Entry("CHAPTER 2", "끝나지 않는 전쟁", "성벽 아래 · 아킬레우스 vs 정예병", 4, 1, 1),
            new Entry("CHAPTER 3", "영웅들의 대립", "성문 앞 · 아킬레우스 vs 헥토르", 6, 2, 2),
            new Entry("CHAPTER 4", "아킬레우스의 최후", "불타는 왕궁 · 오디세우스 vs 수비대장", 8, 3, 2),
            new Entry("CHAPTER 5", "귀향의 시작", "이타카로 가는 바다 · 엔딩", 10, -1, 4)
    };

    /** 마지막 flowStep (엔딩) */
    public static final int ENDING_STEP = 11;

    public static int count() {
        return LIST.length;
    }

    /** flowStep 이 속한 챕터 번호. 범위를 벗어나면 가장 가까운 챕터. */
    public static int chapterOfStep(int step) {
        int idx = 0;
        for (int i = 0; i < LIST.length; i++) {
            if (step >= LIST[i].startStep) idx = i;
        }
        return idx;
    }

    /** 해당 flowStep 이 챕터의 첫 장면(컷신)이면 true, 전투면 false */
    public static boolean isStoryStep(int step) {
        if (step >= ENDING_STEP) return true;
        return LIST[chapterOfStep(step)].startStep == step;
    }

    /** 저장 슬롯에 표시할 "어디부터 시작하는지" 문구 */
    public static String resumeLabel(int step) {
        if (step >= ENDING_STEP) return "엔딩부터 시작";
        return isStoryStep(step) ? "컷신부터 시작" : "전투부터 시작";
    }

    /** 챕터를 고를 수 있는지 (cleared = 클리어한 챕터 수) */
    public static boolean isUnlocked(int chapter, int cleared) {
        return chapter >= 0 && chapter < LIST.length && chapter <= cleared;
    }
}
