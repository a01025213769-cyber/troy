package com.example.trojanprototype.core;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Wi-Fi 방송(UDP broadcast)을 받으려면 일부 기기에서 MulticastLock 이 필요합니다.
 *
 * 담당: 유장원 (시스템)
 *
 * 방 목록을 찾는 동안에만 잠금을 쥐고, 끝나면 바로 놓습니다. (배터리 보호)
 * 권한이 없거나 Wi-Fi 서비스가 없어도 게임이 멈추지 않도록 모든 호출을 감쌌습니다.
 * 잠금이 없어도 방장 주소를 직접 입력해서 접속할 수 있습니다.
 */
final class WifiHelper {

    private final Context ctx;
    private WifiManager.MulticastLock lock;

    WifiHelper(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    void acquireMulticast() {
        try {
            if (lock != null && lock.isHeld()) return;
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            lock = wm.createMulticastLock("troywar-lobby");
            lock.setReferenceCounted(false);
            lock.acquire();
        } catch (Throwable ignored) {
            lock = null;
        }
    }

    void releaseMulticast() {
        try {
            if (lock != null && lock.isHeld()) lock.release();
        } catch (Throwable ignored) {
        }
        lock = null;
    }
}
