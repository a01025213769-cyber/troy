package com.example.trojanprototype.net;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Wi-Fi 로 기기 두 대를 잇는 TCP 연결 하나.
 *
 * 담당: 유장원 (시스템)
 *
 * ── 쓰는 법 ─────────────────────────────────────────────
 *   방장   : new NetLink().listen(PORT)          — 참가자가 들어오면 CONNECTED
 *   참가자 : new NetLink().connect(ip, PORT)     — 연결되면 CONNECTED
 *   그 뒤로는 양쪽 똑같이  send(frame) / poll()  만 씁니다.
 *
 * ── 왜 이렇게 만들었나 ──────────────────────────────────
 *  · 안드로이드는 메인(화면) 스레드에서 네트워크를 쓰면 막아 버립니다.
 *    그래서 연결 · 읽기 · 쓰기는 모두 따로 도는 스레드에서 하고,
 *    게임 쪽은 poll() 로 "도착해 있는 것"만 꺼내 씁니다. (기다리지 않음)
 *  · 한 번에 한 명만 받습니다. 방장에게 두 번째 접속이 오면 연결 직후 닫습니다.
 *  · 프레임 하나 = [길이 2바이트][종류 1바이트][내용...]. 길이가 비정상이면 연결을 끊습니다.
 *  · 1초마다 PING 을 보내고, 일정 시간 아무것도 안 오면 끊어진 것으로 봅니다.
 *
 * 이 파일은 안드로이드 클래스를 하나도 쓰지 않아서 PC 에서도 그대로 시험할 수 있습니다.
 */
public final class NetLink {

    public static final int PORT = 47777;

    // 연결 상태
    public static final int IDLE = 0;
    public static final int LISTENING = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int CLOSED = 4;

    /** 이 시간(ms) 동안 아무 것도 못 받으면 끊긴 것으로 봅니다. */
    public static final int TIMEOUT_MS = 6000;
    /** 프레임 하나의 최대 크기. 이보다 크면 비정상 연결입니다. */
    public static final int MAX_FRAME = 16384;

    private static final byte T_PING = 9;

    private volatile int state = IDLE;
    private volatile String error = "";
    private volatile long lastRx = 0L;
    private volatile String peer = "";

    private ServerSocket server;
    private Socket socket;
    private final Object lock = new Object();

    private final ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<byte[]>();
    private final LinkedBlockingQueue<byte[]> outbox = new LinkedBlockingQueue<byte[]>();

    public int state() {
        return state;
    }

    public boolean isConnected() {
        return state == CONNECTED;
    }

    /** 사람이 읽을 수 있는 실패 이유 (연결이 안 될 때만 채워집니다) */
    public String error() {
        return error;
    }

    /** 상대 기기의 주소 */
    public String peer() {
        return peer;
    }

    /** 마지막으로 무언가를 받은 뒤 흐른 시간(ms) */
    public long silenceMs() {
        return lastRx == 0L ? 0L : System.currentTimeMillis() - lastRx;
    }

    // ==================================================================
    //  방장: 기다리기
    // ==================================================================
    public void listen(final int port) {
        state = LISTENING;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(port));
                    synchronized (lock) {
                        if (state == CLOSED) {
                            ss.close();
                            return;
                        }
                        server = ss;
                    }
                    Socket s = ss.accept();
                    // 한 명만 받습니다. 대기 소켓은 바로 닫아서 두 번째 접속을 막습니다.
                    try {
                        ss.close();
                    } catch (IOException ignored) {
                    }
                    attach(s);
                } catch (IOException e) {
                    if (state != CLOSED) fail("방을 열 수 없습니다 (" + e.getMessage() + ")");
                } finally {
                    if (ss != null && state != CONNECTED) {
                        try {
                            ss.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }, "net-accept");
        t.setDaemon(true);
        t.start();
    }

    // ==================================================================
    //  참가자: 접속하기
    // ==================================================================
    public void connect(final String host, final int port) {
        state = CONNECTING;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket s = new Socket();
                try {
                    synchronized (lock) {
                        if (state == CLOSED) {
                            s.close();
                            return;
                        }
                        socket = s;
                    }
                    s.connect(new InetSocketAddress(host, port), 4000);
                    attach(s);
                } catch (IOException e) {
                    if (state != CLOSED) fail("연결하지 못했습니다 — 같은 Wi-Fi 인지, 방 주소가 맞는지 확인하세요");
                } catch (RuntimeException e) {
                    if (state != CLOSED) fail("주소가 올바르지 않습니다");
                }
            }
        }, "net-connect");
        t.setDaemon(true);
        t.start();
    }

    // ==================================================================
    //  연결된 뒤
    // ==================================================================
    private void attach(final Socket s) throws IOException {
        s.setTcpNoDelay(true);
        s.setKeepAlive(true);
        synchronized (lock) {
            if (state == CLOSED) {
                s.close();
                return;
            }
            socket = s;
        }
        peer = String.valueOf(s.getInetAddress() == null ? "" : s.getInetAddress().getHostAddress());
        lastRx = System.currentTimeMillis();
        state = CONNECTED;

        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                readLoop(s);
            }
        }, "net-read");
        reader.setDaemon(true);
        reader.start();

        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                writeLoop(s);
            }
        }, "net-write");
        writer.setDaemon(true);
        writer.start();
    }

    private void readLoop(Socket s) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream(), 8192));
            while (state == CONNECTED) {
                int len = in.readUnsignedShort();
                if (len < 1 || len > MAX_FRAME) {
                    fail("잘못된 데이터를 받아 연결을 닫았습니다");
                    return;
                }
                byte[] body = new byte[len];
                in.readFully(body);
                lastRx = System.currentTimeMillis();
                if (body[0] == T_PING) continue;
                inbox.add(body);
            }
        } catch (IOException e) {
            if (state == CONNECTED) fail("상대와의 연결이 끊어졌습니다");
        }
    }

    private void writeLoop(Socket s) {
        try {
            OutputStream out = s.getOutputStream();
            while (state == CONNECTED) {
                byte[] f = outbox.poll(1000, TimeUnit.MILLISECONDS);
                if (f == null) {
                    f = new byte[]{T_PING};                 // 조용하면 살아 있다는 신호
                    if (System.currentTimeMillis() - lastRx > TIMEOUT_MS) {
                        fail("상대의 응답이 없어 연결을 닫았습니다");
                        return;
                    }
                }
                out.write((f.length >> 8) & 0xFF);
                out.write(f.length & 0xFF);
                out.write(f);
                if (outbox.isEmpty()) out.flush();
            }
        } catch (IOException e) {
            if (state == CONNECTED) fail("상대와의 연결이 끊어졌습니다");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================================================================
    //  게임 쪽에서 쓰는 함수
    // ==================================================================
    /** 프레임 하나를 보냅니다. frame[0] 이 종류. 연결이 없으면 버립니다. */
    public void send(byte[] frame) {
        if (state != CONNECTED || frame == null || frame.length == 0 || frame.length > MAX_FRAME) return;
        outbox.add(frame);
    }

    /** 밀려 있는 게 많으면 버리는 전송 — 장면 스냅샷처럼 최신 것만 의미 있는 데이터용 */
    public void sendDroppable(byte[] frame) {
        if (outbox.size() > 6) return;
        send(frame);
    }

    /** 도착해 있는 프레임 하나를 꺼냅니다. 없으면 null. */
    public byte[] poll() {
        return inbox.poll();
    }

    /** 상대가 무응답이면 연결을 끊습니다. 게임 스레드에서 가끔 불러 주세요. */
    public void checkAlive() {
        if (state == CONNECTED && lastRx != 0L
                && System.currentTimeMillis() - lastRx > TIMEOUT_MS) {
            fail("상대의 응답이 없어 연결을 닫았습니다");
        }
    }

    private void fail(String why) {
        if (state == CLOSED) return;
        error = why;
        closeSockets();
        state = CLOSED;
    }

    /** 연결을 닫습니다. 여러 번 불러도 안전합니다. */
    public void close() {
        if (state != CLOSED) {
            // 마지막으로 남은 인사(BYE)가 나갈 짧은 틈을 줍니다.
            if (state == CONNECTED) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            state = CLOSED;
        }
        closeSockets();
    }

    private void closeSockets() {
        synchronized (lock) {
            try {
                if (server != null) server.close();
            } catch (IOException ignored) {
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
