package com.example.trojanprototype.data;

/**
 * 캐릭터 · 스테이지 · 난이도 · 기술 수치를 한곳에 모아 둔 파일.
 *
 * 담당: 심신(콘텐츠) + 유장원(밸런스 조정)
 * - 밸런스를 바꾸고 싶으면 이 파일의 숫자만 고치면 됩니다.
 * - 엔진 코드(Fighter, BattleWorld, EnemyBrain)는 고치지 않아도 됩니다.
 */
public final class GameData {

    private GameData() {}

    // ============================================================
    //  기술(Move) 한 개의 정의
    // ============================================================
    public static final class Move {
        /** 화면에 뜨는 기술 이름 */
        public final String name;
        /** 선딜 — 버튼을 누르고 공격이 나가기까지 걸리는 시간(초) */
        public final float windup;
        /** 판정 — 실제로 상대를 때릴 수 있는 시간(초) */
        public final float active;
        /** 후딜 — 공격이 끝나고 다시 움직일 수 있을 때까지(초) */
        public final float recover;
        /** 공격 판정 상자가 몸 앞쪽으로 뻗는 길이(px) */
        public final float reach;
        /** 공격 판정 상자의 세로 길이(px) */
        public final float boxHeight;
        /** 바닥에서 공격 판정 상자 위쪽까지의 높이(px) */
        public final float boxTop;
        /** 피해량 (캐릭터 공격력에 곱해집니다) */
        public final float damage;
        /** 맞은 상대를 밀어내는 힘(px/초) */
        public final float knockback;
        /** 공격하는 동안 앞으로 전진하는 속도(px/초) */
        public final float lunge;
        /** 몇 번 때리는 기술인지 (연속 찌르기 = 3) */
        public final int hits;
        /** 이 기술을 맞혔을 때 차는 분노 게이지 */
        public final float rageGain;
        /** true 면 상대가 방어해도 막히지 않습니다 (가드 브레이크) */
        public final boolean guardBreak;

        public Move(String name, float windup, float active, float recover,
                    float reach, float boxHeight, float boxTop,
                    float damage, float knockback, float lunge,
                    int hits, float rageGain, boolean guardBreak) {
            this.name = name;
            this.windup = windup;
            this.active = active;
            this.recover = recover;
            this.reach = reach;
            this.boxHeight = boxHeight;
            this.boxTop = boxTop;
            this.damage = damage;
            this.knockback = knockback;
            this.lunge = lunge;
            this.hits = hits;
            this.rageGain = rageGain;
            this.guardBreak = guardBreak;
        }

        /** 기술 전체 길이(초) */
        public float total() {
            return windup + active + recover;
        }
    }

    // ============================================================
    //  플레이어 캐릭터
    // ============================================================
    public static final class CharacterDef {
        public final String name;        // 이름
        public final String title;       // 한 줄 수식어
        public final String spriteName;  // res/drawable-nodpi 안의 파일 이름
        public final int maxHp;
        public final float attack;       // 기본 공격력 (Move.damage 에 곱해짐)
        public final float moveSpeed;    // 걷기 속도 (px/초)
        public final float dodgeSpeed;   // 회피 이동 속도 (px/초)
        public final float spriteHeight; // 화면에 그릴 키(px)
        public final int accent;         // 캐릭터 강조 색
        public final String[] traits;    // 선택 화면에 보여 줄 특징 3줄
        public final Move light;         // 일반 공격 (공격 버튼 탭)
        public final Move heavy;         // 강공격 (공격 버튼 길게)
        public final Move ultimate;      // 필살기 (분노 100)

        public CharacterDef(String name, String title, String spriteName, int maxHp,
                            float attack, float moveSpeed, float dodgeSpeed, float spriteHeight,
                            int accent, String[] traits, Move light, Move heavy, Move ultimate) {
            this.name = name;
            this.title = title;
            this.spriteName = spriteName;
            this.maxHp = maxHp;
            this.attack = attack;
            this.moveSpeed = moveSpeed;
            this.dodgeSpeed = dodgeSpeed;
            this.spriteHeight = spriteHeight;
            this.accent = accent;
            this.traits = traits;
            this.light = light;
            this.heavy = heavy;
            this.ultimate = ultimate;
        }
    }

    /** 아킬레우스 — 공격력이 높고 체력이 낮은 공격형 */
    public static final CharacterDef ACHILLES = new CharacterDef(
            "아킬레우스", "프티아의 분노", "achilles_sprite",
            100, 1.00f, 188f, 330f, 150f, 0xFFE0A24C,
            new String[]{"높은 공격력", "짧은 선딜", "낮은 체력"},
            //        이름        선딜   판정   후딜   사거리 높이  위치  피해  넉백  전진 횟수 분노 가드브레이크
            new Move("베기", 0.10f, 0.09f, 0.17f, 92f, 72f, 118f, 11f, 150f, 40f, 1, 9f, false),
            new Move("창 찌르기", 0.24f, 0.11f, 0.30f, 128f, 60f, 112f, 20f, 300f, 220f, 1, 13f, true),
            new Move("분노의 돌격", 0.30f, 0.46f, 0.42f, 150f, 110f, 140f, 15f, 380f, 300f, 4, 0f, true)
    );

    /** 오디세우스 — 체력이 높고 회피가 빠른 균형형 */
    public static final CharacterDef ODYSSEUS = new CharacterDef(
            "오디세우스", "이타카의 지략", "greek_sprite",
            120, 0.84f, 176f, 390f, 148f, 0xFF58A0C8,
            new String[]{"높은 체력", "빠른 회피", "긴 사거리"},
            new Move("장창 찌르기", 0.15f, 0.09f, 0.19f, 104f, 56f, 114f, 10f, 160f, 30f, 1, 10f, false),
            new Move("방패 밀치기", 0.20f, 0.12f, 0.28f, 104f, 80f, 122f, 16f, 420f, 150f, 1, 14f, true),
            new Move("연속 찌르기", 0.24f, 0.54f, 0.38f, 140f, 62f, 120f, 12f, 200f, 170f, 5, 0f, true)
    );

    public static final CharacterDef[] PLAYABLE = {ACHILLES, ODYSSEUS};

    // ============================================================
    //  난이도 — 적 AI 의 반응 속도와 성향을 바꿉니다
    // ============================================================
    public static final class Difficulty {
        public final String name;
        public final String note;
        /** AI 가 다음 행동을 다시 고르기까지의 간격(초). 짧을수록 똑똑해 보입니다. */
        public final float reactionTime;
        /** 공격 성향 (0~1). 높을수록 먼저 달려듭니다. */
        public final float aggression;
        /** 방어/회피 성향 (0~1). 높을수록 잘 막습니다. */
        public final float defensiveness;
        /** 적 피해량 배수 */
        public final float damageScale;

        public Difficulty(String name, String note, float reactionTime,
                          float aggression, float defensiveness, float damageScale) {
            this.name = name;
            this.note = note;
            this.reactionTime = reactionTime;
            this.aggression = aggression;
            this.defensiveness = defensiveness;
            this.damageScale = damageScale;
        }
    }

    public static final Difficulty[] DIFFICULTIES = {
            new Difficulty("쉬움", "적이 느리게 판단합니다", 0.58f, 0.40f, 0.30f, 0.75f),
            new Difficulty("보통", "기본 난이도", 0.34f, 0.64f, 0.58f, 1.00f),
            new Difficulty("어려움", "적이 빈틈을 노립니다", 0.22f, 0.80f, 0.85f, 1.30f)
    };

    // ============================================================
    //  스테이지 (= 전투 한 판)
    // ============================================================
    public static final class StageDef {
        public final String chapter;      // "CHAPTER 1" 같은 표시
        public final String place;        // 장소 이름
        public final String enemyName;    // 적 이름
        public final String enemyTitle;   // 적 한 줄 설명
        public final String spriteName;   // 적 스프라이트
        public final int backdrop;        // 배경 종류 (0 해안 / 1 성벽 / 2 야간 왕궁)
        public final int enemyHp;
        public final float enemyAttack;
        public final float enemyMoveSpeed;
        public final float enemySpriteHeight;
        public final boolean boss;
        public final int timeLimit;       // 제한 시간(초)
        public final Move enemyLight;
        public final Move enemyHeavy;
        /** 승리 후 결과 화면에 뜨는 해설 */
        public final String victoryNote;
        /** 패배 후 결과 화면에 뜨는 해설 */
        public final String defeatNote;

        public StageDef(String chapter, String place, String enemyName, String enemyTitle,
                        String spriteName, int backdrop, int enemyHp, float enemyAttack,
                        float enemyMoveSpeed, float enemySpriteHeight, boolean boss, int timeLimit,
                        Move enemyLight, Move enemyHeavy,
                        String victoryNote, String defeatNote) {
            this.chapter = chapter;
            this.place = place;
            this.enemyName = enemyName;
            this.enemyTitle = enemyTitle;
            this.spriteName = spriteName;
            this.backdrop = backdrop;
            this.enemyHp = enemyHp;
            this.enemyAttack = enemyAttack;
            this.enemyMoveSpeed = enemyMoveSpeed;
            this.enemySpriteHeight = enemySpriteHeight;
            this.boss = boss;
            this.timeLimit = timeLimit;
            this.enemyLight = enemyLight;
            this.enemyHeavy = enemyHeavy;
            this.victoryNote = victoryNote;
            this.defeatNote = defeatNote;
        }
    }

    private static final Move SOLDIER_LIGHT =
            new Move("찌르기", 0.22f, 0.10f, 0.34f, 106f, 64f, 114f, 9f, 170f, 60f, 1, 0f, false);
    private static final Move SOLDIER_HEAVY =
            new Move("내려치기", 0.36f, 0.12f, 0.46f, 118f, 92f, 132f, 15f, 260f, 120f, 1, 0f, true);

    private static final Move HECTOR_LIGHT =
            new Move("검격", 0.17f, 0.10f, 0.26f, 114f, 70f, 118f, 11f, 190f, 80f, 1, 0f, false);
    private static final Move HECTOR_HEAVY =
            new Move("성벽의 일격", 0.30f, 0.14f, 0.38f, 138f, 100f, 138f, 19f, 360f, 210f, 1, 0f, true);

    public static final StageDef[] STAGES = {
            new StageDef(
                    "CHAPTER 1", "트로이 해안", "트로이 병사", "해안을 지키는 수비대",
                    "troy_sprite", 0, 90, 1.00f, 140f, 140f, false, 90,
                    SOLDIER_LIGHT, SOLDIER_HEAVY,
                    "그리스군이 해안에 상륙했다. 그러나 트로이의 성벽은 아직 멀리 있었다.",
                    "상륙은 막혔다. 숨을 고르고 다시 해안으로 나아가자."
            ),
            new StageDef(
                    "CHAPTER 2", "트로이 성벽 아래", "트로이 정예병", "성벽을 지키는 최정예",
                    "troy_sprite", 1, 120, 1.15f, 155f, 142f, false, 90,
                    SOLDIER_LIGHT, SOLDIER_HEAVY,
                    "성벽 앞의 전선을 밀어냈다. 그래도 전쟁은 끝나지 않는다.",
                    "성벽은 높았다. 방어와 회피를 섞어 다시 도전해 보자."
            ),
            new StageDef(
                    "CHAPTER 3", "트로이 성문 앞 · 결투", "헥토르", "트로이의 왕자",
                    "hector_sprite", 2, 180, 1.25f, 168f, 156f, true, 120,
                    HECTOR_LIGHT, HECTOR_HEAVY,
                    "트로이 최강의 전사가 쓰러졌다. 전쟁의 저울이 기울기 시작한다.",
                    "헥토르는 조국을 등지지 않았다. 빈틈을 노려 다시 겨뤄 보자."
            )
    };

    // ============================================================
    //  전투 공통 규칙
    // ============================================================
    /** 방어에 성공했을 때 피해가 줄어드는 비율 (0.25 = 25% 만 들어옴) */
    public static final float GUARD_DAMAGE_RATE = 0.25f;
    /** 피격 경직 시간(초) */
    public static final float HURT_TIME = 0.26f;
    /** 방어 성공 시 경직 시간(초) */
    public static final float GUARD_STUN = 0.12f;
    /** 가드 브레이크를 맞았을 때 경직 시간(초) */
    public static final float GUARD_BREAK_STUN = 0.55f;
    /** 회피 전체 시간(초) */
    public static final float DODGE_TIME = 0.38f;
    /** 회피 중 무적이 시작/끝나는 시점(초) */
    public static final float DODGE_INVINCIBLE_START = 0.05f;
    public static final float DODGE_INVINCIBLE_END = 0.26f;
    /** 회피 후 다시 회피할 수 있을 때까지(초) */
    public static final float DODGE_COOLDOWN = 0.55f;
    /** 맞았을 때 화면이 잠깐 멈추는 시간(초) — 타격감 */
    public static final float HIT_STOP = 0.06f;
    /** 콤보가 이어지는 것으로 인정되는 간격(초) */
    public static final float COMBO_WINDOW = 1.25f;
    /** 연속으로 이만큼 맞으면 밀려나며 콤보에서 탈출합니다 (무한 콤보 방지) */
    public static final int MAX_COMBO_HITS = 6;
    /** 콤보 탈출 시 주어지는 무적 시간(초) */
    public static final float COMBO_BREAK_INVINCIBLE = 0.5f;
    /** 이 시간 동안 맞지 않으면 연속 피격 횟수가 초기화됩니다(초) */
    public static final float COMBO_RESET_TIME = 0.9f;
    /** 필살기에 필요한 분노 게이지 */
    public static final float RAGE_MAX = 100f;
    /** 피해를 입었을 때 차는 분노 */
    public static final float RAGE_ON_HURT = 11f;
    /** 방어했을 때 차는 분노 */
    public static final float RAGE_ON_GUARD = 6f;
    /** 회피에 성공했을 때 차는 분노 */
    public static final float RAGE_ON_DODGE = 9f;
    /** 캐릭터 몸통 판정 상자 크기 */
    public static final float BODY_W = 58f;
    public static final float BODY_H = 124f;
}
