package com.example.trojanprototype.data;

/**
 * 컷신(이야기 장면) 대사 모음.
 *
 * 담당: 심신 (스토리 / 대사 / 사건 해설)
 * - 대사를 고치거나 장면을 추가할 때 이 파일만 수정하면 됩니다.
 * - lines 배열에 문장을 한 줄씩 넣으면 한 문장씩 페이드로 나타납니다.
 */
public final class StoryData {

    private StoryData() {}

    public static final class Chapter {
        /** 화면 왼쪽 위에 작게 뜨는 표시 (예: PROLOGUE) */
        public final String tag;
        /** 큰 제목 */
        public final String title;
        /** 한 줄씩 차례로 나타나는 본문 */
        public final String[] lines;
        /** 배경 종류 — Backdrop 참고 (0 해안 / 1 성벽 / 2 야간 / 3 신전 / 4 바다) */
        public final int backdrop;

        public Chapter(String tag, String title, String[] lines, int backdrop) {
            this.tag = tag;
            this.title = title;
            this.lines = lines;
            this.backdrop = backdrop;
        }
    }

    /** 게임 전체에 공통으로 표시하는 각색 안내 문구 */
    public static final String DISCLAIMER =
            "※ 그리스 신화와 서사시를 바탕으로 한 창작 각색입니다. 역사적 사실의 재현이 아닙니다.";

    /** 프롤로그 — 전쟁의 진짜 시작 */
    public static final Chapter PROLOGUE = new Chapter(
            "PROLOGUE", "불화의 시작",
            new String[]{
                    "신들의 연회에 불화의 여신 에리스만 초대받지 못했다.",
                    "그녀는 연회장에 황금 사과 하나를 던졌다. \"가장 아름다운 여신에게.\"",
                    "헤라와 아테나, 아프로디테가 사과의 주인을 두고 다투었고",
                    "심판은 트로이의 왕자 파리스에게 맡겨졌다."
            }, 3);

    /** 1장 — 전쟁의 발발, 이후 첫 번째 전투 */
    public static final Chapter CH_WAR = new Chapter(
            "CHAPTER 1", "전쟁의 발발",
            new String[]{
                    "파리스는 가장 아름다운 여인을 약속한 아프로디테를 택했다.",
                    "그 여인은 스파르타의 왕비 헬레네였다.",
                    "헬레네가 트로이로 떠나자 그리스의 왕들이 군대를 모았다.",
                    "아킬레우스와 이타카의 왕 오디세우스도 함대에 올랐다.",
                    "그리고 그리스군의 배가 트로이 해안에 닿는다."
            }, 0);

    /** 2장 — 끝나지 않는 전쟁, 이후 두 번째 전투 */
    public static final Chapter CH_WALL = new Chapter(
            "CHAPTER 2", "끝나지 않는 전쟁",
            new String[]{
                    "해안을 넘어도 트로이의 성벽은 무너지지 않았다.",
                    "1년, 3년, 7년. 그리고 10년.",
                    "전쟁은 끝날 기미를 보이지 않았다.",
                    "성벽 아래에서 양측의 정예가 다시 맞붙는다."
            }, 1);

    /** 3장 — 영웅들의 대립, 이후 보스전 */
    public static final Chapter CH_HEROES = new Chapter(
            "CHAPTER 3", "영웅들의 대립",
            new String[]{
                    "전쟁의 가장 강한 두 사람이 성문 앞에서 마주 선다.",
                    "아킬레우스는 명예와 분노를 위해 싸웠고,",
                    "헥토르는 자신의 도시와 가족을 위해 싸웠다.",
                    "서로 다른 이유를 가진 두 영웅의 결투가 시작된다."
            }, 2);

    /** 4장 — 아킬레우스의 죽음과 목마, 트로이 함락 */
    public static final Chapter CH_FALL = new Chapter(
            "CHAPTER 4", "트로이의 함락",
            new String[]{
                    "헥토르가 쓰러진 뒤에도 전쟁은 계속되었다.",
                    "파리스가 쏜 화살이 아킬레우스의 약점을 꿰뚫는다.",
                    "힘으로는 성벽을 넘을 수 없다고 판단한 오디세우스는",
                    "거대한 목마를 남기고 철수하는 척하는 계책을 세웠다.",
                    "밤이 되자 목마 안의 병사들이 성문을 열었고, 트로이는 불길에 휩싸였다."
            }, 2);

    /** 5장 — 귀향의 시작 (영화 《오디세이》로 이어지는 부분) */
    public static final Chapter CH_RETURN = new Chapter(
            "CHAPTER 5", "귀향의 시작",
            new String[]{
                    "10년의 전쟁이 끝났다.",
                    "오디세우스는 부하들과 함께 고향 이타카로 향하는 배에 오른다.",
                    "그러나 그는 아직 알지 못했다.",
                    "트로이에서 이타카까지의 길이, 전쟁보다 긴 여정이 되리라는 것을."
            }, 4);

    /** 엔딩 타이틀에 쓰는 문구 */
    public static final String[] FINALE = {
            "ODYSSEY",
            "The Journey Home",
            "트로이의 전쟁은 끝났다.",
            "하지만 오디세우스의 이야기는 이제 시작이다."
    };

    /** 전투 시작 직전에 뜨는 짧은 안내 */
    public static final String[] BATTLE_TIP = {
            "탭 = 공격 · 길게 누르기 = 강공격",
            "방어 버튼을 누르고 있으면 피해가 줄어듭니다",
            "회피 중에는 잠깐 무적이 됩니다",
            "분노가 가득 차면 필살기를 쓸 수 있습니다"
    };
}
