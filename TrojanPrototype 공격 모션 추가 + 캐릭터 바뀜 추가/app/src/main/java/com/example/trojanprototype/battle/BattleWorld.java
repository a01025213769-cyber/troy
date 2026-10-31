package com.example.trojanprototype.battle;

import android.graphics.RectF;

import com.example.trojanprototype.ai.EnemyBrain;
import com.example.trojanprototype.audio.SoundManager;
import com.example.trojanprototype.core.TouchInput;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.ui.Theme;
import com.example.trojanprototype.util.Assets;

import java.util.Random;

/**
 * 전투 한 판의 진행을 담당합니다.
 *
 * 담당: 유장원 (시스템)
 *
 * 하는 일
 *  - 플레이어 입력을 캐릭터 행동으로 연결
 *  - 적 AI(EnemyBrain) 호출
 *  - 공격 판정 상자와 몸통 판정 상자의 충돌 검사 → 피해 적용
 *  - 두 캐릭터가 서로 겹치지 않도록 밀어내기
 *  - 제한 시간 / 승패 판정
 *  - 타격감 연출 (히트스톱, 화면 흔들림, 이펙트)
 */
public class BattleWorld {

    public static final int PHASE_INTRO = 0;   // "준비 → 시작!"
    public static final int PHASE_FIGHT = 1;   // 실제 전투
    public static final int PHASE_END = 2;     // 승패 확정 후 잠깐

    public static final int RESULT_NONE = 0;
    public static final int RESULT_WIN = 1;
    public static final int RESULT_LOSE = 2;

    // ------------------------------------------------ 이펙트
    public static final int FX_SPARK = 0;
    public static final int FX_GUARD = 1;
    public static final int FX_TEXT = 2;
    public static final int FX_DUST = 3;

    public static class Effect {
        public boolean active;
        public int type;
        public float x, y, vx, vy;
        public float life, maxLife;
        public float size;
        public int color;
        public String text;
    }

    private static final int MAX_EFFECTS = 56;
    public final Effect[] effects = new Effect[MAX_EFFECTS];

    // ------------------------------------------------ 화살 (오디세우스의 활)
    /**
     * 날아가는 화살 하나.
     * 오디세우스의 공격 프레임이 활시위를 당기는 모습이라,
     * 근접 판정 대신 화살을 쏘아 맞히도록 만들었습니다.
     */
    public static class Arrow {
        public boolean active;
        public float x, y, vx;
        public int dir;
        public float life;
        public Fighter owner;
        public GameData.Move move;
    }

    private static final int MAX_ARROWS = 10;
    public final Arrow[] arrows = new Arrow[MAX_ARROWS];

    // ------------------------------------------------ 상태
    public final Fighter player = new Fighter();
    public final Fighter enemy = new Fighter();
    public final EnemyBrain brain = new EnemyBrain();

    public GameData.StageDef stage;
    public GameData.CharacterDef character;
    public GameData.Difficulty difficulty;

    public int phase = PHASE_INTRO;
    public float phaseTime = 0f;
    public int result = RESULT_NONE;
    public boolean finished = false;       // GameEngine 이 결과 화면으로 넘길 신호

    public float timeLeft = 90f;
    public int combo = 0;
    public int bestCombo = 0;
    public float comboTimer = 0f;

    public float hitStop = 0f;             // 타격 순간 화면이 잠깐 멈추는 시간
    public float shake = 0f;               // 화면 흔들림 세기
    public float screenFlash = 0f;         // 화면 전체 번쩍임
    public String tip = "";
    public float elapsed = 0f;

    private SoundManager sound;
    private final Random rnd = new Random();

    public BattleWorld() {
        for (int i = 0; i < MAX_EFFECTS; i++) effects[i] = new Effect();
        for (int i = 0; i < MAX_ARROWS; i++) arrows[i] = new Arrow();
    }

    // ==================================================================
    //  전투 시작
    // ==================================================================
    public void start(GameData.CharacterDef ch, GameData.StageDef st,
                      GameData.Difficulty diff, Assets assets, SoundManager snd, String tipText) {
        this.character = ch;
        this.stage = st;
        this.difficulty = diff;
        this.sound = snd;
        this.tip = tipText;

        // --- 플레이어 설정
        player.name = ch.name;
        player.title = ch.title;
        player.sprites = assets.spriteSet(ch.spriteName);
        player.spriteHeight = ch.spriteHeight;
        player.accent = ch.accent;
        player.maxHp = ch.maxHp;
        player.attackPower = ch.attack;
        player.moveSpeed = ch.moveSpeed;
        player.dodgeSpeed = ch.dodgeSpeed;
        player.damageTaken = 1f;
        player.lightMove = ch.light;
        player.heavyMove = ch.heavy;
        player.ultimateMove = ch.ultimate;
        player.meleeLightMove = ch.meleeLight;
        player.meleeHeavyMove = ch.meleeHeavy;
        player.meleeRange = ch.meleeRange;
        player.resetForBattle(260f, 1);

        // --- 적 설정
        enemy.name = st.enemyName;
        enemy.title = st.enemyTitle;
        enemy.sprites = assets.spriteSet(st.spriteName);
        enemy.spriteHeight = st.enemySpriteHeight;
        enemy.accent = Theme.TROY;
        enemy.maxHp = st.enemyHp;
        enemy.attackPower = st.enemyAttack * diff.damageScale;
        enemy.moveSpeed = st.enemyMoveSpeed;
        enemy.dodgeSpeed = st.enemyMoveSpeed * 1.9f;
        enemy.damageTaken = 1f;
        enemy.lightMove = st.enemyLight;
        enemy.heavyMove = st.enemyHeavy;
        enemy.ultimateMove = null;
        enemy.resetForBattle(700f, -1);

        brain.configure(diff, st.boss, st.enemyLight, st.enemyHeavy);

        phase = PHASE_INTRO;
        phaseTime = 0f;
        result = RESULT_NONE;
        finished = false;
        timeLeft = st.timeLimit;
        combo = 0;
        bestCombo = 0;
        comboTimer = 0f;
        hitStop = 0f;
        shake = 0f;
        screenFlash = 0f;
        elapsed = 0f;
        for (int i = 0; i < MAX_EFFECTS; i++) effects[i].active = false;
        for (int i = 0; i < MAX_ARROWS; i++) arrows[i].active = false;
    }

    /** 같은 스테이지를 다시 시작 */
    public void restart(Assets assets) {
        start(character, stage, difficulty, assets, sound, tip);
    }

    // ==================================================================
    //  매 프레임 갱신
    // ==================================================================
    public void update(float dt, TouchInput input) {
        elapsed += dt;
        phaseTime += dt;

        if (shake > 0f) shake -= dt * 10f;
        if (shake < 0f) shake = 0f;
        if (screenFlash > 0f) screenFlash -= dt * 4.5f;
        if (comboTimer > 0f) {
            comboTimer -= dt;
            if (comboTimer <= 0f) combo = 0;
        }

        updateEffects(dt);

        // 타격 순간에는 아주 잠깐 캐릭터 움직임을 멈춰 타격감을 줍니다.
        if (hitStop > 0f) {
            hitStop -= dt;
            return;
        }

        switch (phase) {
            case PHASE_INTRO:
                player.update(dt);
                enemy.update(dt);
                if (phaseTime >= 1.9f) {
                    phase = PHASE_FIGHT;
                    phaseTime = 0f;
                }
                break;

            case PHASE_FIGHT:
                // 활/검 전환 상태를 매 프레임 갱신합니다.
                // 공격 버튼에 지금 어떤 무기가 나갈지 표시하는 데 씁니다.
                player.usingMelee = (player.meleeLightMove != null)
                        && Math.abs(enemy.x - player.x) <= player.meleeRange;

                handlePlayerInput(input);
                brain.update(dt, enemy, player);

                player.faceTowards(enemy.x);
                enemy.faceTowards(player.x);

                player.update(dt);
                enemy.update(dt);

                separate();

                tryHit(player, enemy);
                tryHit(enemy, player);
                updateArrows(dt);

                timeLeft -= dt;
                checkEnd();
                break;

            case PHASE_END:
                player.update(dt);
                enemy.update(dt);
                if (phaseTime >= 1.7f) finished = true;
                break;

            default:
                break;
        }
    }

    // ==================================================================
    //  플레이어 조작
    // ==================================================================
    private void handlePlayerInput(TouchInput input) {
        boolean pressAttack = input.consumePress(TouchInput.BTN_ATTACK);
        boolean pressDodge = input.consumePress(TouchInput.BTN_DODGE);
        boolean pressUlt = input.consumePress(TouchInput.BTN_ULT);
        boolean guard = input.isHeld(TouchInput.BTN_GUARD);
        int dir = (input.isHeld(TouchInput.BTN_RIGHT) ? 1 : 0)
                - (input.isHeld(TouchInput.BTN_LEFT) ? 1 : 0);

        if (!player.isAlive()) return;

        if (pressUlt && player.tryUltimate()) {
            screenFlash = 0.9f;
            shake = 1.1f;
            spawnText(player.x, Theme.GROUND - 180f, player.ultimateMove.name, Theme.RAGE_FULL, 1.1f);
            playSfx("sfx_ultimate");
            return;
        }
        if (pressDodge) {
            int dodgeDir = (dir != 0) ? dir : -player.facing;
            if (player.tryDodge(dodgeDir)) {
                spawnDust(player.x, Theme.GROUND, -dodgeDir);
                playSfx("sfx_dodge");
                return;
            }
        }
        // 활과 검을 자동으로 바꿔 쓰려면 지금 거리를 알아야 합니다.
        float distance = Math.abs(enemy.x - player.x);
        if (pressAttack && player.tryAttack(distance)) {
            playSfx(player.usingMelee ? "sfx_hit" : "sfx_swing");
        }

        if (guard) {
            player.walk(0);
            player.setGuard(true);
        } else {
            player.setGuard(false);
            player.walk(dir);
        }
    }

    // ==================================================================
    //  두 캐릭터가 겹치지 않게 밀어내기
    // ==================================================================
    private void separate() {
        float minDist = GameData.BODY_W * 0.86f;
        float d = enemy.x - player.x;
        float ad = Math.abs(d);
        if (ad >= minDist) return;

        int sign = (d >= 0f) ? 1 : -1;
        float push = (minDist - ad) * 0.5f;
        player.x -= push * sign;
        enemy.x += push * sign;

        player.x = clamp(player.x, Theme.ARENA_LEFT, Theme.ARENA_RIGHT);
        enemy.x = clamp(enemy.x, Theme.ARENA_LEFT, Theme.ARENA_RIGHT);
    }

    // ==================================================================
    //  공격 판정
    // ==================================================================
    private void tryHit(Fighter attacker, Fighter defender) {
        if (attacker.state != Fighter.ATTACK || attacker.move == null) return;
        if (attacker.attackPhase() != Fighter.PHASE_ACTIVE) return;
        if (!defender.isAlive()) return;

        GameData.Move m = attacker.move;
        if (attacker.hitsDone >= m.hits) return;

        // 여러 번 때리는 기술은 판정 구간을 hits 등분해 한 번씩만 맞춥니다.
        float into = attacker.stateTime - m.windup;
        float step = m.active / m.hits;
        int allowed = (int) (into / step) + 1;
        if (attacker.hitsDone >= allowed) return;

        // 활 공격이면 판정 상자 대신 화살을 쏩니다.
        if (m.ranged) {
            attacker.hitsDone++;
            fireArrow(attacker, m);
            return;
        }

        RectF ab = attacker.attackBox();
        RectF db = defender.bodyBox();
        if (!Fighter.overlap(ab, db)) return;

        attacker.hitsDone++;
        int res = defender.takeHit(attacker, m);
        resolveHit(attacker, defender, m, res);
    }

    private void resolveHit(Fighter attacker, Fighter defender, GameData.Move m, int res) {
        float hitX = (attacker.x + defender.x) * 0.5f;
        float hitY = Theme.GROUND - m.boxTop + m.boxHeight * 0.5f;
        boolean byPlayer = (attacker == player);

        switch (res) {
            case Fighter.RESULT_DODGED:
                spawnText(defender.x, Theme.GROUND - 150f, "회피!", 0xFF9FD8F0, 0.6f);
                playSfx("sfx_dodge");
                break;

            case Fighter.RESULT_GUARDED:
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain * 0.4f);
                spawnEffect(FX_GUARD, hitX, hitY, 0f, 0f, 0.28f, 34f, 0xFF8FC6E8, null);
                spawnText(defender.x, Theme.GROUND - 160f, "방어", 0xFFBFD8E8, 0.45f);
                hitStop = GameData.HIT_STOP * 0.6f;
                shake = Math.max(shake, 0.35f);
                playSfx("sfx_guard");
                break;

            case Fighter.RESULT_GUARD_BREAK:
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain);
                spawnText(defender.x, Theme.GROUND - 170f, "가드 브레이크!", 0xFFFFC46B, 0.8f);
                burst(hitX, hitY, 0xFFFFD27A, 10);
                hitStop = GameData.HIT_STOP * 1.6f;
                shake = Math.max(shake, 1.0f);
                playSfx("sfx_heavy");
                if (byPlayer) addCombo();
                break;

            default: // RESULT_HIT
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain);
                burst(hitX, hitY, byPlayer ? 0xFFFFD98A : 0xFFFF9C86, 8);
                hitStop = GameData.HIT_STOP;
                shake = Math.max(shake, m.knockback > 250f ? 0.9f : 0.5f);
                playSfx(m.guardBreak ? "sfx_heavy" : "sfx_hit");
                if (byPlayer) addCombo();
                else combo = 0;
                // 연속 피격 상한에 걸려 밀려났을 때
                if (defender.brokeOut) {
                    defender.brokeOut = false;
                    spawnText(defender.x, Theme.GROUND - 180f, "밀려남!", 0xFFBFD8E8, 0.7f);
                    spawnDust(defender.x, Theme.GROUND, attacker.facing);
                    shake = Math.max(shake, 1.0f);
                    if (byPlayer) combo = 0;
                }
                break;
        }
    }

    // ==================================================================
    //  화살 (오디세우스의 활)
    // ==================================================================
    private void fireArrow(Fighter owner, GameData.Move m) {
        Arrow a = null;
        for (int i = 0; i < MAX_ARROWS; i++) {
            if (!arrows[i].active) {
                a = arrows[i];
                break;
            }
        }
        if (a == null) a = arrows[0];

        a.active = true;
        a.dir = owner.facing;
        a.owner = owner;
        a.move = m;
        a.x = owner.x + owner.facing * (GameData.BODY_W * 0.5f + 22f);
        a.y = Theme.GROUND - m.boxTop + m.boxHeight * 0.5f;
        a.vx = owner.facing * m.arrowSpeed;
        a.life = GameData.ARROW_LIFE;
        playSfx("sfx_swing");
    }

    private void updateArrows(float dt) {
        for (int i = 0; i < MAX_ARROWS; i++) {
            Arrow a = arrows[i];
            if (!a.active) continue;

            a.life -= dt;
            a.x += a.vx * dt;

            if (a.life <= 0f || a.x < -40f || a.x > Theme.VW + 40f) {
                a.active = false;
                continue;
            }

            Fighter target = (a.owner == player) ? enemy : player;
            if (!target.isAlive()) {
                a.active = false;
                continue;
            }

            RectF body = target.bodyBox();
            float half = 16f;
            if (a.x + half >= body.left && a.x - half <= body.right
                    && a.y >= body.top && a.y <= body.bottom) {
                a.active = false;
                int res = target.takeHit(a.owner, a.move);
                resolveHit(a.owner, target, a.move, res);
            }
        }
    }

    private void addCombo() {
        combo++;
        comboTimer = GameData.COMBO_WINDOW;
        if (combo > bestCombo) bestCombo = combo;
    }

    // ==================================================================
    //  승패 판정
    // ==================================================================
    private void checkEnd() {
        if (result != RESULT_NONE) return;

        if (!enemy.isAlive()) {
            result = RESULT_WIN;
            endPhase();
            player.celebrate();
            spawnText(enemy.x, Theme.GROUND - 170f, "격파!", Theme.TEXT_GOLD, 1.2f);
            playSfx("sfx_win");
            return;
        }
        if (!player.isAlive()) {
            result = RESULT_LOSE;
            endPhase();
            enemy.celebrate();
            playSfx("sfx_lose");
            return;
        }
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            result = (player.hpRatio() > enemy.hpRatio()) ? RESULT_WIN : RESULT_LOSE;
            endPhase();
            if (result == RESULT_WIN) player.celebrate();
            else enemy.celebrate();
        }
    }

    private void endPhase() {
        phase = PHASE_END;
        phaseTime = 0f;
        shake = Math.max(shake, 0.8f);
        screenFlash = 0.5f;
    }

    // ==================================================================
    //  이펙트
    // ==================================================================
    private Effect freeEffect() {
        for (int i = 0; i < MAX_EFFECTS; i++) {
            if (!effects[i].active) return effects[i];
        }
        return effects[rnd.nextInt(MAX_EFFECTS)];
    }

    private void spawnEffect(int type, float x, float y, float vx, float vy,
                             float life, float size, int color, String text) {
        Effect e = freeEffect();
        e.active = true;
        e.type = type;
        e.x = x;
        e.y = y;
        e.vx = vx;
        e.vy = vy;
        e.life = life;
        e.maxLife = life;
        e.size = size;
        e.color = color;
        e.text = text;
    }

    private void burst(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float) (rnd.nextFloat() * Math.PI * 2.0);
            float spd = 90f + rnd.nextFloat() * 220f;
            spawnEffect(FX_SPARK, x, y,
                    (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd,
                    0.22f + rnd.nextFloat() * 0.22f,
                    3f + rnd.nextFloat() * 4f, color, null);
        }
    }

    private void spawnText(float x, float y, String text, int color, float life) {
        spawnEffect(FX_TEXT, x, y, 0f, -46f, life, 22f, color, text);
    }

    private void spawnDust(float x, float y, int dir) {
        for (int i = 0; i < 6; i++) {
            spawnEffect(FX_DUST, x, y - 4f,
                    dir * (40f + rnd.nextFloat() * 110f), -rnd.nextFloat() * 50f,
                    0.3f + rnd.nextFloat() * 0.2f,
                    4f + rnd.nextFloat() * 5f, 0x99C8B9A0, null);
        }
    }

    private void updateEffects(float dt) {
        for (int i = 0; i < MAX_EFFECTS; i++) {
            Effect e = effects[i];
            if (!e.active) continue;
            e.life -= dt;
            if (e.life <= 0f) {
                e.active = false;
                continue;
            }
            e.x += e.vx * dt;
            e.y += e.vy * dt;
            if (e.type == FX_SPARK || e.type == FX_DUST) {
                e.vy += 420f * dt;      // 중력
                e.vx *= 0.94f;
            }
        }
    }

    private void playSfx(String name) {
        if (sound != null) sound.play(name);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
