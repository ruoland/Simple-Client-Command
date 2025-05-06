package org.land.simplecamera.feature.cca.control;

import net.minecraft.nbt.NbtCompound;

public class ControlSettings {
    private boolean disableMoveSideways = false;
    private boolean disableMoveForwards = false;
    private boolean disableAllMove = false;
    private boolean mouseRotationLocked = false; // 마우스 입력 잠금 여부
    // control rotation 명령으로 설정되는 플레이어 회전 목표값 (FOLLOW_PLAYER와는 별개)
    private float playerYaw = 0, playerPitch = 0;

    public ControlSettings(){}

    public boolean isDisableAllMove() {
        return disableAllMove;
    }

    public boolean isDisableMoveForwards() {
        return disableMoveForwards;
    }

    public boolean isDisableMoveSideways() {
        return disableMoveSideways;
    }

    public boolean isMouseRotationLocked() {
        return mouseRotationLocked;
    }

    public float getPlayerPitch() {
        return playerPitch;
    }

    public float getPlayerYaw() {
        return playerYaw;
    }

    public void setMouseRotationLocked(boolean mouseRotationLocked) {
        this.mouseRotationLocked = mouseRotationLocked;
    }

    public void setPlayerYawAndPitch(float playerYaw, float playerPitch) {
        this.playerYaw = playerYaw;
        this.playerPitch = playerPitch;
    }
    public ControlSettings setPlayerPitch(float playerPitch) {
        this.playerPitch = playerPitch;
        return this;
    }

    public ControlSettings setPlayerYaw(float playerYaw) {
        this.playerYaw = playerYaw;
        return this;
    }

    public ControlSettings setDisableAllMove(boolean disableAllMove) {
        this.disableAllMove = disableAllMove;
        return this;
    }

    public ControlSettings setDisableMoveForwards(boolean disableMoveForwards) {
        this.disableMoveForwards = disableMoveForwards;
        return this;
    }

    public ControlSettings setDisableMoveSideways(boolean disableMoveSideways) {
        this.disableMoveSideways = disableMoveSideways;
        return this;
    }

    // NBT Methods for persistence and sync
    public void writeToNbt(NbtCompound nbt) {
        nbt.putBoolean("disableMoveSideways", this.disableMoveSideways);
        nbt.putBoolean("disableMoveForwards", this.disableMoveForwards);
        nbt.putBoolean("disableAllMove", this.disableAllMove);
        nbt.putBoolean("mouseRotationLocked", this.mouseRotationLocked);
        nbt.putFloat("playerYaw", this.playerYaw);
        nbt.putFloat("playerPitch", this.playerPitch);
    }

    public void readFromNbt(NbtCompound nbt) {
        this.disableMoveSideways = nbt.getBoolean("disableMoveSideways");
        this.disableMoveForwards = nbt.getBoolean("disableMoveForwards");
        this.disableAllMove = nbt.getBoolean("disableAllMove");
        this.mouseRotationLocked = nbt.getBoolean("mouseRotationLocked");
        this.playerYaw = nbt.getFloat("playerYaw");
        this.playerPitch = nbt.getFloat("playerPitch");

    }
}
