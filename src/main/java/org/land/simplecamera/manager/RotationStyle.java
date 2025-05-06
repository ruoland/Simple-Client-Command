package org.land.simplecamera.manager;

// === Rotation Style Enum 추가 ===
public enum RotationStyle {
    PLAYER_CONTROLLED, // 플레이어가 마우스로 회전 제어 (기본)
    FIXED,             // 특정 Yaw/Pitch/Roll 값으로 고정
    FOLLOW_PLAYER      // 플레이어의 현재 Yaw/Pitch를 따라감 (Roll은 FIXED 값 사용)
}
