package org.land.simplecamera.feature.cca.camera;


import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper; // NBT Helper 추가
import net.minecraft.util.math.Vec3d;
import org.land.simplecamera.feature.client.control.EnumMoveStyle;

public class RelativeSettings {
    private EnumMoveStyle moveStyle = EnumMoveStyle.ALL;
    private double offsetX = 0.0;
    private double offsetY = 2.0;
    private double offsetZ = 0.0;
    private boolean moveStyleUpdate = false; // 믹스인 초기화 트리거 (명령어/설정 변경 시 사용)

    // === 재접속 시 상태 복원을 위한 필드 추가 ===
    private boolean isRelativeModeActive = false; // 현재 Relative 모드가 활성화 상태인지 여부
    private Vec3d initialPositionSnapshot = Vec3d.ZERO; // Relative 모드 시작 시점의 플레이어 위치 스냅샷
    private float initialYawSnapshot = 0.0F; // Relative 모드 시작 시점의 기준 Yaw 스냅샷
    private float initialPitchSnapshot = 0.0F; // Relative 모드 시작 시점의 기준 Pitch 스냅샷
    // ======================================


    // Default constructor needed for NBT/Gson
    public RelativeSettings() {}

    // Getters
    public EnumMoveStyle getMoveStyle() { return moveStyle; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public Vec3d getOffset() { return new Vec3d(offsetX, offsetY, offsetZ); }
    public boolean isMoveStyleUpdate() { return moveStyleUpdate; } // Getter for moveStyleUpdate

    // === 재접속 시 상태 복원을 위한 필드 Getter ===
    public boolean isRelativeModeActive() { return isRelativeModeActive; }
    public Vec3d getInitialPositionSnapshot() { return initialPositionSnapshot; }
    public float getInitialYawSnapshot() { return initialYawSnapshot; }
    public float getInitialPitchSnapshot() { return initialPitchSnapshot; }
    // ======================================


    // Setters (Component will call these and then sync)
    public void setMoveStyle(EnumMoveStyle moveStyle) { this.moveStyle = moveStyle; }
    public void setOffset(double x, double y, double z){
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }
    public void setOffset(Vec3d vec3d){
        setOffset(vec3d.x, vec3d.y, vec3d.z);
    }
    public void addOffset(double x, double y, double z){
        this.offsetX += x;
        this.offsetY += y;
        this.offsetZ += z;
    }

    public void setMoveStyleUpdate(boolean b) { this.moveStyleUpdate = b; }


    // === 재접속 시 상태 복원을 위한 필드 Setter ===
    public void setRelativeModeActive(boolean relativeModeActive) { this.isRelativeModeActive = relativeModeActive; }
    public void setInitialPositionSnapshot(Vec3d initialPositionSnapshot) { this.initialPositionSnapshot = initialPositionSnapshot; }
    public void setInitialYawSnapshot(float initialYawSnapshot) { this.initialYawSnapshot = initialYawSnapshot; }
    public void setInitialPitchSnapshot(float initialPitchSnapshot) { this.initialPitchSnapshot = initialPitchSnapshot; }
    // ======================================


    // NBT Methods for persistence and sync
    public void writeToNbt(NbtCompound nbt) {
        nbt.putString("moveStyle", this.moveStyle.name());
        nbt.putDouble("offsetX", this.offsetX);
        nbt.putDouble("offsetY", this.offsetY);
        nbt.putDouble("offsetZ", this.offsetZ);
        // moveStyleUpdate는 임시 플래그이므로 저장하지 않습니다.
        // nbt.putBoolean("moveStyleUpdate", this.moveStyleUpdate); // <-- 제거

        // === 재접속 시 상태 복원을 위한 필드 NBT 저장 ===
        nbt.putBoolean("isRelativeModeActive", this.isRelativeModeActive);
        if (this.isRelativeModeActive && this.initialPositionSnapshot != null) { // 활성화 상태이고 위치 값이 유효하면 저장
            NbtCompound posNbt = new NbtCompound();
            posNbt.putDouble("x", this.initialPositionSnapshot.x);
            posNbt.putDouble("y", this.initialPositionSnapshot.y);
            posNbt.putDouble("z", this.initialPositionSnapshot.z);
            nbt.put("initialPositionSnapshot", posNbt);

            nbt.putFloat("initialYawSnapshot", this.initialYawSnapshot);
            nbt.putFloat("initialPitchSnapshot", this.initialPitchSnapshot);
        }
        // ======================================
    }

    public void readFromNbt(NbtCompound nbt) {
        if (nbt.contains("moveStyle")) {
            try { this.moveStyle = EnumMoveStyle.valueOf(nbt.getString("moveStyle")); } catch (IllegalArgumentException e) { this.moveStyle = EnumMoveStyle.ALL; }
        } else { this.moveStyle = EnumMoveStyle.ALL; } // Default if missing

        this.offsetX = nbt.contains("offsetX") ? nbt.getDouble("offsetX") : 0.0;
        this.offsetY = nbt.contains("offsetY") ? nbt.getDouble("offsetY") : 2.0; // Default 2.0
        this.offsetZ = nbt.contains("offsetZ") ? nbt.getDouble("offsetZ") : 0.0;

        // moveStyleUpdate는 불러올 때 항상 false로 초기화됩니다.
        this.moveStyleUpdate = false; // <-- 항상 false로 초기화

        // === 재접속 시 상태 복원을 위한 필드 NBT 로드 ===
        this.isRelativeModeActive = nbt.getBoolean("isRelativeModeActive"); // 기본값 false
        if (this.isRelativeModeActive && nbt.contains("initialPositionSnapshot", NbtCompound.COMPOUND_TYPE)) {
            NbtCompound posNbt = nbt.getCompound("initialPositionSnapshot");
            this.initialPositionSnapshot = new Vec3d(posNbt.getDouble("x"), posNbt.getDouble("y"), posNbt.getDouble("z"));
            this.initialYawSnapshot = nbt.getFloat("initialYawSnapshot");
            this.initialPitchSnapshot = nbt.getFloat("initialPitchSnapshot");
        } else {
            // Relative 모드가 비활성화 상태로 저장되었거나 스냅샷 데이터가 유효하지 않으면 초기화
            this.initialPositionSnapshot = Vec3d.ZERO;
            this.initialYawSnapshot = 0.0F;
            this.initialPitchSnapshot = 0.0F;
            this.isRelativeModeActive = false; // 데이터가 없으면 강제로 비활성화 상태로 설정
        }
        // ======================================
    }
}