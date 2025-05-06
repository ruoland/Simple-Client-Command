package org.land.simplecamera.manager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.land.simplecamera.feature.cca.CameraComponent;
import org.land.simplecamera.feature.cca.ControlComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.camera.AbsoluteSettings;
import org.land.simplecamera.feature.cca.camera.CommonSettings;
import org.land.simplecamera.feature.cca.camera.RelativeSettings;
import org.land.simplecamera.feature.cca.control.ControlSettings;
import org.land.simplecamera.feature.client.camera.EnumPlacement;
import org.land.simplecamera.feature.client.camera.EnumResetType;


import org.land.simplecamera.feature.client.control.EnumMoveStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class CCACameraManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CCACameraManager.class);
    private static final String PROFILE_FOLDER = "simplecamera/profiles"; // 프로필 저장 폴더
    public static void handlePerspective(PlayerEntity player, Perspective type) {
        getCameraComponent(player).getCommonSettings().setPerspective(type);
    }
    public static void handleReset(PlayerEntity player, EnumResetType type){
        switch (type) {
            case RESET_ALL:
                getCameraComponent(player).setCommonSettings(new CommonSettings());
                getCameraComponent(player).setRelativeSettings(new RelativeSettings());
                getCameraComponent(player).setAbsoluteSettings( new AbsoluteSettings());
                getCommonSettings(player).setRotationStyle(RotationStyle.PLAYER_CONTROLLED);
                // 초기화 시 마지막 플레이어 방향도 기본값으로 설정
                getCommonSettings(player).setLastPlayerYaw(0);
                getCommonSettings(player).setLastPlayerPitch(0);
                break;

            case RESET_FOLLOW_STYLE:
                // follow 관련 설정 초기화 (Relative PlacementType 해제, MoveStyle ALL, RotationStyle PLAYER_CONTROLLED)
                getRelativeSettings(player).setMoveStyle(EnumMoveStyle.ALL); // MoveStyle 리셋
                getCommonSettings(player).setRotationStyle(RotationStyle.PLAYER_CONTROLLED); // RotationStyle 리셋
                getCommonSettings(player).setPlacementType(EnumPlacement.NONE); // PlacementType 리셋
                getControlComponent(player).setMouseRotationLocked(false); // 마우스 회전 잠금 해제
                getControlComponent(player).setPlayerYaw(0); // KeyboardManager 회전 목표 초기화
                getControlComponent(player).setPlayerPitch(0); // KeyboardManager 회전 목표 초기화
                getCommonSettings(player).setShouldUpdate(true); // 업데이트 필요
                LOGGER.debug("Camera RESET_FOLLOW_STYLE received. Resetting follow settings, mouse lock, and player rotation target.");
                break;

            case RESET_POS:
                // 절대 위치 관련 설정 초기화
                getAbsoluteSettings(player).setPosition(Vec3d.ZERO);
                getCommonSettings(player).setPlacementType(EnumPlacement.NONE); // Absolute PlacementType 해제
                // 위치 초기화 시 RotationStyle은 PLAYER_CONTROLLED가 안전함.
                getCommonSettings(player).setRotationStyle(RotationStyle.PLAYER_CONTROLLED);
                getControlComponent(player).setMouseRotationLocked(false); // 위치 초기화 시 마우스 잠금 해제
                getControlComponent(player).setPlayerYaw(0); // KeyboardManager 회전 목표 초기화
                getControlComponent(player).setPlayerPitch(0); // KeyboardManager 회전 목표 초기화
                getCommonSettings(player).setShouldUpdate(true); // 업데이트 필요 알림
                LOGGER.debug("Camera RESET_POS received. Resetting absolute position, placement type, mouse lock, and player rotation target.");
                break;
            case EnumResetType.RESET_OFFSET:
                // 상대 오프셋 관련 설정 초기화
                getRelativeSettings(player).setOffset(0, 0, 0);
                getCommonSettings(player).setPlacementType(EnumPlacement.NONE); // Relative PlacementType 해제
                // 오프셋 초기화 시 RotationStyle은 PLAYER_CONTROLLED가 안전함.
                getCommonSettings(player).setRotationStyle(RotationStyle.PLAYER_CONTROLLED);
                getControlComponent(player).setMouseRotationLocked(false); // 오프셋 초기화 시 마우스 잠금 해제
                getControlComponent(player).setPlayerYaw(0); // KeyboardManager 회전 목표 초기화
                getControlComponent(player).setPlayerPitch(0); // KeyboardManager 회전 목표 초기화
                getCommonSettings(player).setShouldUpdate(true); // 업데이트 필요 알림
                LOGGER.debug("Camera RESET_OFFSET received. Resetting offset, placement type, mouse lock, and player rotation target.");
                break;
            case EnumResetType.RESET_ROTATION:
                // 회전 관련 설정만 초기화 (RotationStyle을 PLAYER_CONTROLLED로)
                resetCameraRotation(getCommonSettings(player)); // 값 초기화 및 스타일 변경 포함
                getControlComponent(player).setMouseRotationLocked(false); // 마우스 회전 잠금 해제
                getControlComponent(player).setPlayerYaw(0); // KeyboardManager 회전 목표 초기화
                getControlComponent(player).setPlayerPitch(0); // KeyboardManager 회전 목표 초기화
                getCommonSettings(player).setShouldUpdate(true); // 회전 변경 시 업데이트 필요 알림
                LOGGER.debug("Camera RESET_ROTATION received. Resetting camera rotation settings, mouse lock, and player rotation target.");
                break;
        }

        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }


    // === CameraFollowPayload를 처리하는 핸들러 ===
    // CameraFollowPayload 핸들러 등록에서 호출됨
    public static void handleFollowStyle(PlayerEntity player, boolean isEnable, EnumMoveStyle moveStyle) {
        CommonSettings commonSettings =  getCommonSettings(player);
        RelativeSettings settings = getRelativeSettings(player);;
        if (isEnable) { // follow 활성화 (forward, sideways, none 포함)
            commonSettings.setPlacementType(EnumPlacement.RELATIVE); // follow는 Relative PlacementType

            // MoveStyle 설정
            settings.setMoveStyle(moveStyle);

            // 플레이어의 현재 Yaw/Pitch 값을 가져옴 (이 값을 카메라 고정 또는 KM 목표로 사용)
            float currentPlayerYaw = player != null ? player.getYaw() : 0;
            float currentPlayerPitch = player != null ? player.getPitch() : 0;


            // 카메라 회전 고정 (FIXED 스타일)
            commonSettings.setRotationStyle(RotationStyle.FIXED);
            // 카메라의 고정 회전 값을 현재 플레이어 시점으로 저장
            commonSettings.setLockYaw(currentPlayerYaw);
            commonSettings.setLockPitch(currentPlayerPitch);
            // Roll 값은 유지하거나 0으로 리셋하거나 선택 (여기서는 유지)
            // commonSettings.setCameraRoll(0);

            // 마우스 잠금 활성화 (플레이어 마우스 입력 막기)
            getControlComponent(player).setMouseRotationLocked(true);
            // KeyboardManager의 플레이어 회전 목표도 현재 플레이어 시점과 일치시킴
            // EntityMixin은 MouseRotationLocked일 때 이 값을 사용하여 플레이어 캐릭터 회전 강제
            getControlComponent(player).setPlayerYaw(currentPlayerYaw);
            getControlComponent(player).setPlayerPitch(currentPlayerPitch);
            // follow 활성화 시 기본 오프셋 설정 (원하는 값으로) 및 초기화 플래그 설정
            // 0, 2, -3 은 플레이어 뒤쪽 약간 위 (follow locked rotation 모드에 적합한 기본 오프셋)
            settings.setOffset(0, 2, -3); // 오프셋 초기값 설정
            settings.setMoveStyleUpdate(true); // Relative 모드 초기화 트리거 (MoveStyle 변경 등)
            commonSettings.setShouldUpdate(true); // CameraMixin.update에서 초기화 트리거 (PlacementType 변경 등)

        } else { // follow 비활성화 (CameraFollowPayload enable=false)
            // 이 부분은 RESET_FOLLOW_STYLE 명령과 동일하게 처리
            settings.setMoveStyle(EnumMoveStyle.ALL);
            commonSettings.setRotationStyle(RotationStyle.PLAYER_CONTROLLED);
            commonSettings.setPlacementType(EnumPlacement.NONE);
            getControlComponent(player).setMouseRotationLocked(false);
            getControlComponent(player).setPlayerYaw(0); // KeyboardManager 회전 목표 초기화
            getControlComponent(player).setPlayerPitch(0); // KeyboardManager 회전 목표 초기화
            settings.setMoveStyleUpdate(true);
            commonSettings.setShouldUpdate(true); // 업데이트 필요
            LOGGER.debug("FOLLOW enable=false received. Resetting follow settings, mouse lock, and player rotation target.");
        }
        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }
    public static void handleLock(PlayerEntity player, boolean isLock, float yaw, float pitch){
        CommonSettings commonSettings = getCameraComponent(player).getCommonSettings();
        if(isLock) {
            commonSettings.setLockYaw(yaw);
            commonSettings.setLockPitch(pitch);

            commonSettings.setRotationStyle(RotationStyle.FIXED);
            getControlComponent(player).setMouseRotationLocked(true);
            getControlComponent(player).setPlayerYaw(commonSettings.getLockYaw());
            getControlComponent(player).setPlayerPitch(commonSettings.getLockPitch());
            LOGGER.debug("Camera LOCK received. Setting Camera RotationStyle to FIXED. Locking mouse and syncing player rotation target to player's current view.");
        }
        else{
            // 카메라 회전 잠금 해제 (PLAYER_CONTROLLED 스타일로 리셋)
            resetCameraRotation(commonSettings); // 값 초기화 및 스타일 변경 포함
            // UNLOCK 시 마우스 잠금 해제
            getControlComponent(player).setMouseRotationLocked(false);
            // KeyboardManager 회전 목표 초기화 (플레이어 마우스 제어 복구)
            getControlComponent(player).setPlayerYaw(0);
            getControlComponent(player).setPlayerPitch(0);
            LOGGER.debug("Camera UNLOCK received. Setting Camera RotationStyle to PLAYER_CONTROLLED. Unlocking mouse and resetting player rotation target.");
        }
        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }
    public static void handleRotate(PlayerEntity player, boolean isAdd, Vector3f vec3f){
        CommonSettings commonSettings = getCameraComponent(player).getCommonSettings();
        if (isAdd) { // bValue = true -> add (값 더하기)
            addLockRotation(commonSettings, vec3f.x, vec3f.y, vec3f.z);
        } else { // bValue = false -> set (값 설정)
            setLockRotation(commonSettings, vec3f.x, vec3f.y, vec3f.z);
        }
        // YAW_PITCH_ROLL 명령은 카메라 회전을 FIXED 스타일로 설정
        commonSettings.setRotationStyle(RotationStyle.FIXED);
        // FIXED 회전 시 마우스 잠금
        getControlComponent(player).setMouseRotationLocked(true);
        // KeyboardManager의 플레이어 회전 목표도 카메라의 고정 목표와 일치시킴
        getControlComponent(player).setPlayerYaw(commonSettings.getLockYaw());
        getControlComponent(player).setPlayerPitch(commonSettings.getLockPitch());
        LOGGER.debug("Camera Yaw/Pitch/Roll set/add. Setting Camera RotationStyle to FIXED. Locking mouse and syncing player rotation target.");
        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }
    public static void handleOffset(PlayerEntity player, boolean isAdd, Vec3d vec3d){
        handleOffsetPos(player, isAdd, vec3d);
        getControlComponent(player).setMouseRotationLocked(false);
        getControlComponent(player).setPlayerYaw(0);
        getControlComponent(player).setPlayerPitch(0);
        LOGGER.info("Offset set. Resetting mouse lock and player rotation target.");
        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }


    public static void handleAbsolute(PlayerEntity player, boolean isAdd, Vec3d vec3d){
        // 절대 위치 설정 명령 처리
        handleAbsolutePosition(player, isAdd, vec3d);
        // 절대 위치 설정 시 마우스 잠금 해제 및 KeyboardManager 회전 목표 초기화
        getControlComponent(player).setMouseRotationLocked(false);
        getControlComponent(player).setPlayerYaw(0);
        getControlComponent(player).setPlayerPitch(0);
        LOGGER.debug("Absolute Position set. Resetting mouse lock and player rotation target.");
        SimpleComponents.getCameraDataKey().sync(player);
        SimpleComponents.getControlDataKey().sync(player);
    }
    // 회전 관련 설정 및 RotationStyle을 PLAYER_CONTROLLED로 리셋
    // RESET_ROTATION 또는 UNLOCK 명령에서 호출됨
    private static void resetCameraRotation(CommonSettings commonSettings) {
        commonSettings.setLockYaw(0);
        commonSettings.setLockPitch(0);
        commonSettings.setCameraRoll(0);
        commonSettings.setRotationStyle(RotationStyle.PLAYER_CONTROLLED);
        LOGGER.debug("Reset Camera Rotation to PLAYER_CONTROLLED and values to 0.");

    }

    // Relative Offset 설정 핸들러 (CameraVec3fPayload 핸들러에서 호출됨)
    private static void handleOffsetPos(PlayerEntity player, boolean isAdd, Vec3d vec3d) {
        CommonSettings commonSettings = getCameraComponent(player).getCommonSettings();
        RelativeSettings settings = getCameraComponent(player).getRelativeSettings();
        commonSettings.setPlacementType(EnumPlacement.RELATIVE); // Offset은 Relative PlacementType

        if (isAdd) { // add
            settings.addOffset(vec3d.x, vec3d.y, vec3d.z);
            LOGGER.info("Added to Relative Offset: {}", vec3d);

        } else { // set
            settings.setOffset(vec3d);
            LOGGER.info("Set Relative Offset: {}", vec3d);
        }
        // Offset 설정 시 RotationStyle은 유지 (Follow 명령 등이 설정)
        commonSettings.setShouldUpdate(true); // 업데이트 필요 알림

    }

    private static void addLockRotation(CommonSettings commonSettings, float yaw, float pitch, float roll) {
        commonSettings.setLockYaw(commonSettings.getLockYaw() + yaw);
        commonSettings.setLockPitch(commonSettings.getLockPitch() + pitch);
        commonSettings.setCameraRoll(commonSettings.getCameraRoll() + roll);
    }

    // YAW_PITCH_ROLL set 명령 처리 (값 설정)
    // 이 메서드는 CameraVec3fPayload 핸들러의 YAW_PITCH_ROLL 케이스에서 호출됨
    private static void setLockRotation(CommonSettings commonSettings, float yaw, float pitch, float roll) {
        commonSettings.setLockYaw(yaw);
        commonSettings.setLockPitch(pitch);
        commonSettings.setCameraRoll(roll);
    }
    // Absolute Position 설정 핸들러 (CameraVec3fPayload 핸들러에서 호출됨)
    private static void handleAbsolutePosition(PlayerEntity player, boolean isAdd, Vec3d vec3d) {
        AbsoluteSettings absoluteSettings = getCameraComponent(player).getAbsoluteSettings();
        CommonSettings commonSettings = getCameraComponent(player).getCommonSettings();
        commonSettings.setPlacementType(EnumPlacement.ABSOLUTE);

        if (isAdd) { // add
            Vec3d currentPosition = absoluteSettings.getPosition();
            Vec3d newPosition = currentPosition.add(vec3d);
            absoluteSettings.setPosition(newPosition);
            LOGGER.debug("Added to Absolute Position: {}", vec3d);

        } else { // set
            absoluteSettings.setPosition(vec3d);
            LOGGER.debug("Set Absolute Position: {}", vec3d);
        }
        // Absolute 위치 설정 시 RotationStyle은 유지 (lock 명령 등으로 별도 설정 필요)
        // commonSettings.setRotationStyle(CameraManager.RotationStyle.PLAYER_CONTROLLED); // PLAYER_CONTROLLED로 강제하지 않음

        commonSettings.setShouldUpdate(true); // 업데이트 필요 알림
        SimpleComponents.getControlDataKey().sync(player);
        SimpleComponents.getCameraDataKey().sync(player);
    }

    private static CameraComponent getCameraComponent(PlayerEntity player) {
        return SimpleComponents.getCameraDataKey().get(player);
    }

    private static ControlSettings getControlComponent(PlayerEntity player) {
        return SimpleComponents.getControlDataKey().get(player).getControlSettings();
    }
    private static CommonSettings getCommonSettings(PlayerEntity player) {
        return getCameraComponent(player).getCommonSettings();
    }

    private static RelativeSettings getRelativeSettings(PlayerEntity player) {
        return getCameraComponent(player).getRelativeSettings();
    }

    private static AbsoluteSettings getAbsoluteSettings(PlayerEntity player) {
        return getCameraComponent(player).getAbsoluteSettings();
    }

    // === 프로필 저장/불러오기 핸들러 추가 ===
    public static boolean savePlayerSettings(PlayerEntity player, String profileName) {
        if (player == null || player.getServer() == null) {
            LOGGER.warn("Attempted to save settings for null player or server.");
            return false;
        }
        Path saveDirectory = player.getServer().getSavePath(WorldSavePath.ROOT).resolve(PROFILE_FOLDER);
        File profileFile = saveDirectory.resolve(profileName + ".nbt").toFile();

        NbtCompound rootNbt = new NbtCompound();
        NbtCompound cameraNbt = new NbtCompound();
        NbtCompound controlNbt = new NbtCompound();

        // 카메라 컴포넌트의 모든 설정을 NBT에 저장
        SimpleComponents.getCameraDataKey().get(player).writeToNbt(cameraNbt, player.getServer().getRegistryManager());
        // 컨트롤 컴포넌트의 모든 설정을 NBT에 저장
        SimpleComponents.getControlDataKey().get(player).writeToNbt(controlNbt, player.getServer().getRegistryManager());

        rootNbt.put("camera", cameraNbt);
        rootNbt.put("control", controlNbt);

        try {
            // 저장 디렉토리 생성
            saveDirectory.toFile().mkdirs();
            // NBT 데이터를 파일에 압축하여 저장
            NbtIo.writeCompressed(rootNbt, profileFile.toPath());
            LOGGER.info("Camera profile '{}' saved for player '{}'.", profileName, player.getName().getString());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save camera profile '{}' for player '{}'.", profileName, player.getName().getString(), e);
            return false;
        }
    }

    public static NbtCompound loadProfileNbt(ServerCommandSource source, String profileName) {
        if (source.getServer() == null) {
            LOGGER.warn("Attempted to load profile on null server.");
            return null;
        }
        Path saveDirectory = source.getServer().getSavePath(WorldSavePath.ROOT).resolve(PROFILE_FOLDER);
        File profileFile = saveDirectory.resolve(profileName + ".nbt").toFile();

        if (!profileFile.exists()) {
            LOGGER.warn("Camera profile '{}' not found.", profileName);
            return null; // 파일 없음
        }

        try (FileInputStream fis = new FileInputStream(profileFile)) {
            // 파일에서 NBT 데이터를 압축 해제하여 읽기
            NbtCompound rootNbt = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
            LOGGER.info("Camera profile '{}' loaded from file.", profileName);
            return rootNbt;
        } catch (IOException e) {
            LOGGER.error("Failed to load camera profile '{}'.", profileName, e);
            return null; // 로드 실패
        }
    }

    public static boolean applyProfileNbt(PlayerEntity player, NbtCompound rootNbt) {
        if (player == null || rootNbt == null) {
            LOGGER.warn("Attempted to apply null profile NBT to player.");
            return false;
        }

        NbtCompound cameraNbt = rootNbt.getCompound("camera");
        NbtCompound controlNbt = rootNbt.getCompound("control");

        if (cameraNbt == null || controlNbt == null) {
            LOGGER.warn("Profile NBT is missing 'camera' or 'control' compound.");
            return false; // 필요한 데이터 없음
        }

        try {
            // 현재 플레이어 컴포넌트에 NBT 데이터 적용
            SimpleComponents.getCameraDataKey().get(player).readFromNbt(cameraNbt, player.getServer().getRegistryManager());
            SimpleComponents.getControlDataKey().get(player).readFromNbt(controlNbt, player.getServer().getRegistryManager());

            // 설정 변경 후 클라이언트로 동기화
            SimpleComponents.getCameraDataKey().sync(player);
            SimpleComponents.getControlDataKey().sync(player);

            LOGGER.info("Camera profile applied to player '{}'.", player.getName().getString());
            return true;
        } catch (Exception e) { // NBT 읽기 중 발생 가능한 오류 포함
            LOGGER.error("Failed to apply camera profile to player '{}'.", player.getName().getString(), e);
            return false;
        }
    }
}
