package com.example.trojanprototype.ai;

import com.example.trojanprototype.battle.Fighter;

/**
 * 플레이어 습관 학습 — 적 AI 가 "이 플레이어는 이렇게 싸운다"를 기억합니다.
 *
 * 담당: 유장원 (AI)
 *
 * ── 무엇을 배우나 ─────────────────────────────────────────
 *   방어 습관  적이 공격을 시작했을 때 플레이어가 방어를 올린 비율
 *   회피 습관  적이 공격을 시작했을 때 플레이어가 회피한 비율
 *   공격 성향  적 가까이 있을 때 1초에 공격을 몇 번 시작하는지 (0~1 로 환산)
 *
 * ── 어떻게 배우나 (지수 이동 평균) ────────────────────────
 *   새 값 = 이전 값 × (1 - α) + 이번 관찰 × α
 *   최근 행동일수록 크게 반영되므로, 플레이어가 전략을 바꾸면 AI 도 따라 바뀝니다.
 *   관찰 횟수가 적을 때는 확신도(confidence)를 낮게 두어 섣불리 판단하지 않습니다.
 *
 * 배운 값은 EnemyBrain 이 행동 점수에 곱해 쓰고 (EnemyBrain.adapt 참고),
 * 전투가 끝날 때마다 AiMemory 가 저장해서 다음 전투 · 다음 실행에도 이어집니다.
 *
 * 안드로이드 클래스를 쓰지 않으므로 tools/balance_sim 에서도 그대로 돌아갑니다.
 */
public class PlayerModel {

    /** 아무것도 모를 때의 출발값 */
    public static final float BASE_GUARD = 0.33f;
    public static final float BASE_DODGE = 0.15f;
    public static final float BASE_AGGRESSION = 0.5f;

    /** 한 번의 관찰을 얼마나 크게 반영할지 */
    private static final float ALPHA_REACT = 0.18f;
    private static final float ALPHA_AGGRESSION = 0.25f;
    /** 이 횟수만큼 관찰하면 확신도 100% */
    private static final int FULL_CONFIDENCE = 6;
    /** 공격 성향을 재는 단위 시간 (적 가까이 있었던 시간, 초) */
    private static final float AGG_WINDOW = 3f;
    /** 이만큼 공격을 시작하면 공격 성향 1.0 (회/초) */
    private static final float AGG_FULL_RATE = 2.5f;
    /** 이 거리 안에 있을 때만 공격 성향을 잽니다 */
    private static final float NEAR = 260f;
    /** 적이 공격을 시작한 뒤 플레이어의 대응을 지켜보는 시간(초) */
    private static final float REACT_WINDOW = 0.65f;

    // ------------------------------------------------ 배운 값 (저장되는 부분)
    public float guardRate = BASE_GUARD;
    public float dodgeRate = BASE_DODGE;
    public float aggression = BASE_AGGRESSION;
    /** 적의 공격에 대한 대응을 관찰한 횟수 */
    public int samples = 0;

    // ------------------------------------------------ 관찰 중인 상태
    private boolean watching = false;
    private float watchTime = 0f;
    private boolean sawGuard, sawDodge;

    private float nearTime = 0f;
    private int nearAttacks = 0;
    private int lastState = -1;
    private float lastStateTime = 0f;

    /** 0~1. 관찰이 적으면 낮습니다. */
    public float confidence() {
        return Math.min(1f, samples / (float) FULL_CONFIDENCE);
    }

    public void reset() {
        guardRate = BASE_GUARD;
        dodgeRate = BASE_DODGE;
        aggression = BASE_AGGRESSION;
        samples = 0;
        resetBattle();
    }

    /** 전투가 새로 시작될 때 — 관찰 중이던 것만 비우고 배운 값은 남깁니다. */
    public void resetBattle() {
        watching = false;
        watchTime = 0f;
        nearTime = 0f;
        nearAttacks = 0;
        lastState = -1;
        lastStateTime = 0f;
    }

    /**
     * 전투 중 매 프레임 호출합니다.
     * @param enemy 적 대장 (1대1 이면 상대 영웅)
     */
    public void observe(float dt, Fighter player, Fighter enemy) {
        if (!player.isAlive() || !enemy.isAlive()) return;
        float dist = Math.abs(enemy.x - player.x);

        // ---- 1) 적이 공격을 시작했을 때 플레이어가 어떻게 대응하는가
        if (!watching) {
            if (enemy.state == Fighter.ATTACK && enemy.move != null
                    && enemy.attackPhase() == Fighter.PHASE_WINDUP
                    && dist <= enemy.reachOf(enemy.move) + 60f) {
                watching = true;
                watchTime = 0f;
                sawGuard = player.state == Fighter.GUARD;
                sawDodge = player.state == Fighter.DODGE;
            }
        } else {
            watchTime += dt;
            if (player.state == Fighter.GUARD) sawGuard = true;
            if (player.state == Fighter.DODGE) sawDodge = true;
            // 사람은 적의 동작을 보고 0.2~0.4초 뒤에 반응하므로, 공격이 맞는 순간까지가 아니라
            // 공격을 시작한 뒤 일정 시간(REACT_WINDOW) 동안의 대응을 봅니다.
            if (watchTime >= REACT_WINDOW) {
                watching = false;
                guardRate = ema(guardRate, sawGuard ? 1f : 0f, ALPHA_REACT);
                dodgeRate = ema(dodgeRate, sawDodge ? 1f : 0f, ALPHA_REACT);
                if (samples < 999) samples++;
            }
        }

        // ---- 2) 적 가까이에서 공격을 얼마나 자주 시작하는가
        boolean attackStarted = player.state == Fighter.ATTACK
                && (lastState != Fighter.ATTACK || player.stateTime < lastStateTime);
        lastState = player.state;
        lastStateTime = player.stateTime;
        if (dist <= NEAR) {
            nearTime += dt;
            if (attackStarted) nearAttacks++;
            if (nearTime >= AGG_WINDOW) {
                float rate = nearAttacks / nearTime;
                aggression = ema(aggression, Math.min(1f, rate / AGG_FULL_RATE), ALPHA_AGGRESSION);
                nearTime = 0f;
                nearAttacks = 0;
            }
        }
    }

    private static float ema(float old, float now, float alpha) {
        return old * (1f - alpha) + now * alpha;
    }

    // ------------------------------------------------ 저장용 (AiMemory 가 Gson 으로 JSON 변환)
    public static final class State {
        public float guardRate = BASE_GUARD;
        public float dodgeRate = BASE_DODGE;
        public float aggression = BASE_AGGRESSION;
        public int samples = 0;
    }

    public State toState() {
        State s = new State();
        s.guardRate = guardRate;
        s.dodgeRate = dodgeRate;
        s.aggression = aggression;
        s.samples = samples;
        return s;
    }

    public void apply(State s) {
        if (s == null) return;
        guardRate = clamp01(s.guardRate);
        dodgeRate = clamp01(s.dodgeRate);
        aggression = clamp01(s.aggression);
        samples = Math.max(0, s.samples);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
