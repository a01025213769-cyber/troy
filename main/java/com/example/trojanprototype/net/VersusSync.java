package com.example.trojanprototype.net;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.Fighter;
import com.example.trojanprototype.data.GameData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 방장의 전투 장면을 참가자 기기로 옮기는 변환기.
 *
 * 담당: 유장원 (시스템)
 *
 * ── 방식 ────────────────────────────────────────────────
 *  계산(공격 판정 · 피해 · AI)은 방장 기기 한 곳에서만 합니다.
 *  참가자 기기는 계산하지 않고, 방장이 보낸 "지금 장면"을 받아 그리기만 합니다.
 *  두 기기가 따로 계산하면 조금만 어긋나도 서로 다른 전투가 되기 때문입니다.
 *
 *  장면(SNAP) 한 장에 들어 있는 것
 *   · 전투 단계 · 남은 시간 · 승패 · 화면 흔들림
 *   · 모든 캐릭터(대장 · 병사)의 위치 · 상태 · 체력 · 분노 · 쓰고 있는 기술
 *   · 날아가는 화살
 *   · 그 사이에 생긴 이펙트와 효과음 (타격 불꽃, 글자, 소리)
 *
 *  참가자 기기는 장면과 장면 사이(약 33ms)를 BattleWorld.updateMirror 로 이어서
 *  60프레임으로 부드럽게 보여 줍니다.
 *
 * 두 기기가 같은 순서로 캐릭터를 편성하므로(BattleWorld.startVersus) 캐릭터는 번호로 가리킵니다.
 */
public final class VersusSync {

    private VersusSync() {}

    private static final int MV_NONE = 0, MV_LIGHT = 1, MV_HEAVY = 2, MV_ULT = 3,
            MV_MELEE_L = 4, MV_MELEE_H = 5;

    // ==================================================================
    //  방장 → 장면 만들기
    // ==================================================================
    public static byte[] encode(BattleWorld w) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(512);
        DataOutputStream o = new DataOutputStream(bo);
        try {
            o.writeByte(Proto.T_SNAP);
            o.writeByte(w.phase);
            o.writeByte(w.result);
            o.writeByte(w.finished ? 1 : 0);
            o.writeFloat(w.timeLeft);
            o.writeFloat(w.phaseTime);
            o.writeFloat(w.shake);
            o.writeFloat(w.screenFlash);

            o.writeByte(w.unitCount);
            for (int i = 0; i < w.unitCount; i++) {
                Fighter f = w.units[i];
                o.writeFloat(f.x);
                o.writeFloat(f.vx);
                o.writeByte(f.facing > 0 ? 1 : 0);
                o.writeByte(f.state);
                o.writeFloat(f.stateTime);
                o.writeShort(f.hp);
                o.writeShort(Math.round(f.rage * 10f));
                o.writeByte(moveCode(f));
                o.writeByte(f.hitsDone);
                o.writeShort(Math.round(Math.max(0f, f.flashTime) * 1000f));
                o.writeShort(Math.round(Math.max(0f, f.invincible) * 1000f));
                o.writeShort(Math.round(Math.max(0f, f.dodgeCooldown) * 1000f));
                o.writeByte((f.usingMelee ? 1 : 0) | (f.usingUltimate ? 2 : 0) | (f.dodgeDir > 0 ? 4 : 0));
            }

            int na = 0;
            for (int i = 0; i < w.arrows.length; i++) if (w.arrows[i].active) na++;
            o.writeByte(na);
            for (int i = 0; i < w.arrows.length; i++) {
                BattleWorld.Arrow a = w.arrows[i];
                if (!a.active) continue;
                o.writeFloat(a.x);
                o.writeFloat(a.y);
                o.writeFloat(a.vx);
                o.writeByte(a.dir > 0 ? 1 : 0);
                o.writeFloat(a.life);
            }

            // 이번 장면 사이에 생긴 이펙트 · 효과음 (보낸 뒤에는 비웁니다)
            int nf = w.fxLog.size();
            o.writeByte(nf);
            for (int i = 0; i < nf; i++) {
                BattleWorld.FxEvent e = w.fxLog.get(i);
                o.writeByte(e.type);
                o.writeFloat(e.x);
                o.writeFloat(e.y);
                o.writeFloat(e.vx);
                o.writeFloat(e.vy);
                o.writeFloat(e.life);
                o.writeFloat(e.size);
                o.writeInt(e.color);
                o.writeUTF(e.text == null ? "" : e.text);
            }
            int ns = w.sfxLog.size();
            o.writeByte(ns);
            for (int i = 0; i < ns; i++) o.writeByte(w.sfxLog.get(i).intValue());
            w.fxLog.clear();
            w.sfxLog.clear();
        } catch (IOException ignored) {
        }
        return bo.toByteArray();
    }

    private static int moveCode(Fighter f) {
        GameData.Move m = f.move;
        if (m == null) return MV_NONE;
        if (m == f.lightMove) return MV_LIGHT;
        if (m == f.heavyMove) return MV_HEAVY;
        if (m == f.ultimateMove) return MV_ULT;
        if (m == f.meleeLightMove) return MV_MELEE_L;
        if (m == f.meleeHeavyMove) return MV_MELEE_H;
        return MV_NONE;
    }

    private static GameData.Move moveOf(Fighter f, int code) {
        switch (code) {
            case MV_LIGHT: return f.lightMove;
            case MV_HEAVY: return f.heavyMove;
            case MV_ULT: return f.ultimateMove;
            case MV_MELEE_L: return f.meleeLightMove;
            case MV_MELEE_H: return f.meleeHeavyMove;
            default: return null;
        }
    }

    // ==================================================================
    //  참가자 ← 장면 적용하기
    // ==================================================================
    /** 받은 장면을 world 에 덮어씁니다. 모양이 이상하면 false (그 장면은 버립니다). */
    public static boolean apply(BattleWorld w, byte[] f) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(f, 1, f.length - 1));
            int phase = in.readUnsignedByte();
            int result = in.readUnsignedByte();
            boolean finished = in.readUnsignedByte() != 0;
            float timeLeft = in.readFloat();
            float phaseTime = in.readFloat();
            float shake = in.readFloat();
            float flash = in.readFloat();

            int n = in.readUnsignedByte();
            if (n != w.unitCount) return false;          // 편성이 다르면 적용하지 않습니다

            w.phase = phase;
            w.result = result;
            w.finished = finished;
            w.timeLeft = timeLeft;
            // 단계가 바뀐 순간에만 단계 시간을 맞춥니다 (그 외에는 이 기기가 부드럽게 흘려보냄)
            if (Math.abs(w.phaseTime - phaseTime) > 0.2f) w.phaseTime = phaseTime;
            w.shake = Math.max(w.shake, shake);
            w.screenFlash = Math.max(w.screenFlash, flash);

            for (int i = 0; i < n; i++) {
                Fighter u = w.units[i];
                u.x = in.readFloat();
                u.vx = in.readFloat();
                u.facing = in.readUnsignedByte() != 0 ? 1 : -1;
                int st = in.readUnsignedByte();
                if (st == Fighter.DOWN && u.state != Fighter.DOWN) u.downTime = 0f;
                u.state = st;
                u.stateTime = in.readFloat();
                u.hp = in.readShort();
                u.rage = in.readShort() / 10f;
                u.move = moveOf(u, in.readUnsignedByte());
                u.hitsDone = in.readUnsignedByte();
                u.flashTime = in.readShort() / 1000f;
                u.invincible = in.readShort() / 1000f;
                u.dodgeCooldown = in.readShort() / 1000f;
                int bits = in.readUnsignedByte();
                u.usingMelee = (bits & 1) != 0;
                u.usingUltimate = (bits & 2) != 0;
                u.dodgeDir = (bits & 4) != 0 ? 1 : -1;
            }

            for (int i = 0; i < w.arrows.length; i++) w.arrows[i].active = false;
            int na = in.readUnsignedByte();
            for (int i = 0; i < na; i++) {
                float ax = in.readFloat();
                float ay = in.readFloat();
                float avx = in.readFloat();
                int dir = in.readUnsignedByte() != 0 ? 1 : -1;
                float life = in.readFloat();
                if (i < w.arrows.length) {
                    BattleWorld.Arrow a = w.arrows[i];
                    a.active = true;
                    a.x = ax;
                    a.y = ay;
                    a.vx = avx;
                    a.dir = dir;
                    a.life = life;
                }
            }

            int nf = in.readUnsignedByte();
            for (int i = 0; i < nf; i++) {
                int type = in.readUnsignedByte();
                float x = in.readFloat(), y = in.readFloat(), vx = in.readFloat(), vy = in.readFloat();
                float life = in.readFloat(), size = in.readFloat();
                int color = in.readInt();
                String text = in.readUTF();
                w.spawnEffect(type, x, y, vx, vy, life, size, color, text.length() == 0 ? null : text);
            }
            int ns = in.readUnsignedByte();
            for (int i = 0; i < ns; i++) {
                int idx = in.readUnsignedByte();
                if (idx < com.example.trojanprototype.audio.SoundManager.SFX_NAMES.length) {
                    w.playSfx(com.example.trojanprototype.audio.SoundManager.SFX_NAMES[idx]);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
