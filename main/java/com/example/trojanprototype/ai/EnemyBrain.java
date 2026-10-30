package com.example.trojanprototype.ai;

import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.data.GameData;

import java.util.Random;

/**
 * 적 AI — 상태 기계(FSM) + 유틸리티 기반 행동 선택.
 *
 * 담당: 유장원 (시스템 / AI)
 *
 * ── 어떻게 판단하는가 ─────────────────────────────────────
 *
 *  1) 일정 시간(reactionTime)마다 한 번씩 "지금 뭘 할까?"를 다시 정합니다.
 *     사람도 즉각 반응하지 못하므로, 이 간격이 곧 난이도가 됩니다.
 *       쉬움 0.62초 / 보통 0.38초 / 어려움 0.22초
 *
 *  2) 가능한 행동 7가지마다 점수(utility)를 계산합니다.
 *       대기 · 접근 · 후퇴 · 공격 · 강공격 · 방어 · 회피
 *     점수는 "거리 / 내 체력 / 상대가 지금 뭘 하고 있는지"로 결정됩니다.
 *
 *  3) 점수가 가장 높은 행동을 고르고, 다음 판단 때까지 그 행동을 유지합니다.
 *
 *  ── 발표 시연 포인트 ──────────────────────────────────────
 *  AI 버튼을 누르면 위 점수와 선택 이유가 화면에 그대로 표시됩니다.
 *  (AiDebugView 가 이 클래스의 scores / chosen / reason 을 읽어 그립니다)
 *
 *  ── 왜 if-else 가 아니라 점수 방식인가 ────────────────────
 *  if-else 로 짜면 조건이 늘어날수록 분기가 폭발합니다.
 *  점수 방식은 조건을 "가중치 한 줄"로 추가할 수 있고,
 *  각 행동이 왜 선택됐는지 숫자로 설명할 수 있어 시연에 유리합니다.
 */
public class EnemyBrain {

    public static final int ACT_WAIT = 0;
    public static final int ACT_APPROACH = 1;
    public static final int ACT_RETREAT = 2;
    public static final int ACT_ATTACK = 3;
    public static final int ACT_HEAVY = 4;
    public static final int ACT_GUARD = 5;
    public static final int ACT_DODGE = 6;
    public static final int ACT_COUNT = 7;

    public static final String[] ACTION_NAMES = {
            "대기", "접근", "후퇴", "공격", "강공격", "방어", "회피"
    };

    // ------------------------------------------------ 설정
    private GameData.Difficulty difficulty;
    private boolean boss;
    private GameData.Move lightMove;
    private GameData.Move heavyMove;

    // ------------------------------------------------ 디버그 표시용 (읽기 전용으로 사용)
    public final float[] scores = new float[ACT_COUNT];
    public int chosen = ACT_WAIT;
    public String reason = "전투 준비";
    public float distance = 0f;
    public float decisionTimer = 0f;
    public boolean sawIncoming = false;   // 상대가 공격을 시작했다고 인식
    public boolean sawOpening = false;    // 상대가 빈틈을 보였다고 인식
    public boolean sawGuardBreak = false; // 들어오는 공격이 방어로 막히지 않는 기술
    public boolean sawCounterChance = false; // 막아 낸 직후의 반격 기회
    public boolean sawTargetGuarding = false; // 상대가 방어를 올리고 있음
    public boolean guardTired = false;    // 너무 오래 막아서 방어 선택이 억제된 상태
    public boolean enraged = false;       // 보스 분노 상태

    // ------------------------------------------------ 내부 상태
    /** 공격 재사용 대기 시간 — 디버그 패널에서 "왜 지금 공격하지 않는가"를 설명합니다 */
    public float attackCooldown = 0f;
    private float heavyCooldown = 0f;
    private float guardHoldTime = 0f;     // 연속으로 방어한 시간
    private float counterWindow = 0f;     // 막아 낸 직후의 반격 기회 시간
    private float dodgeRest = 0f;         // 회피만 반복하지 않도록 두는 간격
    private boolean targetWasAttacking = false;
    private final Random rnd = new Random();

    /** 이 시간을 넘겨 계속 막으면 방어 점수가 크게 떨어집니다 (가드 굳히기 방지) */
    private static final float MAX_GUARD_HOLD = 0.85f;
    /** 막아 낸 뒤 반격이 유리해지는 시간 */
    private static final float COUNTER_WINDOW = 0.7f;
    /** 회피를 쓴 뒤 다시 회피를 고르기까지 두는 간격 */
    private static final float DODGE_REST = 1.3f;
    /** 사거리 끝이 아니라 이만큼 안쪽까지 파고들도록 합니다 */
    private static final float APPROACH_MARGIN = 28f;

    public void configure(GameData.Difficulty diff, boolean isBoss,
                          GameData.Move light, GameData.Move heavy) {
        this.difficulty = diff;
        this.boss = isBoss;
        this.lightMove = light;
        this.heavyMove = heavy;
        reset();
    }

    public void reset() {
        chosen = ACT_WAIT;
        reason = "전투 준비";
        decisionTimer = 0.6f;     // 전투 시작 직후 잠깐 멈칫
        attackCooldown = 0f;
        heavyCooldown = 0f;
        guardHoldTime = 0f;
        counterWindow = 0f;
        dodgeRest = 0f;
        targetWasAttacking = false;
        enraged = false;
        guardTired = false;
        for (int i = 0; i < ACT_COUNT; i++) scores[i] = 0f;
    }

    // ==================================================================
    //  매 프레임 호출
    // ==================================================================
    public void update(float dt, Fighter self, Fighter target) {
        if (difficulty == null) return;

        if (attackCooldown > 0f) attackCooldown -= dt;
        if (heavyCooldown > 0f) heavyCooldown -= dt;
        if (counterWindow > 0f) counterWindow -= dt;
        if (dodgeRest > 0f) dodgeRest -= dt;

        // 계속 막고만 있으면 "지친" 상태가 되어 방어 점수가 떨어집니다.
        // 이게 없으면 적이 가드만 올린 채 반격하지 않는 문제가 생깁니다.
        if (self.state == Fighter.GUARD) {
            guardHoldTime += dt;
            counterWindow = COUNTER_WINDOW;   // 막는 동안은 반격 기회가 계속 살아 있음
        } else {
            guardHoldTime -= dt * 1.6f;
            if (guardHoldTime < 0f) guardHoldTime = 0f;
        }
        guardTired = guardHoldTime > MAX_GUARD_HOLD;

        distance = Math.abs(target.x - self.x);

        if (!self.isAlive() || !target.isAlive()) {
            chosen = ACT_WAIT;
            self.walk(0);
            self.setGuard(false);
            return;
        }

        // 보스는 체력이 35% 아래로 떨어지면 더 공격적으로 바뀝니다.
        enraged = boss && self.hpRatio() < 0.35f;

        // 상대가 "방금" 공격을 시작했다면, 사람처럼 조금 늦게 반응합니다.
        boolean nowAttacking = (target.state == Fighter.ATTACK
                && target.attackPhase() == Fighter.PHASE_WINDUP);
        if (nowAttacking && !targetWasAttacking) {
            // 반응 시간만큼 기다렸다가 다시 판단 (난이도가 높을수록 빨리 반응)
            decisionTimer = Math.min(decisionTimer, difficulty.reactionTime * 0.35f);
        }
        targetWasAttacking = nowAttacking;

        decisionTimer -= dt;
        if (decisionTimer <= 0f) {
            decide(self, target);
            decisionTimer = difficulty.reactionTime;
        }

        perform(self, target);
    }

    // ==================================================================
    //  1) 행동 점수 계산 → 가장 높은 것 선택
    // ==================================================================
    private void decide(Fighter self, Fighter target) {
        float aggression = difficulty.aggression + (enraged ? 0.25f : 0f);
        float defensiveness = difficulty.defensiveness;
        if (aggression > 1f) aggression = 1f;

        float myReach = self.reachOf(lightMove);
        float gap = distance - myReach;          // 0 이하면 사거리 안
        float hpR = self.hpRatio();

        // 상대가 공격 중이고 그 공격이 나에게 닿는가? (막거나 피할 상황)
        sawIncoming = target.state == Fighter.ATTACK
                && target.attackPhase() != Fighter.PHASE_RECOVER
                && target.move != null
                && distance <= target.reachOf(target.move) + 40f;

        // 들어오는 공격이 방어로 막히지 않는 기술인가? → 막지 말고 피해야 함
        sawGuardBreak = sawIncoming && target.move != null && target.move.guardBreak;

        // 상대가 빈틈을 보였는가? (공격 후딜 / 경직 중 = 반격 기회)
        sawOpening = (target.state == Fighter.ATTACK
                && target.attackPhase() == Fighter.PHASE_RECOVER)
                || target.state == Fighter.HURT;

        // 상대가 방어 중이면 일반 공격은 거의 막히므로, 방어를 뚫는 강공격이 답입니다.
        sawTargetGuarding = (target.state == Fighter.GUARD);

        boolean inRange = gap <= 8f;
        boolean tooClose = gap < -70f;
        // 막아 낸 직후이고 상대 공격이 끝났다면 지금이 반격할 때
        sawCounterChance = (counterWindow > 0f) && !sawIncoming;
        boolean counterChance = sawCounterChance;

        // --- 대기: 아무 조건도 맞지 않을 때의 기본값
        scores[ACT_WAIT] = 0.16f + 0.12f * (1f - aggression);

        // --- 접근: 멀수록 높아짐
        //
        // ★ 주의 ★ 목표 지점을 "사거리 끝"이 아니라 "사거리보다 조금 안쪽"으로 잡습니다.
        //   사거리 끝을 목표로 하면, 사거리 바로 바깥(예: 11px 모자람)에서
        //   접근 점수가 거의 0이 되어 "공격도 접근도 안 하고 멈춰 서는" 사각지대가 생깁니다.
        //   또한 거리가 조금만 모자라도 반드시 다가가도록 최소 점수(0.35)를 둡니다.
        float approachNeed = distance - (myReach - APPROACH_MARGIN);
        scores[ACT_APPROACH] = (approachNeed > 0f)
                ? (0.35f + 0.55f * clamp01(approachNeed / 240f)) * (0.6f + 0.6f * aggression)
                : 0f;

        // --- 후퇴: 체력이 낮거나 너무 붙었을 때
        float lowHp = (hpR < 0.35f) ? (0.35f - hpR) * 2.4f : 0f;
        scores[ACT_RETREAT] = lowHp * (0.4f + defensiveness)
                + (tooClose ? 0.22f : 0f)
                - (enraged ? 0.4f : 0f);

        // --- 공격: 사거리 안 + 쿨타임 끝. 상대 빈틈이나 막아 낸 직후면 크게 상승
        scores[ACT_ATTACK] = (inRange && attackCooldown <= 0f)
                ? (0.58f + 0.45f * aggression)
                * (sawOpening ? 1.8f : 1f)
                * (counterChance ? 1.7f : 1f)
                * (sawIncoming ? 0.4f : 1f)
                * (guardTired ? 1.6f : 1f)
                * (sawTargetGuarding ? 0.40f : 1f)   // 막힐 게 뻔하면 점수를 낮춤
                : 0f;

        // --- 강공격: 느리지만 세고 방어를 뚫음. 확실한 빈틈에만.
        scores[ACT_HEAVY] = (inRange && heavyCooldown <= 0f && attackCooldown <= 0f)
                ? (0.40f + 0.40f * aggression)
                * (sawOpening ? 1.9f : 0.65f)
                * (counterChance ? 1.6f : 1f)
                * (sawIncoming ? 0.2f : 1f)
                * (boss ? 1.25f : 1f)
                * (sawTargetGuarding ? 2.6f : 1f)    // 방어를 뚫을 수 있으므로 점수를 크게 올림
                : 0f;

        // --- 방어: 상대 공격이 들어올 때.
        //     단 (1) 막히지 않는 강공격이거나 (2) 너무 오래 막고 있었으면 점수가 떨어집니다.
        scores[ACT_GUARD] = sawIncoming
                ? (0.65f + 0.70f * defensiveness)
                * (hpR < 0.5f ? 1.25f : 1f)
                * (sawGuardBreak ? 0.15f : 1f)
                * (guardTired ? 0.12f : 1f)
                : 0f;

        // --- 회피: 상대 공격이 들어오고 회피 쿨타임이 끝났을 때.
        //     막히지 않는 강공격이면 회피가 유일한 대응이므로 점수가 크게 오릅니다.
        scores[ACT_DODGE] = (sawIncoming && self.dodgeCooldown <= 0f && dodgeRest <= 0f)
                ? (0.40f + 0.75f * defensiveness)
                * (sawGuardBreak ? 2.1f : 1f)
                * (guardTired ? 1.5f : 1f)
                * (hpR < 0.4f ? 1.4f : 1f)
                : 0f;

        // 같은 패턴만 반복하지 않도록 아주 약간의 무작위성을 더합니다.
        for (int i = 0; i < ACT_COUNT; i++) {
            if (scores[i] > 0f) scores[i] += rnd.nextFloat() * 0.09f;
            if (scores[i] < 0f) scores[i] = 0f;
        }

        int best = ACT_WAIT;
        for (int i = 1; i < ACT_COUNT; i++) {
            if (scores[i] > scores[best]) best = i;
        }
        chosen = best;
        reason = buildReason(hpR);
    }

    private String buildReason(float hpR) {
        int d = (int) distance;
        int hpPct = (int) (hpR * 100f);
        switch (chosen) {
            case ACT_APPROACH:
                return "거리 " + d + " · 사거리 밖 → 접근";
            case ACT_RETREAT:
                return "체력 " + hpPct + "% · 위험 → 거리 벌리기";
            case ACT_ATTACK:
                if (sawOpening) return "상대 후딜 포착 → 반격";
                if (sawCounterChance) return "막아 낸 직후 → 반격";
                if (guardTired) return "방어만으론 불리 → 공격 전환";
                return "사거리 안 → 공격";
            case ACT_HEAVY:
                if (sawTargetGuarding) return "상대 방어 중 → 가드 브레이크";
                if (sawOpening) return "큰 빈틈 → 강공격(방어 무시)";
                if (sawCounterChance) return "막아 낸 직후 → 강공격";
                return "사거리 안 → 강공격";
            case ACT_GUARD:
                return "상대 공격 감지 → 방어";
            case ACT_DODGE:
                if (sawGuardBreak) return "막을 수 없는 공격 → 회피";
                if (guardTired) return "방어 소모 → 회피로 대응";
                return "상대 공격 감지 → 회피";
            default:
                if (attackCooldown > 0f) {
                    return "공격 재사용 대기 중 · 거리 유지";
                }
                return "거리 " + d + " · 관망";
        }
    }

    // ==================================================================
    //  2) 고른 행동을 실제로 실행
    // ==================================================================
    private void perform(Fighter self, Fighter target) {
        int toward = (target.x >= self.x) ? 1 : -1;

        switch (chosen) {
            case ACT_APPROACH:
                self.setGuard(false);
                self.walk(toward);
                break;

            case ACT_RETREAT:
                self.setGuard(false);
                self.walk(-toward);
                break;

            case ACT_ATTACK:
                self.setGuard(false);
                self.walk(0);
                if (attackCooldown <= 0f && self.tryMove(lightMove)) {
                    attackCooldown = lightMove.total() + difficulty.reactionTime * 0.55f;
                    counterWindow = 0f;
                }
                break;

            case ACT_HEAVY:
                self.setGuard(false);
                self.walk(0);
                if (heavyCooldown <= 0f && attackCooldown <= 0f && self.tryMove(heavyMove)) {
                    heavyCooldown = boss ? 2.4f : 3.6f;
                    attackCooldown = heavyMove.total() + difficulty.reactionTime;
                }
                break;

            case ACT_GUARD:
                self.walk(0);
                self.setGuard(true);
                break;

            case ACT_DODGE:
                self.setGuard(false);
                if (self.tryDodge(-toward)) {
                    dodgeRest = DODGE_REST;
                    // 피한 직후는 상대의 후딜이므로 반격 기회로 간주합니다.
                    counterWindow = COUNTER_WINDOW;
                } else {
                    self.walk(0);
                }
                break;

            default: // ACT_WAIT
                self.setGuard(false);
                self.walk(0);
                break;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
