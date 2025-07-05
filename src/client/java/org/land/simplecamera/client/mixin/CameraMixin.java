package org.land.simplecamera.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.MathHelper;

import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.camera.settings.AbsoluteSettings;
import org.land.simplecamera.feature.cca.camera.settings.CommonSettings;
import org.land.simplecamera.feature.cca.camera.settings.RelativeSettings;
import org.land.simplecamera.feature.client.camera.EnumPlacement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(Camera.class)
public abstract class CameraMixin {
    // private static final Logger LOGGER = LoggerFactory.getLogger(CameraMixin.class); // 로거 인스턴스

    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Shadow public abstract boolean isThirdPerson();


    @Shadow public abstract Vec3d getPos();

    @Shadow protected abstract void setPos(Vec3d pos);

    private static final double SMOOTHING_FACTOR = 0.1;

    private Vec3d lastTargetPos = Vec3d.ZERO;
    private Vec3d initialPlayerPos = Vec3d.ZERO; // Relative 모드 시작 시점 플레이어 위치 기준점
    private Vec3d initialLookVector = Vec3d.ZERO; // Relative 모드 시작 시점 기준 시점 벡터


    @Inject(method = "update", at = @At("TAIL"))
    private void modifyCameraPosition(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (focusedEntity == null) return;
        MinecraftClient mc = MinecraftClient.getInstance(); // MinecraftClient 인스턴스 가져오기
        CommonSettings commonSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getCommonSettings();
        RelativeSettings relativeSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getRelativeSettings();

        // Relative 모드 활성화 상태를 CCA에서 가져옵니다.
        boolean wasRelativeModeActiveInCCA = relativeSettings.isRelativeModeActive();
        // 현재 PlacementType이 Relative인지 확인합니다.
        boolean isCurrentPlacementRelative = commonSettings.getPlacementType() == EnumPlacement.RELATIVE;

        // 믹스인 필드가 초기화되지 않은 상태인지 (주로 재접속 후 첫 틱)
        boolean mixinFieldsUninitialized = initialPlayerPos.equals(Vec3d.ZERO); // initialPlayerPos가 기준점 역할을 하므로, 이것이 ZERO이면 초기화 안된 상태로 간주

        if(commonSettings.getPerspective() != null && !commonSettings.getPerspective().name().equalsIgnoreCase("reset")) {
            Perspective targetPerspective = Perspective.valueOf(commonSettings.getPerspective().name());
            // 현재 게임 시점과 다를 경우에만 설정 (불필요한 이벤트 발생 방지)
            if (mc.options.getPerspective() != targetPerspective) {
                mc.options.setPerspective(targetPerspective);
            }
        }
        if (isCurrentPlacementRelative) {

            // 재접속 복원
            // 새로운 진입 (CCA 상태와 현재 설정 불일치)
            // 서버 명령 트리거
            boolean needsRelativeInitialization = mixinFieldsUninitialized && wasRelativeModeActiveInCCA || !wasRelativeModeActiveInCCA || commonSettings.isShouldUpdate() || relativeSettings.isMoveStyleUpdate(); // Relative 설정 변경 트리거
            if (needsRelativeInitialization) {
                if (mixinFieldsUninitialized && wasRelativeModeActiveInCCA && !relativeSettings.getInitialPositionSnapshot().equals(Vec3d.ZERO)) {
                    initialPlayerPos = relativeSettings.getInitialPositionSnapshot(); // CCA에서 기준 위치 복원
                    initialLookVector = getHorizontalLookVector(relativeSettings.getInitialYawSnapshot(), relativeSettings.getInitialPitchSnapshot()); // CCA에서 기준 시점 복원

                    lastTargetPos = Vec3d.ZERO;
                } else {
                    // === 2. 새로운 Relative 모드 진입 또는 설정 변경으로 인한 재초기화 ===
                    initialPlayerPos = focusedEntity.getPos(); // 현재 플레이어 위치를 새로운 기준 위치로 설정

                    // initialLookVector는 CommonSettings의 lastPlayerYaw/Pitch (follow 명령 시 설정됨)를 우선 사용하고,
                    // 없으면 현재 플레이어 시점을 사용합니다.
                    float initialYaw = commonSettings.getLastPlayerYaw();
                    float initialPitch = commonSettings.getLastPlayerPitch();

                    if (initialYaw != 0.0F || initialPitch != 0.0F) {
                        initialLookVector = getHorizontalLookVector(initialYaw, initialPitch);
                    } else {
                        initialLookVector = getHorizontalLookVector(focusedEntity.getYaw(), focusedEntity.getPitch()); // 현재 플레이어 시점 사용 (fallback)
                    }

                    if (initialLookVector.lengthSquared() < 1.0E-5) {
                        System.err.println("CameraMixin: Initial look vector is still invalid. Falling back to default Z+.");
                        initialLookVector = new Vec3d(0,0,1);
                    }


                    lastTargetPos = Vec3d.ZERO; // 다음 틱에 updateRelativeCamera에서 다시 계산될 예정

                    // === 새로 초기화된 상태를 CCA 스냅샷에 저장 ===
                    relativeSettings.setInitialPositionSnapshot(initialPlayerPos);
                    relativeSettings.setInitialYawSnapshot(initialYaw); // initialLookVector 계산에 사용된 yaw 저장
                    relativeSettings.setInitialPitchSnapshot(initialPitch); // initialLookVector 계산에 사용된 pitch 저장
                    relativeSettings.setRelativeModeActive(true); // Relative 모드 활성 상태 표시

                    // CCA 컴포넌트 동기화 요청 (서버에 변경 사항 저장)
                    SimpleComponents.getCameraDataKey().sync(focusedEntity); // 필요한 경우
                    // LOGGER.debug("CameraMixin: Relative mode state newly initialized and snapshot saved.");
                }

                // 초기화 플래그는 사용 후 해제합니다.
                commonSettings.setShouldUpdate(false);
                relativeSettings.setMoveStyleUpdate(false);
            }

        } else { // 현재 PlacementType이 Relative가 아닌 경우 (NONE 또는 ABSOLUTE)
            // 믹스인 필드 초기화
            initialPlayerPos = Vec3d.ZERO;
            initialLookVector = Vec3d.ZERO;
            lastTargetPos = Vec3d.ZERO;

            if (wasRelativeModeActiveInCCA) {
                relativeSettings.setInitialPositionSnapshot(Vec3d.ZERO);
                relativeSettings.setInitialYawSnapshot(0.0F);
                relativeSettings.setInitialPitchSnapshot(0.0F);
                relativeSettings.setRelativeModeActive(false);
            }
            commonSettings.setShouldUpdate(false);
            relativeSettings.setMoveStyleUpdate(false);
            SimpleComponents.getCameraDataKey().sync(focusedEntity);
        }
        // --- Relative 모드 활성화/비활성화 및 초기화 로직 끝 ---


        // 카메라 현재 시점 업데이트 (다른 계산에 필요할 수 있습니다)
        updateCurrentLookVector(focusedEntity);

        if (!mc.options.getPerspective().isFirstPerson()) {
            updateCameraPlacement(focusedEntity); // 이 안에서 Relative일 경우 updateRelativeCamera 호출
        }
        // Camera Rotation 로직
        updateCameraRotation(focusedEntity);

        // 기타 설정 업데이트
        updateEtc();


    }

    // === updateCameraPlacement 메서드 본문 ===
    private void updateCameraPlacement(Entity focusedEntity){
        CommonSettings commonSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getCommonSettings();
        AbsoluteSettings absoluteSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getAbsoluteSettings();


        switch (commonSettings.getPlacementType()) {
            case ABSOLUTE:
                Vec3d absolutePosition = absoluteSettings.getPosition();
                setPos(absolutePosition.x, absolutePosition.y, absolutePosition.z);
                break;
            case RELATIVE:
                // Relative 모드일 때, Relative 카메라 위치 업데이트 로직 실행
                updateRelativeCamera(focusedEntity);
                break;
            case NONE:

                break;
        }

    }


    private void updateRelativeCamera(Entity focusedEntity) {
        CommonSettings commonSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getCommonSettings();
        RelativeSettings relativeSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getRelativeSettings();

        // Relative 모드가 활성화 상태이고, initialPlayerPos와 initialLookVector가 유효하게 초기화(복원)되었다면
        if (commonSettings.getPlacementType() == EnumPlacement.RELATIVE
                && !initialPlayerPos.equals(Vec3d.ZERO)
                && !initialLookVector.equals(Vec3d.ZERO)) {

            // Relative 모드 시작 시점(initialPlayerPos)을 기준점으로 사용 (카메라 베이스 위치 계산의 출발점)
            Vec3d cameraBasePos = initialPlayerPos;
            // Relative 모드 시작 시점의 기준 시점 벡터(initialLookVector)를 사용 (기본 움직임 필터링 기준)
            Vec3d initialViewBasis = initialLookVector; // 변수 이름을 더 명확하게 변경

            // 플레이어의 현재 위치
            Vec3d currentPos = focusedEntity.getPos();

            // 플레이어의 '카메라 기준 위치'로부터 '현재 플레이어 위치'까지의 총 이동 벡터
            Vec3d totalMovementFromBase = currentPos.subtract(cameraBasePos);

            Vec3d filteredMovementFromBase; // 기준 위치로부터 필터링된 이동 (카메라 베이스에 더해질 값)
            Vec3d offsetRotationBasisVector; // 오프셋 회전에 사용할 기준 벡터

            // 플레이어 움직임 필터링 로직 및 오프셋 회전 기준 설정
            switch (relativeSettings.getMoveStyle()) {
                case ALL:
                    filteredMovementFromBase = totalMovementFromBase;

                    // 오프셋 회전 기준 벡터를 플레이어의 현재 시점 벡터로 설정합니다.
                    offsetRotationBasisVector = focusedEntity.getRotationVector();
                    break;

                case FORWARD:
                    // 총 이동 벡터에서 기준 시점 기준 앞/뒤 성분만 추출합니다.
                    double forwardComponent = totalMovementFromBase.dotProduct(initialViewBasis);
                    filteredMovementFromBase = initialViewBasis.multiply(forwardComponent);
                    // 오프셋 회전 기준은 Relative 모드 시작 시 시점으로 유지
                    offsetRotationBasisVector = initialViewBasis;
                    break;

                case SIDEWAYS:
                    // 총 이동 벡터에서 기준 시점 기준 좌/우 성분만 추출합니다.
                    Vec3d rightVector = new Vec3d(0, 1, 0).crossProduct(initialViewBasis);
                    if (rightVector.lengthSquared() < 1.0E-5) {
                        // System.err.println("updateRelativeCamera: initialViewBasis is vertical. Using default right vector for sideways.");
                        rightVector = new Vec3d(1, 0, 0);
                    } else {
                        rightVector = rightVector.normalize();
                    }
                    double sidewaysComponent = totalMovementFromBase.dotProduct(rightVector);
                    filteredMovementFromBase = rightVector.multiply(sidewaysComponent);
                    // 오프셋 회전 기준은 Relative 모드 시작 시 시점으로 유지
                    offsetRotationBasisVector = initialViewBasis;
                    break;

                default: // 이 default 케이스를 "플레이어 현재 위치 & 현재 시점 기준"으로 만듭니다.
                    filteredMovementFromBase = Vec3d.ZERO;
                    offsetRotationBasisVector = focusedEntity.getRotationVector();;
                    break;
            }

            // 카메라의 기준 위치는 Relative 모드 시작 시점의 '카메라 기준 위치'에
            // 플레이어의 총 이동 중 '필터링된 성분'만을 더한 위치입니다.
            // default 케이스에서는 이게 결국 플레이어의 현재 위치가 됩니다.
            Vec3d adjustedPlayerPos = cameraBasePos.add(filteredMovementFromBase);

            // Relative 오프셋
            Vec3d offset = new Vec3d(relativeSettings.getOffsetX(), relativeSettings.getOffsetY(), relativeSettings.getOffsetZ());
            // 오프셋은 위에서 결정된 기준 벡터(offsetRotationBasisVector)에 따라 회전합니다.
            Vec3d rotatedOffset = rotateOffset(offset, offsetRotationBasisVector); // <-- 여기서 offsetRotationBasisVector 사용

            // 카메라의 최종 목표 위치
            Vec3d targetPos = adjustedPlayerPos.add(rotatedOffset);

            // 목표 위치로 카메라 부드럽게 이동
            smoothCameraMovement(targetPos);

            // 최종 카메라 위치 설정 (smoothCameraMovement에서 계산된 lastTargetPos 사용)
            updateRelativeCameraPosition();

        }
        // Relative 모드가 비활성화 상태이거나 초기화 안됨
        // else 블록은 modifyCameraPosition에 이미 있으므로 여기에 추가할 필요 없음
    }


    private Vec3d getHorizontalLookVector(float yaw, float pitch) {

        float yawRad = yaw * ((float)Math.PI / 180F);
        float x = -MathHelper.sin(yawRad);
        float z = MathHelper.cos(yawRad);
        Vec3d horizontalVec = new Vec3d(x, 0, z);
        if (horizontalVec.lengthSquared() < 1.0E-5) {
            return new Vec3d(0, 0, 1);
        }
        return horizontalVec.normalize();
    }

    private void updateCurrentLookVector(Entity focusedEntity) {
    }

    private Vec3d rotateOffset(Vec3d offset, Vec3d lookVector) {

        Vec3d right = new Vec3d(0, 1, 0).crossProduct(lookVector);
        if (right.lengthSquared() < 1.0E-5) {
            right = new Vec3d(1, 0, 0);
            System.err.println("rotateOffset: Calculated right vector is zero. Using default X+.");
        } else {
            right = right.normalize();
        }

        Vec3d up = lookVector.crossProduct(right);
        if (up.lengthSquared() < 1.0E-5) {
            up = new Vec3d(0, 1, 0);
            System.err.println("rotateOffset: Calculated up vector is zero. Using default Y+.");
        } else {
            up = up.normalize();
        }

        return right.multiply(offset.x)
                .add(up.multiply(offset.y))
                .add(lookVector.multiply(offset.z));
    }

    private void smoothCameraMovement(Vec3d targetPos) {

        if (lastTargetPos.equals(Vec3d.ZERO)) { // lastTargetPos가 초기 상태이면 스무딩 없이 바로 설정
            lastTargetPos = targetPos;
        } else { // 이후에는 스무딩 적용
            lastTargetPos = lastTargetPos.lerp(targetPos, SMOOTHING_FACTOR);
        }
    }

    private void updateRelativeCameraPosition() {
        setPos(lastTargetPos.x, lastTargetPos.y, lastTargetPos.z);
    }

    private void updateCameraRotation(Entity focusedEntity) {
        CommonSettings commonSettings = SimpleComponents.getCameraDataKey().get(focusedEntity).getCommonSettings();

        float targetYaw = 0;
        float targetPitch = 0;

        switch (commonSettings.getRotationStyle()) {
            case PLAYER_CONTROLLED:
                return; // 플레이어가 직접 제어

            case FIXED:
                // FIXED 모드에서는 LockYaw/Pitch 사용 (follow 명령 등이 설정)
                targetYaw = commonSettings.getLockYaw();
                targetPitch = commonSettings.getLockPitch();
                break;

            case FOLLOW_PLAYER:
                // FOLLOW_PLAYER 모드에서는 현재 플레이어 시점 사용
                targetYaw = focusedEntity.getYaw();
                targetPitch = focusedEntity.getPitch();
                break;
        }

        setRotation(targetYaw, targetPitch);
    }

    private void updateEtc(){
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            CommonSettings commonSettings = SimpleComponents.getCameraDataKey().get(mc.player).getCommonSettings();
            if (commonSettings != null) {
                mc.gameRenderer.setRenderHand(commonSettings.isRenderHand());
            }
        }
    }
}