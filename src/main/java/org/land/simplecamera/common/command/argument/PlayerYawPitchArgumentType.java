package org.land.simplecamera.common.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;

public class PlayerYawPitchArgumentType implements ArgumentType<PlayerYawPitchArgument> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ 0~", "~-5 ~5");
    public static final SimpleCommandExceptionType INCOMPLETE_ROTATION_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player_rotation.incomplete"));

    public PlayerYawPitchArgumentType() {
    }

    public static PlayerYawPitchArgumentType playerYawPitch() {
        return new PlayerYawPitchArgumentType();
    }

    public static PlayerYawPitchArgument getPlayerYawPitch(CommandContext<ServerCommandSource> context, String name) {
        return context.getArgument(name, PlayerYawPitchArgument.class);
    }

    @Override
    public PlayerYawPitchArgument parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        if (!stringReader.canRead()) {
            throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
        } else {
            CoordinateArgument yaw = CoordinateArgument.parse(stringReader, false);
            if (stringReader.canRead() && stringReader.peek() == ' ') {
                stringReader.skip();
                CoordinateArgument pitch = CoordinateArgument.parse(stringReader, false);

                return new PlayerYawPitchArgument(yaw, pitch);

            }
            stringReader.setCursor(i);
            throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

