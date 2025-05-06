package org.land.simplecamera.feature.cca.camera;

import net.minecraft.nbt.NbtCompound;
import org.land.simplecamera.feature.client.camera.EnumPlacement;
import org.land.simplecamera.manager.Perspective;
import org.land.simplecamera.manager.RotationStyle;

public class CommonSettings {
    private EnumPlacement placementType = EnumPlacement.NONE;
    private boolean isRenderHand = true;
    private RotationStyle rotationStyle = RotationStyle.PLAYER_CONTROLLED;
    private float lockYaw = 0, lockPitch = 0, cameraRoll = 0;
    private float targetYaw = 0, targetPitch = 0; // CCA 예제에서 회전 목표값 저장용 필드 추가
    private boolean isMouseLocked = false; // CCA 예제에서 마우스 잠금 상태 저장용 필드 추가
    private float lastPlayerYaw = 0; // Relative 초기화 기준용
    private float lastPlayerPitch = 0; // Relative 초기화 기준용

    private Perspective perspective = Perspective.FIRST_PERSON;
    private boolean shouldUpdate = false;
    // Default constructor needed for NBT/Gson
    public CommonSettings() {}

    // Getters
    public EnumPlacement getPlacementType() { return placementType; }
    public boolean isRenderHand() { return isRenderHand; }
    public RotationStyle getRotationStyle() { return rotationStyle; }
    public float getLockYaw() { return lockYaw; }
    public float getLockPitch() { return lockPitch; }
    public float getCameraRoll() { return cameraRoll; }
    public float getTargetYaw() { return targetYaw; }
    public float getTargetPitch() { return targetPitch; }
    public boolean isMouseLocked() { return isMouseLocked; }
    public float getLastPlayerYaw() { return lastPlayerYaw; }
    public float getLastPlayerPitch() { return lastPlayerPitch; }


    // Setters (Component will call these and then sync)
    public void setPlacementType(EnumPlacement placementType) { this.placementType = placementType; }
    public void setRenderHand(boolean renderHand) { this.isRenderHand = renderHand; }
    public void setRotationStyle(RotationStyle rotationStyle) { this.rotationStyle = rotationStyle; }
    public void setLockYaw(float lockYaw) { this.lockYaw = lockYaw; }
    public void setLockPitch(float lockPitch) { this.lockPitch = lockPitch; }
    public void setCameraRoll(float cameraRoll) { this.cameraRoll = cameraRoll; }
    public void setTargetYaw(float targetYaw) { this.targetYaw = targetYaw; }
    public void setTargetPitch(float targetPitch) { this.targetPitch = targetPitch; }
    public void setMouseLocked(boolean mouseLocked) { this.isMouseLocked = mouseLocked; }
    public void setLastPlayerYaw(float lastPlayerYaw) { this.lastPlayerYaw = lastPlayerYaw; }
    public void setLastPlayerPitch(float lastPlayerPitch) { this.lastPlayerPitch = lastPlayerPitch; }

    // NBT Methods for persistence and sync
    public void writeToNbt(NbtCompound nbt) {
        nbt.putString("placementType", this.placementType.name());
        nbt.putBoolean("isRenderHand", this.isRenderHand);
        nbt.putString("rotationStyle", this.rotationStyle.name());
        nbt.putDouble("lockYaw", this.lockYaw);
        nbt.putDouble("lockPitch", this.lockPitch);
        nbt.putDouble("cameraRoll", this.cameraRoll);
        nbt.putFloat("targetYaw", this.targetYaw); // Save new fields
        nbt.putFloat("targetPitch", this.targetPitch); // Save new fields
        nbt.putBoolean("isMouseLocked", this.isMouseLocked); // Save new field
        nbt.putFloat("lastPlayerYaw", this.lastPlayerYaw); // Save new fields
        nbt.putFloat("lastPlayerPitch", this.lastPlayerPitch); // Save new fields
        nbt.putString("perspective", this.perspective.name());
        nbt.putBoolean("shouldUpdate", this.shouldUpdate);

    }

    public void readFromNbt(NbtCompound nbt) {
        // Use contains() for backward compatibility
        if (nbt.contains("placementType")) {
            try { this.placementType = EnumPlacement.valueOf(nbt.getString("placementType")); } catch (IllegalArgumentException e) { this.placementType = EnumPlacement.NONE; }
        } else { this.placementType = EnumPlacement.NONE; }

        this.isRenderHand = nbt.getBoolean("isRenderHand"); // boolean defaults to false if missing

        if (nbt.contains("rotationStyle")) {
             try { this.rotationStyle = RotationStyle.valueOf(nbt.getString("rotationStyle")); } catch (IllegalArgumentException e) { this.rotationStyle = RotationStyle.PLAYER_CONTROLLED; }
        } else { this.rotationStyle = RotationStyle.PLAYER_CONTROLLED; } // Default if missing

        this.lockYaw = nbt.getFloat("lockYaw");
        this.lockPitch = nbt.getFloat("lockPitch");
        this.cameraRoll = nbt.getFloat("cameraRoll");

        // Read new fields, providing defaults if they don't exist in old saves
        this.targetYaw = nbt.contains("targetYaw") ? nbt.getFloat("targetYaw") : 0;
        this.targetPitch = nbt.contains("targetPitch") ? nbt.getFloat("targetPitch") : 0;
        this.isMouseLocked = nbt.getBoolean("isMouseLocked"); // boolean defaults to false

        this.lastPlayerYaw = nbt.contains("lastPlayerYaw") ? nbt.getFloat("lastPlayerYaw") : 0;
        this.lastPlayerPitch = nbt.contains("lastPlayerPitch") ? nbt.getFloat("lastPlayerPitch") : 0;

        if (nbt.contains("perspective")) {
            try { this.perspective = Perspective.valueOf(nbt.getString("perspective")); } catch (IllegalArgumentException e) { this.perspective = Perspective.FIRST_PERSON; }
        } else { this.perspective = Perspective.FIRST_PERSON; }

        this.shouldUpdate = nbt.getBoolean("shouldUpdate");


    }

    public Perspective getPerspective() {
        return perspective;
    }

    public CommonSettings setPerspective(Perspective perspective) {
        this.perspective = perspective;
        return this;
    }

    public boolean isShouldUpdate() {
        return shouldUpdate;
    }

    public void setShouldUpdate(boolean b) {
        this.shouldUpdate = b;
    }
}