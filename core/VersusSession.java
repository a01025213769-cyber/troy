package com.example.trojanprototype.core;

import com.example.trojanprototype.battle.BattleWorld;
import com.example.trojanprototype.battle.PadInput;
import com.example.trojanprototype.data.GameData;
import com.example.trojanprototype.net.LanDiscovery;
import com.example.trojanprototype.net.NetLink;
import com.example.trojanprototype.net.Proto;
import com.example.trojanprototype.net.VersusSync;

import java.util.List;

/**
 * 미니게임 2인 대전의 "방" 하나 — 연결 · 방 상태 · 메시지 처리.
 *
 * 담당: 유장원 (시스템)
 *
 * GameEngine 은 화면과 흐름만 맡고, 네트워크에 관한 판단은 전부 여기서 합니다.
 *
 * ── 권한 ────────────────────────────────────────────────
 *  방장    : 모드(1대1 / 다대다) 선택 · 자기 캐릭터 선택 · 전투 시작 · 결과 후 방으로 복귀
 *  참가자  : 자기 캐릭터 선택 · 준비 · 전투 중 자기 조작 전송
 *  참가자가 보낸 메시지 중 GUEST / INPUT / BYE / HELLO 외에는 방장이 무시합니다.
 *  → 참가자 쪽 화면을 고쳐서 모드를 바꾸려고 해도 방장 기기가 받아 주지 않습니다.
 */
public final class VersusSession {

    // pump() 가 알려 주는 사건
    public static final int EV_NONE = 0;
    public static final int EV_JOINED = 1;      // (방장) 참가자가 들어왔다
    public static final int EV_START = 2;       // (참가자) 방장이 전투를 시작했다
    public static final int EV_BACK = 3;        // (참가자) 방장이 방으로 돌아간다
    public static final int EV_LEFT = 4;        // 상대가 나갔거나 연결이 끊어졌다
    public static final int EV_REJECTED = 5;    // (참가자) 방이 거절했다

    public final boolean host;
    public final String myName;
    public NetLink link;
    private LanDiscovery.Announcer announcer;
    private LanDiscovery.Finder finder;

    /** 방 상태. 방장이 정한 값이며 참가자 기기에는 ROOM 메시지로 복사됩니다. */
    public final Proto.Room room = new Proto.Room();

    /** 참가자: 내가 고른 캐릭터와 준비 여부 (화면은 이 값을 바로 보여 줍니다) */
    public int myChar = 1;
    public boolean myReady = false;

    /** 시작 신호로 받은 값 (참가자) */
    public int startMode, startHostChar, startGuestChar;

    /** 마지막으로 일어난 일의 이유 (화면에 띄울 문구) */
    public String message = "";

    private float roomTimer = 0f;
    private float inputTimer = 0f;
    private int lastBits = -1, lastA = -1, lastD = -1, lastU = -1;
    private float snapTimer = 0f;
    private boolean joined = false;

    public VersusSession(boolean host, String myName) {
        this.host = host;
        this.myName = LanDiscovery.clean(myName.length() == 0 ? "플레이어" : myName, 12);
        if (host) {
            room.hostName = this.myName;
        } else {
            room.guestName = this.myName;
        }
    }

    // ==================================================================
    //  방장: 방 열기
    // ==================================================================
    public void openRoom() {
        link = new NetLink();
        link.listen(NetLink.PORT);
        announcer = new LanDiscovery.Announcer();
        announcer.setMode(room.mode);
        announcer.setFull(false);
        announcer.start(myName, NetLink.PORT);
        joined = false;
    }

    /** 참가자가 나간 뒤 같은 방을 다시 엽니다. */
    public void reopenRoom() {
        if (link != null) link.close();
        room.guestName = "";
        room.guestReady = false;
        link = new NetLink();
        link.listen(NetLink.PORT);
        if (announcer != null) announcer.setFull(false);
        joined = false;
        hadGuest = false;
        unjoinedTime = 0f;
    }

    public boolean guestPresent() {
        return host && joined && link != null && link.isConnected();
    }

    /** 참가자: 방장의 인사(방 상태)를 받아 방에 들어왔는지 */
    public boolean joined() {
        return joined;
    }

    public boolean listening() {
        return link != null && link.state() == NetLink.LISTENING;
    }

    /** 방장이 모드를 바꿉니다 (방장만 호출할 수 있습니다). */
    public void setMode(int mode) {
        if (!host) return;
        room.mode = (mode == Proto.MODE_ARMY) ? Proto.MODE_ARMY : Proto.MODE_DUEL;
        if (announcer != null) announcer.setMode(room.mode);
        sendRoom();
    }

    public void setHostChar(int idx) {
        if (!host) return;
        room.hostChar = wrap(idx);
        sendRoom();
    }

    private static int wrap(int i) {
        int n = GameData.VERSUS_ROSTER.length;
        return ((i % n) + n) % n;
    }

    public void sendRoom() {
        if (host && link != null && link.isConnected()) link.send(Proto.room(room));
        roomTimer = 0f;
    }

    // ==================================================================
    //  참가자: 방 찾기 / 입장
    // ==================================================================
    public void startFinder() {
        if (finder == null) {
            finder = new LanDiscovery.Finder();
            finder.start();
        }
    }

    public List<LanDiscovery.Room> foundRooms() {
        return finder == null ? new java.util.ArrayList<LanDiscovery.Room>() : finder.list();
    }

    public boolean finderFailed() {
        return finder != null && finder.failed();
    }

    public void stopFinder() {
        if (finder != null) finder.stop();
        finder = null;
    }

    public void join(String ip, int port) {
        if (link != null) link.close();
        message = "";
        link = new NetLink();
        link.connect(ip, port);
        joined = false;
    }

    public boolean connecting() {
        return link != null && (link.state() == NetLink.CONNECTING
                || (link.state() == NetLink.CONNECTED && !joined));
    }

    /** 참가자: 내 캐릭터 / 준비를 방장에게 알립니다. */
    public void sendGuestState() {
        if (!host && link != null && link.isConnected()) link.send(Proto.guest(myChar, myReady));
    }

    // ==================================================================
    //  방장: 시작 / 복귀
    // ==================================================================
    public boolean canStart() {
        return guestPresent() && room.guestReady;
    }

    public void sendStart() {
        if (link != null) link.send(Proto.start(room.mode, room.hostChar, room.guestChar));
    }

    public void sendBack() {
        room.guestReady = false;
        if (link != null) link.send(Proto.simple(Proto.T_BACK));
        sendRoom();
    }

    public void sendBye() {
        if (link != null && link.isConnected()) link.send(Proto.simple(Proto.T_BYE));
    }

    // ==================================================================
    //  메시지 처리 (매 프레임)
    // ==================================================================
    /**
     * 도착한 메시지를 모두 처리합니다. 전투 중이면 world 를 넘겨 주세요.
     * @return 화면 흐름이 바뀔 만한 사건 (없으면 EV_NONE). 한 번에 가장 중요한 하나만 돌려줍니다.
     */
    public int pump(float dt, BattleWorld world) {
        if (link == null) return EV_NONE;
        int ev = EV_NONE;

        // 연결이 끊겼는지
        link.checkAlive();
        if (link.state() == NetLink.CLOSED) {
            if (link.error().length() > 0 && message.length() == 0) message = link.error();
            return EV_LEFT;
        }

        // 방장: 연결만 하고 인사가 없는 접속은 4초 뒤 정리하고 다시 기다립니다.
        if (host && link.isConnected() && !joined) {
            unjoinedTime += dt;
            if (unjoinedTime > 4f) {
                unjoinedTime = 0f;
                reopenRoom();
                return EV_NONE;
            }
        }

        // 참가자: 연결되면 인사
        if (!host && link.isConnected() && !joined && !helloSent) {
            helloSent = true;
            link.send(Proto.hello(myName));
        }

        byte[] f;
        while ((f = link.poll()) != null) {
            int e = host ? onHostFrame(f, world) : onGuestFrame(f, world);
            if (e != EV_NONE) ev = e;
            if (link.state() == NetLink.CLOSED) break;
        }

        // 방장은 방 상태를 가끔 다시 알려 줍니다 (놓친 메시지 보정)
        if (host && guestPresent()) {
            roomTimer += dt;
            if (roomTimer >= 1f) sendRoom();
        }
        return ev;
    }

    private boolean helloSent = false;
    private float unjoinedTime = 0f;
    /** 이번 방에 참가자가 들어온 적이 있는지 (EV_LEFT 를 어떻게 다룰지 정할 때 씁니다) */
    public boolean hadGuest = false;

    private int onHostFrame(byte[] f, BattleWorld world) {
        switch (f[0]) {
            case Proto.T_HELLO: {
                String name = Proto.parseHello(f);
                if (name == null) {
                    link.send(Proto.simple(Proto.T_REJECT));
                    message = "게임 버전이 달라 입장을 거절했습니다";
                    link.close();
                    return EV_LEFT;
                }
                joined = true;
                hadGuest = true;
                room.guestName = name.length() == 0 ? "참가자" : name;
                room.guestReady = false;
                room.guestChar = 1;
                if (announcer != null) announcer.setFull(true);
                sendRoom();
                return EV_JOINED;
            }
            case Proto.T_GUEST:
                if (f.length >= 3 && joined) {
                    int ch = f[1] & 0xFF;
                    if (ch < GameData.VERSUS_ROSTER.length) room.guestChar = ch;
                    room.guestReady = f[2] != 0;
                    sendRoom();
                }
                return EV_NONE;
            case Proto.T_INPUT:
                if (f.length >= 5 && world != null && world.versus && !world.mirror) {
                    PadInput p = world.pad1;
                    p.setHeldBits(f[1]);
                    p.attackCount = f[2] & 0xFF;
                    p.dodgeCount = f[3] & 0xFF;
                    p.ultCount = f[4] & 0xFF;
                }
                return EV_NONE;
            case Proto.T_BYE:
                message = "참가자가 나갔습니다";
                link.close();
                return EV_LEFT;
            default:
                return EV_NONE;     // 참가자가 보낼 수 없는 종류는 무시합니다
        }
    }

    private int onGuestFrame(byte[] f, BattleWorld world) {
        switch (f[0]) {
            case Proto.T_ROOM: {
                Proto.Room r = Proto.parseRoom(f);
                if (r == null) return EV_NONE;
                joined = true;
                room.mode = r.mode;
                room.hostChar = r.hostChar < GameData.VERSUS_ROSTER.length ? r.hostChar : 0;
                room.guestChar = myChar;                  // 내 선택은 내 화면이 기준
                room.guestReady = myReady;
                room.hostName = r.hostName;
                if (!hostNameKnown) {
                    hostNameKnown = true;
                    sendGuestState();                     // 입장 직후 내 캐릭터를 알립니다
                }
                return EV_NONE;
            }
            case Proto.T_START:
                if (f.length >= 4) {
                    startMode = (f[1] == Proto.MODE_ARMY) ? Proto.MODE_ARMY : Proto.MODE_DUEL;
                    startHostChar = Math.min(f[2] & 0xFF, GameData.VERSUS_ROSTER.length - 1);
                    startGuestChar = Math.min(f[3] & 0xFF, GameData.VERSUS_ROSTER.length - 1);
                    return EV_START;
                }
                return EV_NONE;
            case Proto.T_SNAP:
                if (world != null && world.versus && world.mirror) VersusSync.apply(world, f);
                return EV_NONE;
            case Proto.T_BACK:
                myReady = false;
                room.guestReady = false;
                return EV_BACK;
            case Proto.T_REJECT:
                message = "방이 입장을 거절했습니다 (버전이 다르거나 가득 참)";
                link.close();
                return EV_REJECTED;
            case Proto.T_BYE:
                message = "방장이 방을 닫았습니다";
                link.close();
                return EV_LEFT;
            default:
                return EV_NONE;
        }
    }

    private boolean hostNameKnown = false;

    // ==================================================================
    //  전투 중
    // ==================================================================
    /** 방장: 약 30번/초로 장면을 보냅니다. force 면 시간과 상관없이 바로 보냅니다. */
    public void sendSnapshot(float dt, BattleWorld world, boolean force) {
        if (!host || link == null || !link.isConnected()) return;
        snapTimer += dt;
        if (force || snapTimer >= 1f / 30f) {
            snapTimer = 0f;
            byte[] snap = VersusSync.encode(world);
            if (force) link.send(snap);
            else link.sendDroppable(snap);
        }
    }

    /** 참가자: 내 조작을 방장에게 보냅니다. 바뀐 게 있거나 50ms 가 지났을 때만. */
    public void sendInput(float dt, PadInput pad) {
        if (host || link == null || !link.isConnected()) return;
        inputTimer += dt;
        int bits = pad.heldBits();
        boolean changed = bits != lastBits || pad.attackCount != lastA
                || pad.dodgeCount != lastD || pad.ultCount != lastU;
        if (changed || inputTimer >= 0.05f) {
            inputTimer = 0f;
            lastBits = bits;
            lastA = pad.attackCount;
            lastD = pad.dodgeCount;
            lastU = pad.ultCount;
            link.send(Proto.input(bits, pad.attackCount, pad.dodgeCount, pad.ultCount));
        }
    }

    /** 새 판을 시작할 때 전송 상태를 초기화합니다. */
    public void resetBattleState() {
        lastBits = lastA = lastD = lastU = -1;
        inputTimer = 0f;
        snapTimer = 0f;
    }

    public boolean isLinkAlive() {
        return link != null && link.state() != NetLink.CLOSED;
    }

    // ==================================================================
    public void close() {
        if (link != null) {
            sendBye();
            link.close();
        }
        if (announcer != null) announcer.stop();
        if (finder != null) finder.stop();
        link = null;
        announcer = null;
        finder = null;
    }
}
