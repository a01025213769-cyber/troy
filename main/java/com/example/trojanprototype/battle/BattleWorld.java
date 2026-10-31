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
 *
 * ── 군대 전투 (다대다) ────────────────────────────────────
 *  units[] 에 이 전투에 나온 모두가 들어 있습니다.
 *    그리스군: 플레이어(대장) + 돌격군 3 + 궁수 2
 *    트로이군: 대장(보스 개체) + 돌격군 3 + 궁수 2
 *  병사는 각자 EnemyBrain(AI)으로 가장 가까운 적을 골라 싸웁니다.
 *  player = 그리스군 대장, enemy = 트로이군 대장 이므로,
 *  "대장이 쓰러지면 패배" 판정은 1대1 때와 똑같이 player/enemy 만 보면 됩니다.
 *  1대1 스테이지는 units = {player, enemy} 로 같은 코드가 그대로 돌아갑니다.
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

    private static final int MAX_EFFECTS = 96;
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

    private static final int MAX_ARROWS = 24;
    public final Arrow[] arrows = new Arrow[MAX_ARROWS];

    // ------------------------------------------------ 상태
    public final Fighter player = new Fighter();
    public final Fighter enemy = new Fighter();
    /** 적 대장의 AI (AI 디버그 패널이 읽습니다) */
    public final EnemyBrain brain = new EnemyBrain();

    // ------------------------------------------------ 군대
    public static final int MAX_UNITS = 12;
    /** 이 전투에 나온 모든 병사(플레이어·대장 포함) */
    public final Fighter[] units = new Fighter[MAX_UNITS];
    public int unitCount = 0;
    /** player/enemy 를 뺀 일반 병사 10명분 미리 만들어 둔 자리 */
    private final Fighter[] soldiers = new Fighter[MAX_UNITS - 2];
    public boolean armyMode = false;

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
        for (int i = 0; i < soldiers.length; i++) {
            soldiers[i] = new Fighter();
            soldiers[i].ai = new EnemyBrain();
        }
        enemy.ai = brain;
    }

    // ==================================================================
    //  군대 현황 (HUD / 결과 화면용)
    // ==================================================================
    /** 해당 진영의 살아 있는 병사 수 (대장 제외) */
    public int aliveSoldiers(int team, int role) {
        int n = 0;
        for (int i = 0; i < unitCount; i++) {
            Fighter u = units[i];
            if (u.team == team && !u.commander && u.role == role && u.isAlive()) n++;
        }
        return n;
    }

    public int totalSoldiers(int team) {
        int n = 0;
        for (int i = 0; i < unitCount; i++) {
            if (units[i].team == team && !units[i].commander) n++;
        }
        return n;
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

        // 군대 전투에서는 대장이 여러 명에게 둘러싸이므로 체력을 늘려 줍니다.
        armyMode = st.army;
        player.lane = 0f;
        if (armyMode) {
            player.maxHp = Math.round(ch.maxHp * GameData.ARMY_PLAYER_HP_SCALE);
            player.resetForBattle(GameData.GREEK_HERO_X, 1);
            player.lane = GameData.GREEK_HERO_LANE;
        }
        player.team = 0;
        player.role = Fighter.ROLE_HERO;
        player.commander = true;
        player.ai = null;

        // --- 적(대장) 설정
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
        enemy.team = 1;
        enemy.commander = armyMode;
        enemy.role = armyMode ? Fighter.ROLE_COMMANDER : Fighter.ROLE_HERO;
        enemy.lane = 0f;
        enemy.resetForBattle(armyMode ? GameData.TROY_CMDR_X : 700f, -1);
        if (armyMode) enemy.lane = GameData.TROY_CMDR_LANE;

        brain.configure(diff, st.boss, st.enemyLight, st.enemyHeavy);

        // --- 편성
        unitCount = 0;
        units[unitCount++] = player;
        units[unitCount++] = enemy;
        if (armyMode) buildArmies(assets, diff);

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

    /** 돌격군 3 + 궁수 2 를 양쪽에 편성합니다. */
    private void buildArmies(Assets assets, GameData.Difficulty diff) {
        // 아군 병사는 난이도와 상관없이 '보통' 으로 움직입니다.
        GameData.Difficulty allyDiff = GameData.DIFFICULTIES[1];
        int si = 0;
        for (int team = 0; team < 2; team++) {
            for (int k = 0; k < GameData.ARMY_ASSAULT_COUNT + GameData.ARMY_ARCHER_COUNT; k++) {
                boolean archer = k >= GameData.ARMY_ASSAULT_COUNT;
                GameData.UnitDef def = (team == 0)
                        ? (archer ? GameData.GREEK_ARCHER : GameData.GREEK_ASSAULT)
                        : (archer ? GameData.TROY_ARCHER : GameData.TROY_ASSAULT);
                Fighter f = soldiers[si++];

                f.name = def.name;
                f.title = "";
                f.sprites = assets.spriteSet(def.spriteName);
                f.spriteHeight = def.spriteHeight;
                f.accent = (team == 0) ? Theme.GREEK : Theme.TROY;
                f.maxHp = def.maxHp;
                f.attackPower = def.attack * GameData.ARMY_SOLDIER_DAMAGE
                        * (team == 1 ? diff.damageScale : 1f);
                f.moveSpeed = def.moveSpeed;
                f.dodgeSpeed = def.moveSpeed * 1.9f;
                f.damageTaken = 1f;
                f.lightMove = def.light;
                f.heavyMove = def.heavy;
                f.ultimateMove = null;
                f.meleeLightMove = null;
                f.meleeHeavyMove = null;
                f.meleeRange = 0f;
                f.usingMelee = false;
                f.team = team;
                f.role = archer ? Fighter.ROLE_ARCHER : Fighter.ROLE_ASSAULT;
                f.commander = false;

                float x = (team == 0) ? GameData.GREEK_UNIT_X[k] : GameData.TROY_UNIT_X[k];
                float lane = (team == 0) ? GameData.GREEK_UNIT_LANE[k] : GameData.TROY_UNIT_LANE[k];
                f.resetForBattle(x, team == 0 ? 1 : -1);
                f.lane = lane;

                f.ai.configure(team == 0 ? allyDiff : diff, false, def.light, def.heavy);
                // 모두가 같은 순간에 판단하지 않도록 첫 판단 시점을 흩뜨립니다.
                f.ai.decisionTimer = 0.45f + rnd.nextFloat() * 0.6f;

                units[unitCount++] = f;
            }
        }
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
                updateUnits(dt);
                if (phaseTime >= 1.9f) {
                    phase = PHASE_FIGHT;
                    phaseTime = 0f;
                }
                break;

            case PHASE_FIGHT: {
                // 플레이어가 노리는(바라보는) 상대 = 가장 가까운 적
                Fighter target = nearestFoe(player);
                if (target == null) target = enemy;

                // 활/검 전환 상태를 매 프레임 갱신합니다.
                // 공격 버튼에 지금 어떤 무기가 나갈지 표시하는 데 씁니다.
                player.usingMelee = (player.meleeLightMove != null)
                        && Math.abs(target.x - player.x) <= player.meleeRange;

                handlePlayerInput(input, target);

                // 병사마다 가장 가까운 적을 골라 AI 를 돌립니다.
                for (int i = 0; i < unitCount; i++) {
                    Fighter u = units[i];
                    if (u == player || u.ai == null) continue;
                    Fighter t = nearestFoe(u);
                    if (t != null) u.ai.update(dt, u, t);
                }

                player.faceTowards(target.x);
                for (int i = 0; i < unitCount; i++) {
                    Fighter u = units[i];
                    if (u == player) continue;
                    Fighter t = nearestFoe(u);
                    if (t != null) u.faceTowards(t.x);
                }

                updateUnits(dt);
                separate();

                for (int i = 0; i < unitCount; i++) tryHit(units[i]);
                updateArrows(dt);

                timeLeft -= dt;
                checkEnd();
                break;
            }

            case PHASE_END:
                updateUnits(dt);
                if (phaseTime >= 1.7f) finished = true;
                break;

            default:
                break;
        }
    }

    private void updateUnits(float dt) {
        for (int i = 0; i < unitCount; i++) units[i].update(dt);
    }

    /** me 와 다른 진영에서 가장 가까이 있는 살아 있는 상대 */
    private Fighter nearestFoe(Fighter me) {
        Fighter best = null;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < unitCount; i++) {
            Fighter u = units[i];
            if (u.team == me.team || !u.isAlive()) continue;
            float dd = Math.abs(u.x - me.x);
            if (dd < bestD) {
                bestD = dd;
                best = u;
            }
        }
        return best;
    }

    // ==================================================================
    //  플레이어 조작
    // ==================================================================
    private void handlePlayerInput(TouchInput input, Fighter target) {
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
        float distance = Math.abs(target.x - player.x);
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
        for (int i = 0; i < unitCount; i++) {
            Fighter a = units[i];
            if (!a.isAlive()) continue;
            for (int j = i + 1; j < unitCount; j++) {
                Fighter b = units[j];
                if (!b.isAlive()) continue;
                // 앞뒤 줄이 다르면 서로 스쳐 지나갈 수 있습니다.
                if (Math.abs(a.lane - b.lane) >= 12f) continue;

                float d = b.x - a.x;
                float ad = Math.abs(d);
                if (ad >= minDist) continue;

                int sign = (d > 0f) ? 1 : (d < 0f ? -1 : (i < j ? 1 : -1));
                float push = (minDist - ad) * 0.5f;
                a.x = clamp(a.x - push * sign, Theme.ARENA_LEFT, Theme.ARENA_RIGHT);
                b.x = clamp(b.x + push * sign, Theme.ARENA_LEFT, Theme.ARENA_RIGHT);
            }
        }
    }

    // ==================================================================
    //  공격 판정
    // ==================================================================
    private void tryHit(Fighter attacker) {
        if (attacker.state != Fighter.ATTACK || attacker.move == null) return;
        if (attacker.attackPhase() != Fighter.PHASE_ACTIVE) return;
        if (!attacker.isAlive()) return;

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

        // 근접 공격: 판정 상자에 처음 닿는 적 한 명을 맞힙니다.
        RectF ab = attacker.attackBox();
        for (int i = 0; i < unitCount; i++) {
            Fighter defender = units[i];
            if (defender.team == attacker.team || !defender.isAlive()) continue;
            if (!Fighter.overlap(ab, defender.bodyBox())) continue;

            attacker.hitsDone++;
            int res = defender.takeHit(attacker, m);
            resolveHit(attacker, defender, m, res);
            return;
        }
    }

    private void resolveHit(Fighter attacker, Fighter defender, GameData.Move m, int res) {
        float hitX = (attacker.x + defender.x) * 0.5f;
        float hitY = Theme.GROUND + defender.lane - m.boxTop + m.boxHeight * 0.5f;
        boolean byPlayer = (attacker == player);
        // 병사끼리 싸울 때마다 화면이 멈추거나 흔들리면 정신이 없으므로,
        // 타격 연출은 플레이어나 대장이 얽힌 타격에만 줍니다.
        boolean major = byPlayer || defender == player || attacker.commander || defender.commander;
        float hs = major ? 1f : 0f;
        float shk = major ? 1f : 0f;

        switch (res) {
            case Fighter.RESULT_DODGED:
                if (major) {
                    spawnText(defender.x, Theme.GROUND + defender.lane - 150f, "회피!", 0xFF9FD8F0, 0.6f);
                    playSfx("sfx_dodge");
                }
                break;

            case Fighter.RESULT_GUARDED:
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain * 0.4f);
                spawnEffect(FX_GUARD, hitX, hitY, 0f, 0f, 0.28f, 34f, 0xFF8FC6E8, null);
                if (major) spawnText(defender.x, Theme.GROUND + defender.lane - 160f, "방어", 0xFFBFD8E8, 0.45f);
                hitStop = Math.max(hitStop, GameData.HIT_STOP * 0.6f * hs);
                shake = Math.max(shake, 0.35f * shk);
                playSfx("sfx_guard");
                break;

            case Fighter.RESULT_GUARD_BREAK:
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain);
                if (major) spawnText(defender.x, Theme.GROUND + defender.lane - 170f, "가드 브레이크!", 0xFFFFC46B, 0.8f);
                burst(hitX, hitY, 0xFFFFD27A, 10);
                hitStop = Math.max(hitStop, GameData.HIT_STOP * 1.6f * hs);
                shake = Math.max(shake, 1.0f * shk);
                playSfx("sfx_heavy");
                if (byPlayer) addCombo();
                break;

            default: // RESULT_HIT
                attacker.rage = Math.min(GameData.RAGE_MAX, attacker.rage + m.rageGain);
                burst(hitX, hitY, attacker.team == 0 ? 0xFFFFD98A : 0xFFFF9C86, major ? 8 : 4);
                hitStop = Math.max(hitStop, GameData.HIT_STOP * hs);
                shake = Math.max(shake, (m.knockback > 250f ? 0.9f : 0.5f) * shk);
                playSfx(m.guardBreak ? "sfx_heavy" : "sfx_hit");
                if (byPlayer) addCombo();
                else if (defender == player) combo = 0;
                // 연속 피격 상한에 걸려 밀려났을 때
                if (defender.brokeOut) {
                    defender.brokeOut = false;
                    if (major) {
                        spawnText(defender.x, Theme.GROUND + defender.lane - 180f, "밀려남!", 0xFFBFD8E8, 0.7f);
                        shake = Math.max(shake, 1.0f);
                    }
                    spawnDust(defender.x, Theme.GROUND + defender.lane, attacker.facing);
                    if (byPlayer) combo = 0;
                }
                // 병사가 쓰러졌을 때
                if (!defender.isAlive() && !defender.commander) {
                    burst(defender.x, Theme.GROUND + defender.lane - 60f, 0xFFE8E0D0, 12);
                    spawnText(defender.x, Theme.GROUND + defender.lane - 150f, "쓰러짐", 0xFFE0C8A0, 0.7f);
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
        a.y = Theme.GROUND + owner.lane - m.boxTop + m.boxHeight * 0.5f;
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

            // 화살은 아군 머리 위를 지나가고, 길을 막는 가장 앞의 적 한 명만 맞힙니다.
            Fighter hit = null;
            float half = 16f;
            for (int k = 0; k < unitCount; k++) {
                Fighter t = units[k];
                if (t.team == a.owner.team || !t.isAlive()) continue;
                RectF body = t.bodyBox();
                if (a.x + half >= body.left && a.x - half <= body.right
                        && a.y >= body.top && a.y <= body.bottom) {
                    if (hit == null || Math.abs(t.x - a.owner.x) < Math.abs(hit.x - a.owner.x)) {
                        hit = t;
                    }
                }
            }
            if (hit != null) {
                a.active = false;
                int res = hit.takeHit(a.owner, a.move);
                resolveHit(a.owner, hit, a.move, res);
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
            celebrateTeam(0);
            spawnText(enemy.x, Theme.GROUND - 170f, "격파!", Theme.TEXT_GOLD, 1.2f);
            playSfx("sfx_win");
            return;
        }
        if (!player.isAlive()) {
            result = RESULT_LOSE;
            endPhase();
            celebrateTeam(1);
            playSfx("sfx_lose");
            return;
        }
        if (timeLeft <= 0f) {
            timeLeft = 0f;
            result = (player.hpRatio() > enemy.hpRatio()) ? RESULT_WIN : RESULT_LOSE;
            endPhase();
            celebrateTeam(result == RESULT_WIN ? 0 : 1);
        }
    }

    /** 이긴 군대의 병사들이 환호합니다. */
    private void celebrateTeam(int team) {
        for (int i = 0; i < unitCount; i++) {
            if (units[i].team == team) units[i].celebrate();
        }
    }

    private void endPhase() {
        // 걷던 병사가 승패가 난 뒤에도 계속 걸어가지 않게 멈춥니다.
        for (int i = 0; i < unitCount; i++) {
            units[i].setGuard(false);
            units[i].walk(0);
        }
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
