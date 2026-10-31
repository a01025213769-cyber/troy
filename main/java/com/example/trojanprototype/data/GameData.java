package com.example.trojanprototype.data;

/**
 * 캐릭터 · 스테이지 · 난이도 · 기술 수치를 한곳에 모아 둔 파일.
 *
 * 담당: 심신(콘텐츠) + 유장원(밸런스 조정)
 * - 밸런스를 바꾸고 싶으면 이 파일의 숫자만 고치면 됩니다.
 * - 엔진 코드(Fighter, BattleWorld, EnemyBrain)는 고치지 않아도 됩니다.
 *
 * ★ 주인공은 플레이어가 고르지 않고 스토리가 정합니다 ★
 *   1~3장은 아킬레우스, 아킬레우스가 쓰러진 뒤 마지막 장은 오디세우스입니다.
 *   각 StageDef 의 hero 항목이 그 전투의 주인공입니다.
 */
public final class GameData {

    private GameData() {}

    // ============================================================
    //  기술(Move) 한 개의 정의
    // ============================================================
    public static final class Move {
        /** 프레임을 따로 지정하지 않은 기술이 쓰는 기본값 (_f4 한 장) */
        private static final int[] DEFAULT_FRAMES = {4};

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
        /** true 면 화살을 쏘는 원거리 공격입니다 (오디세우스의 활) */
        public final boolean ranged;
        /** 화살 속도(px/초). ranged 일 때만 사용 */
        public final float arrowSpeed;
        /**
         * 이 기술을 쓸 때 보여 줄 스프라이트 프레임 번호들.
         *
         *   [0]     선딜에 보여 줄 그림 (치켜드는 동작)
         *   [1]     판정 구간 그림 (실제로 베는 순간)
         *   [2...]  후딜에 차례로 보여 줄 그림 (휘두른 뒤 마무리)
         *
         * 한 장만 넣으면 공격 내내 그 그림만 나옵니다.
         * 기본값은 {4} = 공격 프레임(_f4) 한 장.
         */
        public final int[] attackFrames;

        public Move(String name, float windup, float active, float recover,
                    float reach, float boxHeight, float boxTop,
                    float damage, float knockback, float lunge,
                    int hits, float rageGain, boolean guardBreak) {
            this(name, windup, active, recover, reach, boxHeight, boxTop,
                    damage, knockback, lunge, hits, rageGain, guardBreak, false, 0f,
                    DEFAULT_FRAMES);
        }

        public Move(String name, float windup, float active, float recover,
                    float reach, float boxHeight, float boxTop,
                    float damage, float knockback, float lunge,
                    int hits, float rageGain, boolean guardBreak,
                    boolean ranged, float arrowSpeed) {
            this(name, windup, active, recover, reach, boxHeight, boxTop,
                    damage, knockback, lunge, hits, rageGain, guardBreak,
                    ranged, arrowSpeed, DEFAULT_FRAMES);
        }

        public Move(String name, float windup, float active, float recover,
                    float reach, float boxHeight, float boxTop,
                    float damage, float knockback, float lunge,
                    int hits, float rageGain, boolean guardBreak,
                    boolean ranged, float arrowSpeed, int[] attackFrames) {
            this.attackFrames = (attackFrames == null || attackFrames.length == 0)
                    ? DEFAULT_FRAMES : attackFrames;
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
            this.ranged = ranged;
            this.arrowSpeed = arrowSpeed;
        }

        /** 기술 전체 길이(초) */
        public float total() {
            return windup + active + recover;
        }
    }

    // ============================================================
    //  캐릭터
    // ============================================================
    public static final class CharacterDef {
        public final String name;        // 이름
        public final String title;       // 한 줄 수식어
        public final String spriteName;  // res/drawable-nodpi 안의 파일 이름
        public final int maxHp;
        public final float attack;       // 기본 공격력 (Move.damage 에 곱해짐)
        public final float moveSpeed;    // 걷기 속도 (px/초)
        public final float dodgeSpeed;   // 회피 이동 속도 (px/초)
        public final float spriteHeight; // 화면에 그릴 "몸" 높이(px)
        public final int accent;         // 캐릭터 강조 색
        public final String[] traits;    // 소개에 보여 줄 특징 3줄
        public final Move light;         // 일반 공격 (공격 버튼 탭)
        public final Move heavy;         // 강공격 (3타째 자동)
        public final Move ultimate;      // 필살기 (분노 100)

        /**
         * 적이 가까이 붙었을 때 쓰는 근접 기술. 없으면 null.
         * 오디세우스는 평소엔 활을 쏘다가 적이 meleeRange 안으로 들어오면
         * 자동으로 검으로 바꿔 싸웁니다.
         */
        public final Move meleeLight;
        public final Move meleeHeavy;
        /** 이 거리(두 캐릭터 중심 사이, px) 안이면 근접 기술을 씁니다. */
        public final float meleeRange;

        public CharacterDef(String name, String title, String spriteName, int maxHp,
                            float attack, float moveSpeed, float dodgeSpeed, float spriteHeight,
                            int accent, String[] traits, Move light, Move heavy, Move ultimate) {
            this(name, title, spriteName, maxHp, attack, moveSpeed, dodgeSpeed, spriteHeight,
                    accent, traits, light, heavy, ultimate, null, null, 0f);
        }

        public CharacterDef(String name, String title, String spriteName, int maxHp,
                            float attack, float moveSpeed, float dodgeSpeed, float spriteHeight,
                            int accent, String[] traits, Move light, Move heavy, Move ultimate,
                            Move meleeLight, Move meleeHeavy, float meleeRange) {
            this.meleeLight = meleeLight;
            this.meleeHeavy = meleeHeavy;
            this.meleeRange = meleeRange;
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

    // ------------------------------------------------------------
    //  아킬레우스 — 1~3장의 주인공. 긴 창, 높은 화력, 낮은 체력.
    //  사거리는 공격 프레임(achilles_sprite_f4)의 창끝에 맞춰 잡았습니다.
    // ------------------------------------------------------------
    public static final CharacterDef ACHILLES = new CharacterDef(
            "아킬레우스", "프티아의 분노", "achilles_sprite",
            100, 1.00f, 188f, 330f, 150f, 0xFFE0A24C,
            new String[]{"긴 창 사거리", "높은 공격력", "낮은 체력"},
            //        이름        선딜   판정   후딜   사거리 높이  위치  피해  넉백  전진 횟수 분노 가드브레이크
            new Move("창 찌르기", 0.11f, 0.09f, 0.18f, 130f, 64f, 112f, 10f, 150f, 40f, 1, 9f, false),
            new Move("관통 돌진", 0.24f, 0.11f, 0.30f, 148f, 58f, 110f, 18f, 300f, 220f, 1, 13f, true),
            new Move("분노의 돌격", 0.30f, 0.46f, 0.42f, 155f, 110f, 140f, 13f, 380f, 300f, 4, 0f, true)
    );

    // ------------------------------------------------------------
    //  오디세우스 — 마지막 장의 주인공. 활을 쓰는 원거리 캐릭터.
    //  공격 프레임(odysseus_sprite_f4)이 활시위를 당기는 모습이라
    //  근접 공격이 아니라 화살을 쏘도록 만들었습니다.
    // ------------------------------------------------------------
    public static final CharacterDef ODYSSEUS = new CharacterDef(
            "오디세우스", "이타카의 지략", "odysseus_sprite",
            126, 1.00f, 182f, 380f, 146f, 0xFF58A0C8,
            new String[]{"활을 쏘는 원거리", "빠른 회피", "접근당하면 불리"},
            //       활 공격은 reach 대신 화살이 날아가 맞힙니다.
            new Move("화살", 0.16f, 0.06f, 0.38f, 60f, 40f, 104f, 9f, 120f, 0f, 1, 10f, false,
                    true, 760f),
            new Move("강궁", 0.30f, 0.06f, 0.42f, 60f, 44f, 106f, 17f, 300f, 0f, 1, 14f, true,
                    true, 920f),
            new Move("연속 사격", 0.24f, 0.50f, 0.36f, 60f, 40f, 104f, 10f, 160f, 0f, 5, 0f, true,
                    true, 840f),
            // 적이 붙었을 때 쓰는 검. 활 모션(f4) 대신 검을 든 걸음 프레임(f2)을 보여 줍니다.
            //        이름        선딜   판정   후딜  사거리 높이  위치  피해  넉백  전진 횟수 분노 가드브레이크 원거리 화살속도 프레임
            // 검 동작은 f6~f11 에 들어 있습니다.
            //   f6 치켜듦 → f7 찌르기 → f8 내려베기 → f9 마무리
            //   f10 크게 치켜듦 → f11 돌진 베기 → f8 → f9
            new Move("돌진 베기", 0.10f, 0.14f, 0.24f, 92f, 70f, 116f, 9f, 240f, 1250f, 1, 10f,
                    false, false, 0f, new int[]{6, 7, 8, 9}),
            new Move("돌진 올려베기", 0.22f, 0.16f, 0.32f, 102f, 86f, 126f, 16f, 360f, 1350f, 1, 14f,
                    true, false, 0f, new int[]{10, 11, 8, 9}),
            275f   // 이 거리 안으로 들어오면 활 대신 검 (파고들어 벱니다)
    );

    /** 소개 화면 등에서 쓰는 목록 (플레이어가 고르는 용도가 아닙니다) */
    public static final CharacterDef[] HEROES = {ACHILLES, ODYSSEUS};

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
        /** 이 전투의 주인공 (플레이어가 고르지 않고 스토리가 정합니다) */
        public final CharacterDef hero;
        public final String enemyName;    // 적 이름
        public final String enemyTitle;   // 적 한 줄 설명
        public final String spriteName;   // 적 스프라이트
        public final int backdrop;        // 배경 종류 (0 해안 / 1 성벽 / 2 야간 왕궁)
        public final int enemyHp;
        public final float enemyAttack;
        public final float enemyMoveSpeed;
        public final float enemySpriteHeight;
        public final boolean boss;
        /**
         * true 면 1대1 결투가 아니라 군대 대 군대 전투입니다.
         * 이때 enemy* 항목은 "트로이 대장" 의 능력치가 되고,
         * 나머지 병사 구성은 아래 ARMY_ 로 시작하는 상수와 GREEK_·TROY_ 로 시작하는 병사 정의를 따릅니다.
         */
        public final boolean army;
        public final int timeLimit;       // 제한 시간(초)
        public final Move enemyLight;
        public final Move enemyHeavy;
        /** 승리 후 결과 화면에 뜨는 해설 */
        public final String victoryNote;
        /** 패배 후 결과 화면에 뜨는 해설 */
        public final String defeatNote;

        /** 1대1 결투 스테이지 */
        public StageDef(String chapter, String place, CharacterDef hero,
                        String enemyName, String enemyTitle,
                        String spriteName, int backdrop, int enemyHp, float enemyAttack,
                        float enemyMoveSpeed, float enemySpriteHeight, boolean boss, int timeLimit,
                        Move enemyLight, Move enemyHeavy,
                        String victoryNote, String defeatNote) {
            this(chapter, place, hero, enemyName, enemyTitle, spriteName, backdrop, enemyHp,
                    enemyAttack, enemyMoveSpeed, enemySpriteHeight, boss, false, timeLimit,
                    enemyLight, enemyHeavy, victoryNote, defeatNote);
        }

        public StageDef(String chapter, String place, CharacterDef hero,
                        String enemyName, String enemyTitle,
                        String spriteName, int backdrop, int enemyHp, float enemyAttack,
                        float enemyMoveSpeed, float enemySpriteHeight, boolean boss, boolean army,
                        int timeLimit,
                        Move enemyLight, Move enemyHeavy,
                        String victoryNote, String defeatNote) {
            this.army = army;
            this.chapter = chapter;
            this.place = place;
            this.hero = hero;
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

    // ============================================================
    //  군대 전투 — 병사 정의
    //
    //  한 군대 = 대장 1 + 돌격군 3(검과 방패) + 궁수 2(활)
    //   · 그리스군 대장 = 플레이어(아킬레우스)
    //   · 트로이군 대장 = 일반 병사의 "보스 개체" (StageDef 의 enemy* 항목)
    //  먼저 상대 대장을 쓰러뜨리는 쪽이 이깁니다.
    //  병사 그림은 오디세우스 한 벌(검·방패·활)을 진영 색으로 물들여 씁니다.
    // ============================================================
    public static final int ARMY_ASSAULT_COUNT = 3;
    public static final int ARMY_ARCHER_COUNT = 2;

    public static final String GREEK_ARMY_NAME = "그리스군";
    public static final String TROY_ARMY_NAME = "트로이군";

    /** 군대 전투에서 플레이어(대장) 체력 배수 — 여러 명에게 둘러싸이기 때문 */
    public static final float ARMY_PLAYER_HP_SCALE = 2.4f;
    /** 군대 전투에서 일반 병사끼리 주고받는 피해 배수 */
    public static final float ARMY_SOLDIER_DAMAGE = 0.8f;

    public static final class UnitDef {
        public final String name;
        public final String spriteName;
        public final int maxHp;
        public final float attack;
        public final float moveSpeed;
        public final float spriteHeight;
        public final Move light;
        public final Move heavy;
        public final boolean archer;

        public UnitDef(String name, String spriteName, int maxHp, float attack, float moveSpeed,
                       float spriteHeight, Move light, Move heavy, boolean archer) {
            this.name = name;
            this.spriteName = spriteName;
            this.maxHp = maxHp;
            this.attack = attack;
            this.moveSpeed = moveSpeed;
            this.spriteHeight = spriteHeight;
            this.light = light;
            this.heavy = heavy;
            this.archer = archer;
        }
    }

    // 돌격군의 검 동작: f6 치켜듦 → f7 베기 → f8 → f9 / 올려베기 f10 → f11 → f8 → f9
    private static final Move ASSAULT_LIGHT =
            new Move("검격", 0.26f, 0.12f, 0.36f, 86f, 70f, 116f, 9f, 150f, 150f, 1, 0f,
                    false, false, 0f, new int[]{6, 7, 8, 9});
    private static final Move ASSAULT_HEAVY =
            new Move("방패 올려베기", 0.40f, 0.14f, 0.46f, 96f, 86f, 126f, 14f, 260f, 200f, 1, 0f,
                    true, false, 0f, new int[]{10, 11, 8, 9});

    // 궁수의 활: 시위를 당기는 그림(f4)이 곧 공격 동작입니다.
    private static final Move ARCHER_LIGHT =
            new Move("화살", 0.42f, 0.06f, 0.70f, 60f, 40f, 104f, 7f, 100f, 0f, 1, 0f, false,
                    true, 700f);
    private static final Move ARCHER_HEAVY =
            new Move("강궁", 0.65f, 0.06f, 0.80f, 60f, 44f, 106f, 12f, 240f, 0f, 1, 0f, true,
                    true, 860f);

    public static final UnitDef GREEK_ASSAULT = new UnitDef(
            "그리스 돌격병", "greek_soldier_sprite", 52, 1.0f, 150f, 128f,
            ASSAULT_LIGHT, ASSAULT_HEAVY, false);
    public static final UnitDef GREEK_ARCHER = new UnitDef(
            "그리스 궁수", "greek_soldier_sprite", 36, 1.0f, 140f, 124f,
            ARCHER_LIGHT, ARCHER_HEAVY, true);
    public static final UnitDef TROY_ASSAULT = new UnitDef(
            "트로이 돌격병", "troy_soldier_sprite", 52, 1.0f, 150f, 128f,
            ASSAULT_LIGHT, ASSAULT_HEAVY, false);
    public static final UnitDef TROY_ARCHER = new UnitDef(
            "트로이 궁수", "troy_soldier_sprite", 36, 1.0f, 140f, 124f,
            ARCHER_LIGHT, ARCHER_HEAVY, true);

    /**
     * 시작 배치. 인덱스 0~2 = 돌격군, 3~4 = 궁수.  (플레이어/대장은 따로 정합니다)
     * x 는 가상 화면 좌표, lane 은 앞뒤 줄(세로 오프셋, px) 입니다.
     * 그리스군 줄과 트로이군 줄을 서로 어긋나게 둬서 한 줄로 엉키지 않게 합니다.
     */
    public static final float[] GREEK_UNIT_X = {250f, 270f, 230f, 150f, 125f};
    public static final float[] GREEK_UNIT_LANE = {-24f, 18f, -2f, -12f, 26f};
    public static final float GREEK_HERO_X = 330f;
    public static final float GREEK_HERO_LANE = 4f;

    public static final float[] TROY_UNIT_X = {690f, 710f, 670f, 800f, 825f};
    public static final float[] TROY_UNIT_LANE = {-20f, 22f, 2f, -10f, 28f};
    public static final float TROY_CMDR_X = 750f;
    public static final float TROY_CMDR_LANE = -4f;

    // 적 기술 — 사거리는 각 캐릭터 공격 프레임(_f4)의 무기 끝에 맞췄습니다.
    private static final Move SOLDIER_LIGHT =
            new Move("찌르기", 0.22f, 0.10f, 0.32f, 124f, 64f, 114f, 9f, 170f, 60f, 1, 0f, false);
    private static final Move SOLDIER_HEAVY =
            new Move("내려치기", 0.36f, 0.12f, 0.44f, 136f, 92f, 132f, 15f, 260f, 120f, 1, 0f, true);

    private static final Move HECTOR_LIGHT =
            new Move("검격", 0.17f, 0.10f, 0.26f, 116f, 70f, 118f, 11f, 190f, 90f, 1, 0f, false);
    private static final Move HECTOR_HEAVY =
            new Move("성벽의 일격", 0.30f, 0.14f, 0.38f, 132f, 100f, 138f, 19f, 360f, 220f, 1, 0f, true);

    private static final Move GUARD_LIGHT =
            new Move("창 찌르기", 0.20f, 0.10f, 0.30f, 128f, 64f, 114f, 10f, 180f, 70f, 1, 0f, false);
    private static final Move GUARD_HEAVY =
            new Move("방패 밀치기", 0.34f, 0.12f, 0.42f, 108f, 92f, 130f, 16f, 300f, 140f, 1, 0f, true);

    // 트로이 대장 — 일반 병사의 창을 키운 "보스 개체"
    private static final Move CMDR_LIGHT =
            new Move("대장의 창", 0.20f, 0.10f, 0.30f, 130f, 66f, 116f, 10f, 170f, 70f, 1, 0f, false);
    private static final Move CMDR_HEAVY =
            new Move("지휘관의 일격", 0.34f, 0.13f, 0.42f, 146f, 92f, 132f, 17f, 300f, 160f, 1, 0f, true);

    public static final StageDef[] STAGES = {
            // CHAPTER 1·2 는 군대 전투입니다. (enemy* = 트로이 대장)
            new StageDef(
                    "CHAPTER 1", "트로이 해안", ACHILLES,
                    "트로이 대장", "해안 수비대를 이끄는 지휘관",
                    "troy_sprite", 0, 240, 0.95f, 150f, 172f, true, true, 150,
                    CMDR_LIGHT, CMDR_HEAVY,
                    "그리스군이 해안에 상륙했다. 그러나 트로이의 성벽은 아직 멀리 있었다.",
                    "상륙은 막혔다. 숨을 고르고 다시 해안으로 나아가자."
            ),
            new StageDef(
                    "CHAPTER 2", "트로이 성벽 아래", ACHILLES,
                    "트로이 정예 대장", "성벽을 지키는 최정예의 우두머리",
                    "troy_sprite", 1, 290, 1.05f, 165f, 176f, true, true, 150,
                    CMDR_LIGHT, CMDR_HEAVY,
                    "성벽 앞의 전선을 밀어냈다. 그래도 전쟁은 끝나지 않는다.",
                    "성벽은 높았다. 방어와 회피를 섞어 다시 도전해 보자."
            ),
            new StageDef(
                    "CHAPTER 3", "트로이 성문 앞 · 결투", ACHILLES,
                    "헥토르", "트로이의 왕자",
                    "hector_sprite", 2, 165, 1.12f, 168f, 156f, true, 120,
                    HECTOR_LIGHT, HECTOR_HEAVY,
                    "트로이 최강의 전사가 쓰러졌다. 전쟁의 저울이 기울기 시작한다.",
                    "헥토르는 조국을 등지지 않았다. 빈틈을 노려 다시 겨뤄 보자."
            ),
            // 아킬레우스가 쓰러진 뒤, 오디세우스로 치르는 마지막 전투
            new StageDef(
                    "FINAL", "불타는 트로이 왕궁", ODYSSEUS,
                    "왕궁 수비대장", "마지막까지 성문을 지킨 자",
                    "troy_sprite", 2, 138, 1.10f, 172f, 144f, true, 120,
                    GUARD_LIGHT, GUARD_HEAVY,
                    "트로이의 마지막 저항이 끝났다. 10년의 전쟁이 막을 내린다.",
                    "성은 아직 함락되지 않았다. 거리를 벌리며 화살을 노려 보자."
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

    /** 화살이 사라지기까지의 시간(초) */
    public static final float ARROW_LIFE = 1.6f;
    /**
     * 적 AI 가 "원거리 공격이 날아온다"고 인식하는 거리.
     * 활의 실제 사거리는 화면 끝까지지만, 이 값을 쓰지 않으면
     * 적이 화면 반대편에서도 계속 방어만 하게 됩니다.
     */
    public static final float RANGED_AI_THREAT_RANGE = 260f;
}
