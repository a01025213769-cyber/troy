package com.example.trojanprototype.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 같은 Wi-Fi 안에서 열려 있는 방을 찾는 장치 (UDP 방송).
 *
 * 담당: 유장원 (시스템)
 *
 *  방장   : Announcer — 1초마다 "여기 방이 있어요" 를 방송합니다.
 *  참가자 : Finder    — 방송을 들어서 방 목록을 만듭니다.
 *
 * 공유기에 따라 방송이 막혀 있을 수 있어서, 참가자 화면에는 방장의 IP 를 직접 입력하는
 * 길도 함께 있습니다. (방장 화면에 자기 IP 가 크게 표시됩니다)
 *
 * 안드로이드 클래스를 쓰지 않는 순수 자바입니다.
 * (안드로이드에서 방송을 받으려면 MulticastLock 이 필요하며, 그건 WifiHelper 가 맡습니다.)
 */
public final class LanDiscovery {

    private LanDiscovery() {}

    public static final int PORT = 47778;
    private static final String MAGIC = "TROYWAR1";

    /** 화면에 보여 줄 방 한 칸 */
    public static final class Room {
        public String ip = "";
        public int port = NetLink.PORT;
        public String name = "";
        public int mode = 0;
        public boolean full = false;
        long lastSeen = 0L;
    }

    // ==================================================================
    //  내 주소
    // ==================================================================
    /** 이 기기의 Wi-Fi(사설망) IPv4 주소. 없으면 빈 문자열. */
    public static String localIp() {
        String best = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> ads = ni.getInetAddresses();
                while (ads.hasMoreElements()) {
                    InetAddress a = ads.nextElement();
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress()) continue;
                    if (a.isSiteLocalAddress()) {
                        String name = ni.getName() == null ? "" : ni.getName();
                        // wlan0 처럼 Wi-Fi 로 보이는 것을 우선합니다.
                        if (name.startsWith("wlan") || name.startsWith("eth") || best.length() == 0) {
                            best = a.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return best;
    }

    private static List<InetAddress> broadcastTargets() {
        List<InetAddress> out = new ArrayList<InetAddress>();
        try {
            out.add(InetAddress.getByName("255.255.255.255"));
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress b = ia.getBroadcast();
                    if (b != null) out.add(b);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** 화면에 올릴 이름에서 이상한 글자와 구분자를 걷어 냅니다. */
    public static String clean(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < max; i++) {
            char ch = s.charAt(i);
            if (ch == '|' || ch < 32 || ch == 127) continue;
            sb.append(ch);
        }
        return sb.toString().trim();
    }

    // ==================================================================
    //  방장: 방송하기
    // ==================================================================
    public static final class Announcer {
        private volatile boolean running = false;
        private volatile int mode = 0;
        private volatile boolean full = false;
        private DatagramSocket sock;

        public void setMode(int m) {
            mode = m;
        }

        public void setFull(boolean f) {
            full = f;
        }

        public void start(final String name, final int tcpPort) {
            if (running) return;
            running = true;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sock = new DatagramSocket();
                        sock.setBroadcast(true);
                        while (running) {
                            String msg = MAGIC + "|" + tcpPort + "|" + clean(name, 12) + "|" + mode + "|"
                                    + (full ? 1 : 0);
                            byte[] b = msg.getBytes("UTF-8");
                            List<InetAddress> targets = broadcastTargets();
                            for (int i = 0; i < targets.size(); i++) {
                                try {
                                    sock.send(new DatagramPacket(b, b.length, targets.get(i), PORT));
                                } catch (IOException ignored) {
                                }
                            }
                            Thread.sleep(1000);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (sock != null) sock.close();
                    }
                }
            }, "net-announce");
            t.setDaemon(true);
            t.start();
        }

        public void stop() {
            running = false;
            if (sock != null) sock.close();
        }
    }

    // ==================================================================
    //  참가자: 방 찾기
    // ==================================================================
    public static final class Finder {
        private volatile boolean running = false;
        private final List<Room> rooms = new ArrayList<Room>();
        private DatagramSocket sock;
        private volatile boolean failed = false;

        /** 방송을 들을 수 없는 상태인지 (그럴 땐 IP 직접 입력을 안내합니다) */
        public boolean failed() {
            return failed;
        }

        public void start() {
            if (running) return;
            running = true;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sock = new DatagramSocket(null);
                        sock.setReuseAddress(true);
                        sock.bind(new java.net.InetSocketAddress(PORT));
                        sock.setSoTimeout(500);
                        byte[] buf = new byte[256];
                        while (running) {
                            DatagramPacket p = new DatagramPacket(buf, buf.length);
                            try {
                                sock.receive(p);
                            } catch (java.net.SocketTimeoutException e) {
                                continue;
                            }
                            handle(p);
                        }
                    } catch (Exception e) {
                        failed = true;
                    } finally {
                        if (sock != null) sock.close();
                    }
                }
            }, "net-find");
            t.setDaemon(true);
            t.start();
        }

        private void handle(DatagramPacket p) {
            try {
                String s = new String(p.getData(), 0, Math.min(p.getLength(), 200), "UTF-8");
                String[] f = s.split("\\|", -1);
                if (f.length < 5 || !MAGIC.equals(f[0])) return;
                int port = Integer.parseInt(f[1].trim());
                if (port < 1024 || port > 65535) return;
                String ip = p.getAddress().getHostAddress();
                synchronized (rooms) {
                    Room r = null;
                    for (int i = 0; i < rooms.size(); i++) {
                        if (rooms.get(i).ip.equals(ip)) r = rooms.get(i);
                    }
                    if (r == null) {
                        if (rooms.size() >= 8) return;
                        r = new Room();
                        r.ip = ip;
                        rooms.add(r);
                    }
                    r.port = port;
                    r.name = clean(f[2], 12);
                    r.mode = "1".equals(f[3].trim()) ? 1 : 0;
                    r.full = "1".equals(f[4].trim());
                    r.lastSeen = System.currentTimeMillis();
                }
            } catch (Exception ignored) {
                // 우리 게임의 방송이 아니면 그냥 무시합니다.
            }
        }

        /** 최근 4초 안에 방송이 들린 방 목록 */
        public List<Room> list() {
            long now = System.currentTimeMillis();
            List<Room> out = new ArrayList<Room>();
            synchronized (rooms) {
                for (int i = rooms.size() - 1; i >= 0; i--) {
                    if (now - rooms.get(i).lastSeen > 4000L) rooms.remove(i);
                }
                out.addAll(rooms);
            }
            return out;
        }

        public void stop() {
            running = false;
            if (sock != null) sock.close();
        }
    }
}
