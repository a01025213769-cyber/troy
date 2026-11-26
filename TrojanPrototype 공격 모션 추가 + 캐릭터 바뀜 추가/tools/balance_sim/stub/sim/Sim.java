package sim;

import com.example.trojanprototype.ai.PlayerModel;
import com.example.trojanprototype.audio.SoundManager;
import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.core.TouchInput;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.util.Assets;

import java.util.Random;

/**
 * 난이도 밸런스 시뮬레이터.
 *
 * 사람처럼 반응이 늦는 가상 플레이어로 전투를 반복해 승률 · 남은 체력 · 걸린 시간을 잽니다.
 * 가상 플레이어의 실력은 3단계입니다 (반응 시간 / 방어 확률 / 회피 확률 / 연타 속도).
 *   novice  0.40초 · 방어 30% · 회피 15%
 *   average 0.28초 · 방어 60% · 회피 45%
 *   skilled 0.20초 · 방어 85% · 회피 75%
 *   turtle  0.15초 · 방어 100% · 공격은 가끔 (방어만 하는 플레이어)
 *
 * 한계: 실제 사람은 상대 패턴을 배워 전략을 바꾸지만 이 플레이어는 바꾸지 않습니다.
 * (예: 방어만 하다가 가드 브레이크를 계속 맞는 경우) 그래서 절대값보다는
 * "스테이지끼리 / 난이도끼리 비교"하는 용도로 씁니다.
 *
 * 출력이 콘솔 글꼴에 상관없이 보이도록 영어로 찍습니다. (d0 쉬움 / d1 보통 / d2 어려움)
 */
public class Sim {
    static final float DT = 1f / 60f;

    static class Skill {
        final String name;
        final float reaction, guardP, dodgeP, mashRate, ultP;

        Skill(String n, float r, float g, float d, float m, float u) {
            name = n; reaction = r; guardP = g; dodgeP = d; mashRate = m; ultP = u;
        }
    }

    static final Skill[] SKILLS = {
            new Skill("novice", 0.40f, 0.30f, 0.15f, 4f, 0.7f),
            new Skill("average", 0.28f, 0.60f, 0.45f, 6f, 0.95f),
            new Skill("skilled", 0.20f, 0.85f, 0.75f, 7f, 1f),
            // 방어만 굳히는 플레이어 — 적이 습관을 배워 가드 브레이크로 대응하는지 확인용
            new Skill("turtle", 0.15f, 1.0f, 0.0f, 1.5f, 1f),
    };

    final Random rnd = new Random();

    /** returns {win(1/0), hp ratio left, seconds} */
    float[] fight(int stage, int diff, Skill sk, PlayerModel model) {
        BattleWorld w = new BattleWorld();
        w.model = model;
        GameData.StageDef st = GameData.STAGES[stage];
        w.start(st.hero, st, GameData.DIFFICULTIES[diff], new Assets(), new SoundManager(), "");
        TouchInput in = new TouchInput();
        float seenWindup = -1f;
        Fighter seenAttacker = null;
        boolean willGuard = false, willDodge = false;
        float nextTap = 0f;
        float t = 0f;
        while (!w.finished && t < 400f) {
            t += DT;
            in.clear();
            Fighter p = w.player;
            Fighter tg = nearest(w, p);
            if (tg != null && p.isAlive()) {
                float dist = Math.abs(tg.x - p.x);
                Fighter threat = null;
                for (int i = 0; i < w.unitCount; i++) {
                    Fighter u = w.units[i];
                    if (u.team == p.team || !u.isAlive() || u.state != Fighter.ATTACK || u.move == null) continue;
                    if (u.attackPhase() == Fighter.PHASE_RECOVER) continue;
                    float reach = u.move.ranged ? 900f : (GameData.BODY_W * 0.5f + u.move.reach + 30f);
                    if (Math.abs(u.x - p.x) <= reach) {
                        threat = u;
                        if (u.commander || u == w.enemy) break;
                    }
                }
                if (threat != null && threat != seenAttacker) {
                    seenAttacker = threat;
                    seenWindup = t;
                    willGuard = rnd.nextFloat() < sk.guardP;
                    willDodge = rnd.nextFloat() < sk.dodgeP;
                } else if (threat == null) {
                    seenAttacker = null;
                }
                boolean reacting = false;
                if (seenAttacker != null && t - seenWindup >= sk.reaction) {
                    if (seenAttacker.move != null && seenAttacker.move.guardBreak) {
                        if (willDodge) { in.pressed[TouchInput.BTN_DODGE] = true; reacting = true; }
                    } else if (willGuard) {
                        in.held[TouchInput.BTN_GUARD] = true; reacting = true;
                    } else if (willDodge) {
                        in.pressed[TouchInput.BTN_DODGE] = true; reacting = true;
                    }
                }
                if (!reacting) {
                    boolean ranged = p.lightMove != null && p.lightMove.ranged;
                    boolean melee = p.meleeLightMove != null && dist <= p.meleeRange;
                    float myReach = (ranged && !melee) ? 700f
                            : (GameData.BODY_W * 0.5f + (melee ? p.meleeLightMove.reach + 120f : p.lightMove.reach));
                    if (p.ultimateReady() && dist < myReach + 30f && rnd.nextFloat() < sk.ultP * DT * 4f) {
                        in.pressed[TouchInput.BTN_ULT] = true;
                    } else if (dist <= myReach - 6f) {
                        if (t >= nextTap) {
                            in.pressed[TouchInput.BTN_ATTACK] = true;
                            nextTap = t + 1f / sk.mashRate * (0.7f + rnd.nextFloat() * 0.6f);
                        }
                    } else {
                        in.held[tg.x > p.x ? TouchInput.BTN_RIGHT : TouchInput.BTN_LEFT] = true;
                    }
                }
            }
            w.update(DT, in);
        }
        return new float[]{w.result == BattleWorld.RESULT_WIN ? 1f : 0f, w.player.hpRatio(), w.elapsed};
    }

    static Fighter nearest(BattleWorld w, Fighter me) {
        Fighter best = null;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < w.unitCount; i++) {
            Fighter u = w.units[i];
            if (u.team == me.team || !u.isAlive()) continue;
            float d = Math.abs(u.x - me.x);
            if (d < bd) { bd = d; best = u; }
        }
        return best;
    }

    public static void main(String[] a) {
        int n = a.length > 0 ? Integer.parseInt(a[0]) : 80;
        // 두 번째 인자로 nolearn 을 주면 적의 습관 학습을 끄고 잽니다.
        boolean learn = !(a.length > 1 && a[1].equals("nolearn"));
        Sim s = new Sim();
        System.out.println("learning " + (learn ? "ON" : "OFF")
                + "  (learned = guard/dodge/aggression the enemy learned about that player)");
        System.out.println("stage      diff  skill    win%  hpLeft%  time(s)  learned");
        for (int st = 0; st < GameData.STAGES.length; st++) {
            for (int d = 0; d < GameData.DIFFICULTIES.length; d++) {
                for (Skill sk : SKILLS) {
                    float wins = 0, hp = 0, tm = 0;
                    // 한 플레이어가 같은 스테이지를 n 번 하는 것처럼 모델을 이어서 씁니다.
                    PlayerModel model = learn ? new PlayerModel() : null;
                    for (int k = 0; k < n; k++) {
                        float[] r = s.fight(st, d, sk, model);
                        wins += r[0]; hp += r[1]; tm += r[2];
                    }
                    String learned = model == null ? "-" : String.format("%.2f/%.2f/%.2f",
                            model.guardRate, model.dodgeRate, model.aggression);
                    System.out.printf("%-10s %-5s %-8s %4.0f  %6.0f  %6.0f  %s%n", GameData.STAGES[st].chapter,
                            "d" + d, sk.name, 100 * wins / n, 100 * hp / n, tm / n, learned);
                }
            }
        }
    }
}
