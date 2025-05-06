
package org.land.simplecamera.common.command.argument;

import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class PlayerYawPitchArgument implements PosArgument {
    private final CoordinateArgument yaw;
    private final CoordinateArgument pitch;


    public PlayerYawPitchArgument(CoordinateArgument yaw, CoordinateArgument pitch) {
        this.yaw = yaw;
        this.pitch = pitch;

    }
    public float getYaw(ServerPlayerEntity target) {
        return (float) this.yaw.toAbsoluteCoordinate(target.getYaw());
    }

    public float getPitch(ServerPlayerEntity target) {
        return (float) this.pitch.toAbsoluteCoordinate(target.getPitch());
    }

    public float getYaw(){
        return (float) this.yaw.toAbsoluteCoordinate(0);
    }

    public float getPitch(){
        return (float) this.pitch.toAbsoluteCoordinate(0);
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
        return false;
    }

}
