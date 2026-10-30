package com.example.trojanprototype.battle;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.ui.Theme;

/**
 * 전투에 등장하는 캐릭터 한 명 (플레이어 / 적 공통).
 *
 * 담당: 유장원 (시스템)
 *
 * ── 캐릭터 상태 전이(FSM) ────────────────────────────────
 *
 *            ┌──────── 공격 끝 ────────┐
 *            ▼                         │
 *   IDLE ⇄ WALK ──공격 버튼──► ATTACK ─┘
 *    │  ▲                │
 *    │  └── 경직 끝 ──┐  ├─ 맞음 ─► HURT ─► IDLE
 *    │                │  │
 *    ├── 방어 ──► GUARD   └─ 체력 0 ─► DOWN (전투 종료)
 *    └── 회피 ──► DODGE ─► IDLE   (중간에 잠깐 무적)
 *
 * ── 공격 한 번의 시간 구성 ───────────────────────────────
 *
 *   |── 선딜(windup) ──|── 판정(active) ──|── 후딜(recover) ──|
 *      아직 안 맞음        이때만 맞음         반격당하기 쉬움
 *
 *   적 AI 는 이 "후딜" 구간을 노려 공격합니다. (EnemyBrain 참고)
 */
public class Fighter {

    // 상태 값
    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int ATTACK = 2;
    public static final int GUARD = 3;
    public static final int DODGE = 4;
    public static final int HURT = 5;
    public static final int DOWN = 6;
    public static final int VICTORY = 7;

    // 공격 진행 단계
    public static final int PHASE_WINDUP = 0;
    public static final int PHASE_ACTIVE = 1;
    public static final int PHASE_RECOVER = 2;

    // takeHit 결과
    public static final int RESULT_DODGED = 0;
    public static final int RESULT_GUARDED = 1;
    public static final int RESULT_HIT = 2;
    public static final int RESULT_GUARD_BREAK = 3;

    // ------------------------------------------------ 캐릭터 정보
    public String name = "";
    public String title = "";
    public Bitmap sprite;            // 한 장짜리 대기 그림 (UI · 프레임이 없을 때의 대체용)
    public Bitmap[] frames;          // 프레임 애니메이션 6장. 없으면 null (Assets.frames 참고)
    /** 화면에 그릴 "서 있는 키"(px). 프레임 칸 크기가 아니라 캐릭터 본체 기준입니다. */
    public float spriteHeight = 150f;
    public int accent = 0xFFE0A24C;

    public int maxHp = 100;
    public int hp = 100;
    public float rage = 0f;
    public float attackPower = 1f;
    public float moveSpeed = 180f;
    public float dodgeSpeed = 330f;
    public float damageTaken = 1f;   // 받는 피해 배수 (난이도 조절용)

    public GameData.Move lightMove;
    public GameData.Move heavyMove;
    public GameData.Move ultimateMove;

    // ------------------------------------------------ 현재 상태
    public float x = 300f;
    public float vx = 0f;
    public int facing = 1;           // +1 오른쪽, -1 왼쪽

    public int state = IDLE;
    public float stateTime = 0f;
    public GameData.Move move;       // 지금 쓰고 있는 기술
    public boolean usingUltimate = false;
    public int hitsDone = 0;         // 이번 기술로 때린 횟수
    public float hurtDuration = 0f;

    public float invincible = 0f;
    public float dodgeCooldown = 0f;
    public float flashTime = 0f;     // 맞았을 때 하얗게 번쩍이는 시간
    public int dodgeDir = 1;

    // 연속 공격(3타) 관리
    public int chainStep = 0;
    public float chainTimer = 0f;

    // 연속으로 맞은 횟수 — 무한 콤보를 막기 위해 셉니다.
    public int comboTaken = 0;
    public float sinceLastHurt = 99f;
    public boolean brokeOut = false;   // 방금 콤보에서 탈출했는지 (연출용)

    // 연출용
    public float animTime = 0f;

    private final RectF tmpBody = new RectF();
    private final RectF tmpAttack = new RectF();

    // ==================================================================
    //  준비
    // ==================================================================
    public void resetForBattle(float startX, int startFacing) {
        x = startX;
        facing = startFacing;
        vx = 0f;
        hp = maxHp;
        rage = 0f;
        state = IDLE;
        stateTime = 0f;
        move = null;
        usingUltimate = false;
        hitsDone = 0;
        invincible = 0f;
        dodgeCooldown = 0f;
        flashTime = 0f;
        chainStep = 0;
        chainTimer = 0f;
        animTime = 0f;
        comboTaken = 0;
        sinceLastHurt = 99f;
        brokeOut = false;
    }

    // ==================================================================
    //  상태 확인
    // ==================================================================
    public boolean isAlive() {
        return hp > 0 && state != DOWN;
    }

    /** 새 행동(공격/회피/이동)을 시작할 수 있는 상태인가 */
    public boolean canAct() {
        if (state == IDLE || state == WALK || state == GUARD) return true;
        // 공격 후딜 중에는 연속 공격으로 이어 갈 수 있습니다.
        if (state == ATTACK && attackPhase() == PHASE_RECOVER) return true;
        return false;
    }

    /** 자유롭게 걸어 다닐 수 있는 상태인가 */
    public boolean canMove() {
        return state == IDLE || state == WALK;
    }

    public int attackPhase() {
        if (state != ATTACK || move == null) return PHASE_RECOVER;
        if (stateTime < move.windup) return PHASE_WINDUP;
        if (stateTime < move.windup + move.active) return PHASE_ACTIVE;
        return PHASE_RECOVER;
    }

    public boolean isInvincible() {
        if (state == DODGE
                && stateTime >= GameData.DODGE_INVINCIBLE_START
                && stateTime <= GameData.DODGE_INVINCIBLE_END) {
            return true;
        }
        return invincible > 0f;
    }

    public float hpRatio() {
        return maxHp <= 0 ? 0f : (float) hp / (float) maxHp;
    }

    public boolean ultimateReady() {
        return rage >= GameData.RAGE_MAX && isAlive();
    }

    // ==================================================================
    //  행동 시작
    // ==================================================================
    /** 공격 버튼을 눌렀을 때. 3타째는 자동으로 강공격이 나갑니다. */
    public boolean tryAttack() {
        if (!canAct()) return false;

        int step = (chainTimer > 0f) ? chainStep : 0;
        GameData.Move next = (step >= 2) ? heavyMove : lightMove;
        startMove(next, false);
        chainStep = (step >= 2) ? 0 : step + 1;
        chainTimer = Theme.CHAIN_WINDOW;
        return true;
    }

    /** 적 AI 가 쓰는 공격 (연속 공격 없이 지정한 기술만) */
    public boolean tryMove(GameData.Move m) {
        if (m == null || !canAct()) return false;
        startMove(m, false);
        return true;
    }

    public boolean tryUltimate() {
        if (!canAct() || !ultimateReady() || ultimateMove == null) return false;
        rage = 0f;
        startMove(ultimateMove, true);
        invincible = Math.max(invincible, ultimateMove.windup); // 발동 순간 보호
        return true;
    }

    public boolean tryDodge(int dir) {
        if (!canMove() || dodgeCooldown > 0f) return false;
        state = DODGE;
        stateTime = 0f;
        dodgeDir = dir >= 0 ? 1 : -1;
        dodgeCooldown = GameData.DODGE_COOLDOWN;
        vx = dodgeDir * dodgeSpeed;
        return true;
    }

    public void setGuard(boolean on) {
        if (on) {
            if (state == IDLE || state == WALK) {
                state = GUARD;
                stateTime = 0f;
                vx = 0f;
            }
        } else if (state == GUARD) {
            state = IDLE;
            stateTime = 0f;
        }
    }

    private void startMove(GameData.Move m, boolean ultimate) {
        move = m;
        usingUltimate = ultimate;
        state = ATTACK;
        stateTime = 0f;
        hitsDone = 0;
        vx = 0f;
    }

    /** 걷기 입력. dir 은 -1 / 0 / +1 */
    public void walk(int dir) {
        if (!canMove()) return;
        if (dir == 0) {
            if (state == WALK) state = IDLE;
            vx = 0f;
        } else {
            state = WALK;
            vx = dir * moveSpeed;
        }
    }

    // ==================================================================
    //  피격 처리
    // ==================================================================
    /**
     * 공격을 맞았을 때 호출합니다.
     * @param attacker 때린 쪽
     * @param m        맞은 기술
     * @return RESULT_DODGED / RESULT_GUARDED / RESULT_HIT / RESULT_GUARD_BREAK
     */
    public int takeHit(Fighter attacker, GameData.Move m) {
        if (!isAlive()) return RESULT_DODGED;

        // 1) 회피 중 무적이면 안 맞습니다.
        if (isInvincible()) {
            rage = Math.min(GameData.RAGE_MAX, rage + GameData.RAGE_ON_DODGE);
            return RESULT_DODGED;
        }

        int awayDir = (attacker.x <= x) ? 1 : -1;  // 공격자 반대 방향
        boolean facingAttacker = (facing == -awayDir);
        boolean blocking = (state == GUARD) && facingAttacker;

        float dmg = m.damage * attacker.attackPower * damageTaken;

        if (blocking && !m.guardBreak) {
            // 2) 방어 성공 — 피해가 크게 줄고 살짝 밀려납니다.
            hp -= Math.max(1, Math.round(dmg * GameData.GUARD_DAMAGE_RATE));
            rage = Math.min(GameData.RAGE_MAX, rage + GameData.RAGE_ON_GUARD);
            vx = awayDir * m.knockback * 0.35f;
            stateTime = 0f;
            invincible = Math.max(invincible, GameData.GUARD_STUN * 0.5f);
            flashTime = 0.08f;
            if (hp <= 0) die();
            return RESULT_GUARDED;
        }

        // 3) 그대로 맞음
        hp -= Math.max(1, Math.round(dmg));
        rage = Math.min(GameData.RAGE_MAX, rage + GameData.RAGE_ON_HURT);
        vx = awayDir * m.knockback;
        facing = -awayDir;                    // 맞으면 공격자 쪽을 봅니다
        state = HURT;
        stateTime = 0f;
        hurtDuration = (blocking && m.guardBreak)
                ? GameData.GUARD_BREAK_STUN : GameData.HURT_TIME;
        flashTime = 0.14f;
        chainStep = 0;
        chainTimer = 0f;

        if (hp <= 0) {
            die();
            return RESULT_HIT;
        }

        // 연속으로 너무 많이 맞으면 밀려나면서 잠깐 무적이 됩니다.
        // 이 규칙이 없으면 공격 버튼만 연타해도 상대가 경직에서 못 빠져나오는
        // "무한 콤보"가 되어 전투가 성립하지 않습니다.
        comboTaken++;
        sinceLastHurt = 0f;
        brokeOut = false;
        if (comboTaken >= GameData.MAX_COMBO_HITS) {
            comboTaken = 0;
            brokeOut = true;
            invincible = GameData.COMBO_BREAK_INVINCIBLE;
            vx = awayDir * (m.knockback + 320f);
            hurtDuration = GameData.HURT_TIME * 0.6f;
        }
        return (blocking && m.guardBreak) ? RESULT_GUARD_BREAK : RESULT_HIT;
    }

    private void die() {
        hp = 0;
        state = DOWN;
        stateTime = 0f;
        move = null;
    }

    public void celebrate() {
        if (isAlive()) {
            state = VICTORY;
            stateTime = 0f;
            vx = 0f;
        }
    }

    // ==================================================================
    //  매 프레임 갱신
    // ==================================================================
    public void update(float dt) {
        animTime += dt;
        stateTime += dt;

        sinceLastHurt += dt;
        if (sinceLastHurt > GameData.COMBO_RESET_TIME) comboTaken = 0;
        if (flashTime > 0f) flashTime -= dt;
        if (invincible > 0f) invincible -= dt;
        if (dodgeCooldown > 0f) dodgeCooldown -= dt;
        if (chainTimer > 0f) {
            chainTimer -= dt;
            if (chainTimer <= 0f) chainStep = 0;
        }

        switch (state) {
            case ATTACK:
                if (move != null) {
                    if (attackPhase() == PHASE_ACTIVE) {
                        vx = facing * move.lunge;
                    } else {
                        vx = decay(vx, dt, 10f);
                    }
                    if (stateTime >= move.total()) {
                        move = null;
                        usingUltimate = false;
                        state = IDLE;
                        stateTime = 0f;
                        vx = 0f;
                    }
                } else {
                    state = IDLE;
                }
                break;

            case DODGE: {
                float p = stateTime / GameData.DODGE_TIME;
                vx = dodgeDir * dodgeSpeed * (1f - p) * 1.4f;
                if (stateTime >= GameData.DODGE_TIME) {
                    state = IDLE;
                    stateTime = 0f;
                    vx = 0f;
                }
                break;
            }

            case HURT:
                vx = decay(vx, dt, 7f);
                if (stateTime >= hurtDuration) {
                    state = IDLE;
                    stateTime = 0f;
                }
                break;

            case GUARD:
                vx = decay(vx, dt, 12f);
                break;

            case DOWN:
                vx = decay(vx, dt, 5f);
                break;

            default:
                // IDLE / WALK / VICTORY — vx 는 입력이 정해 줍니다.
                break;
        }

        x += vx * dt;

        // 무대 밖으로 못 나가게
        if (x < Theme.ARENA_LEFT) {
            x = Theme.ARENA_LEFT;
            if (vx < 0) vx = 0;
        }
        if (x > Theme.ARENA_RIGHT) {
            x = Theme.ARENA_RIGHT;
            if (vx > 0) vx = 0;
        }
    }

    private static float decay(float v, float dt, float rate) {
        float k = 1f - rate * dt;
        if (k < 0f) k = 0f;
        return v * k;
    }

    /** 공격 중이 아닐 때 상대 쪽을 바라보게 합니다. */
    public void faceTowards(float targetX) {
        if (state == ATTACK || state == HURT || state == DOWN) return;
        facing = (targetX >= x) ? 1 : -1;
    }

    // ==================================================================
    //  충돌 상자
    // ==================================================================
    public RectF bodyBox() {
        float half = GameData.BODY_W * 0.5f;
        tmpBody.set(x - half, Theme.GROUND - GameData.BODY_H, x + half, Theme.GROUND);
        return tmpBody;
    }

    /** 공격 판정 상자. 판정 구간이 아니면 null. */
    public RectF attackBox() {
        if (state != ATTACK || move == null) return null;
        if (attackPhase() != PHASE_ACTIVE) return null;
        float front = x + facing * (GameData.BODY_W * 0.5f);
        float l = (facing > 0) ? front : front - move.reach;
        float r = (facing > 0) ? front + move.reach : front;
        float top = Theme.GROUND - move.boxTop;
        tmpAttack.set(l, top, r, top + move.boxHeight);
        return tmpAttack;
    }

    /** 이 캐릭터의 공격이 닿는 최대 거리 (AI 판단용) */
    public float reachOf(GameData.Move m) {
        if (m == null) return 0f;
        return GameData.BODY_W * 0.5f + m.reach;
    }

    public static boolean overlap(RectF a, RectF b) {
        if (a == null || b == null) return false;
        return a.left < b.right && b.left < a.right
                && a.top < b.bottom && b.top < a.bottom;
    }
}
