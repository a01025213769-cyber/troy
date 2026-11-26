package com.example.trojanprototype.battle;

import com.example.trojanprototype.core.TouchInput;

/**
 * 한 명분의 조작 상태. (왼쪽 · 오른쪽 · 방어는 누르고 있는 동안, 공격 · 회피 · 필살기는 한 번 눌린 순간)
 *
 * 담당: 유장원 (시스템)
 *
 * 1인 플레이는 TouchInput 을 직접 읽었지만, 2인 대전에서는
 * "내 손가락"과 "Wi-Fi 로 건너온 상대의 조작"을 똑같은 모양으로 다뤄야 합니다.
 * 그래서 조작을 이 작은 상자에 담아 BattleWorld 에 넘깁니다.
 *
 * 공격 · 회피 · 필살기는 '눌린 횟수'를 셉니다. 네트워크 패킷이 하나 늦거나 합쳐져도
 * 누른 것이 사라지지 않고, 횟수 차이만큼 반응하면 되기 때문입니다.
 */
public final class PadInput {

    public boolean left, right, guard;
    /** 지금까지 누른 누적 횟수 (0~255 를 돌아가며 셉니다) */
    public int attackCount, dodgeCount, ultCount;

    // 읽어 간 위치 (소비하는 쪽이 갖고 있습니다)
    private int seenAttack, seenDodge, seenUlt;

    public void clear() {
        left = right = guard = false;
        attackCount = dodgeCount = ultCount = 0;
        seenAttack = seenDodge = seenUlt = 0;
    }

    /** 내 터치 입력을 옮겨 담습니다. (TouchInput 의 '눌린 순간' 은 여기서 소비됩니다) */
    public void readFrom(TouchInput in) {
        left = in.isHeld(TouchInput.BTN_LEFT);
        right = in.isHeld(TouchInput.BTN_RIGHT);
        guard = in.isHeld(TouchInput.BTN_GUARD);
        if (in.consumePress(TouchInput.BTN_ATTACK)) attackCount = (attackCount + 1) & 0xFF;
        if (in.consumePress(TouchInput.BTN_DODGE)) dodgeCount = (dodgeCount + 1) & 0xFF;
        if (in.consumePress(TouchInput.BTN_ULT)) ultCount = (ultCount + 1) & 0xFF;
    }

    public int heldBits() {
        return (left ? 1 : 0) | (right ? 2 : 0) | (guard ? 4 : 0);
    }

    public void setHeldBits(int bits) {
        left = (bits & 1) != 0;
        right = (bits & 2) != 0;
        guard = (bits & 4) != 0;
    }

    public int dir() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    /** 아직 읽지 않은 '눌림' 을 모두 버립니다. */
    public void discard() {
        seenAttack = attackCount;
        seenDodge = dodgeCount;
        seenUlt = ultCount;
    }

    // ---- 소비 (BattleWorld 가 한 프레임에 한 번씩 부릅니다)
    public boolean takeAttack() {
        if (seenAttack == attackCount) return false;
        seenAttack = attackCount;
        return true;
    }

    public boolean takeDodge() {
        if (seenDodge == dodgeCount) return false;
        seenDodge = dodgeCount;
        return true;
    }

    public boolean takeUlt() {
        if (seenUlt == ultCount) return false;
        seenUlt = ultCount;
        return true;
    }
}
