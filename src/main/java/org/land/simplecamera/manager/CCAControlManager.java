package org.land.simplecamera.manager;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.land.simplecamera.feature.cca.ControlComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.control.ControlSettings;
import org.land.simplecamera.feature.client.control.EnumMoveStyle;

public class CCAControlManager {
    public static void handleMouseLock(PlayerEntity player, boolean lock) {
        getControlSettings(player).setMouseRotationLocked(lock);
        getControlSettings(player).setPlayerYawAndPitch(player.getYaw(), player.getPitch());
        getControlComponent(player).sync();
    }

    public static void handleMoveStyle(PlayerEntity player, EnumMoveStyle moveStyle, boolean isLock) {


        switch (moveStyle) {
            case ALL -> getControlSettings(player).setDisableAllMove(isLock);
            case FORWARD -> getControlSettings(player).setDisableMoveForwards(isLock);
            case SIDEWAYS -> getControlSettings(player).setDisableMoveSideways(isLock);
        }

        getControlComponent(player).sync();
    }
    public static void handlePlayerRotation(PlayerEntity player, float yaw, float pitch) {
        getControlSettings(player).setPlayerYawAndPitch(yaw, pitch);
        getControlComponent(player).sync();

    }

    private static ControlSettings getControlSettings(PlayerEntity player) {
        return getControlComponent(player).getControlSettings();
    }
    private static @NotNull ControlComponent getControlComponent(PlayerEntity player) {
        return SimpleComponents.getControlDataKey().get(player);
    }
}
