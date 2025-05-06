package org.land.simplecamera.common.command.argument;

import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PlayerRotationArgument implements PosArgument {
    private final CoordinateArgument yaw;
    private final CoordinateArgument pitch;
    private final CoordinateArgument roll;

    public PlayerRotationArgument(CoordinateArgument yaw, CoordinateArgument pitch, CoordinateArgument roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }
    public float getYaw(ServerPlayerEntity target) {
        return (float) this.yaw.toAbsoluteCoordinate(target.getYaw());
    }

    public float getPitch(ServerPlayerEntity target) {
        return (float) this.pitch.toAbsoluteCoordinate(target.getPitch());
    }

    public float getRoll(ServerPlayerEntity target) {
        // roll은 기본적으로 지원되지 않으므로, 별도 처리 필요
        return (float) this.roll.toAbsoluteCoordinate(0);
    }
    @Override
    public Vec3d getPos(ServerCommandSource source) {
        return new Vec3d(0,0,0);
    }

    @Override
    public Vec2f getRotation(ServerCommandSource source) {
        return new Vec2f(0,0);
    }

    @Override
    public boolean isXRelative() {
        return yaw.isRelative();
    }

    @Override
    public boolean isYRelative() {
        return pitch.isRelative();
    }

    @Override
    public boolean isZRelative() {
        return roll.isRelative();
    }


    // PosArgument 인터페이스의 다른 메서드들도 구현해야 합니다.
    // ...
}
