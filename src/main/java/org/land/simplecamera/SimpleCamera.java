package org.land.simplecamera;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import org.land.simplecamera.common.command.argument.LockUnlockArgument;
import org.land.simplecamera.common.command.argument.PlayerRotationArgumentType;
import org.land.simplecamera.common.command.argument.PlayerYawPitchArgumentType;
import org.land.simplecamera.feature.client.camera.commands.CameraCommands;
import org.land.simplecamera.feature.client.control.command.ControlCommands;

public class SimpleCamera implements ModInitializer {

    public static final String MOD_ID = "simplecamera";
    @Override
    public void onInitialize() {
        CameraCommands.register();

        ControlCommands.init();

        etc();
    }

    public void etc(){

        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of(MOD_ID, "player_rotation_args"),
                PlayerRotationArgumentType.class,
                ConstantArgumentSerializer.of(PlayerRotationArgumentType::new)
        );

        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of(MOD_ID, "player_yaw_pitch"),
                PlayerYawPitchArgumentType.class,
                ConstantArgumentSerializer.of(PlayerYawPitchArgumentType::new)
        );
        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of(MOD_ID, "lock"),
                LockUnlockArgument.class,
                ConstantArgumentSerializer.of(LockUnlockArgument::new)
        );

    }

}
