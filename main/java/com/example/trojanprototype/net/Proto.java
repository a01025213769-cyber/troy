package com.example.trojanprototype.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 두 기기가 주고받는 메시지의 종류와 모양.
 *
 * 담당: 유장원 (시스템)
 *
 *   참가자 ─ HELLO ──────────► 방장      들어왔어요 (버전 · 이름)
 *   방장   ─ ROOM  ──────────► 참가자    방 상태 (모드 · 두 사람의 캐릭터 · 준비)
 *   참가자 ─ GUEST ──────────► 방장      내 캐릭터 · 준비
 *   방장   ─ START ──────────► 참가자    전투 시작! (모드 · 캐릭터 2명)
 *   참가자 ─ INPUT ──────────► 방장      내 조작 (전투 중, 매 프레임)
 *   방장   ─ SNAP  ──────────► 참가자    전투 장면 (초당 30번, VersusSync 참고)
 *   방장   ─ BACK  ──────────► 참가자    결과 화면이 끝났으니 방으로 돌아가요
 *   양쪽   ─ BYE   ──────────►           나갑니다
 *
 * 방 상태와 모드를 정하는 권한은 방장에게만 있습니다.
 * 참가자가 보낼 수 있는 것은 GUEST (자기 캐릭터와 준비) 와 INPUT (자기 조작) 뿐이고,
 * 방장은 그 외의 것을 받아도 무시합니다.
 */
public final class Proto {

    private Proto() {}

    public static final int VERSION = 1;

    public static final byte T_HELLO = 1;
    public static final byte T_ROOM = 2;
    public static final byte T_GUEST = 3;
    public static final byte T_START = 4;
    public static final byte T_INPUT = 5;
    public static final byte T_SNAP = 6;
    public static final byte T_BACK = 7;
    public static final byte T_BYE = 8;
    public static final byte T_REJECT = 10;

    public static final int MODE_DUEL = 0;
    public static final int MODE_ARMY = 1;

    /** 방 상태 — 방장이 정해서 참가자에게 알려 주는 값 */
    public static final class Room {
        public int mode = MODE_DUEL;
        public int hostChar = 0;
        public int guestChar = 1;
        public boolean guestReady = false;
        public String hostName = "";
        public String guestName = "";
    }

    public static byte[] hello(String name) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        try {
            o.writeByte(T_HELLO);
            o.writeByte(VERSION);
            o.writeUTF(LanDiscovery.clean(name, 12));
        } catch (IOException ignored) {
        }
        return bo.toByteArray();
    }

    public static byte[] room(Room r) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bo);
        try {
            o.writeByte(T_ROOM);
            o.writeByte(r.mode);
            o.writeByte(r.hostChar);
            o.writeByte(r.guestChar);
            o.writeByte(r.guestReady ? 1 : 0);
            o.writeUTF(LanDiscovery.clean(r.hostName, 12));
            o.writeUTF(LanDiscovery.clean(r.guestName, 12));
        } catch (IOException ignored) {
        }
        return bo.toByteArray();
    }

    public static byte[] guest(int ch, boolean ready) {
        return new byte[]{T_GUEST, (byte) ch, (byte) (ready ? 1 : 0)};
    }

    public static byte[] start(int mode, int hostChar, int guestChar) {
        return new byte[]{T_START, (byte) mode, (byte) hostChar, (byte) guestChar};
    }

    public static byte[] input(int heldBits, int atk, int dodge, int ult) {
        return new byte[]{T_INPUT, (byte) heldBits, (byte) atk, (byte) dodge, (byte) ult};
    }

    public static byte[] simple(byte type) {
        return new byte[]{type};
    }

    /** ROOM 프레임을 읽습니다. 모양이 이상하면 null. */
    public static Room parseRoom(byte[] f) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(f, 1, f.length - 1));
            Room r = new Room();
            r.mode = in.readUnsignedByte() == MODE_ARMY ? MODE_ARMY : MODE_DUEL;
            r.hostChar = in.readUnsignedByte();
            r.guestChar = in.readUnsignedByte();
            r.guestReady = in.readUnsignedByte() != 0;
            r.hostName = LanDiscovery.clean(in.readUTF(), 12);
            r.guestName = LanDiscovery.clean(in.readUTF(), 12);
            return r;
        } catch (IOException e) {
            return null;
        }
    }

    /** HELLO 프레임에서 이름을 읽습니다. 버전이 다르면 null. */
    public static String parseHello(byte[] f) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(f, 1, f.length - 1));
            if (in.readUnsignedByte() != VERSION) return null;
            return LanDiscovery.clean(in.readUTF(), 12);
        } catch (IOException e) {
            return null;
        }
    }
}
