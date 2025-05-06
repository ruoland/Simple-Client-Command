package org.land.simplecamera.common.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Collections;

public class CommandUtil
{

    public static Collection<ServerPlayerEntity> executeTarget(CommandContext<ServerCommandSource> context){
        Collection<ServerPlayerEntity> targets;

        try {
            targets = EntityArgumentType.getPlayers(context, "targets");
        }catch (CommandSyntaxException e){
            // 타겟이 지정되지 않은 경우 명령어 실행자를 기본 대상으로 설정
            targets = Collections.singleton(context.getSource().getPlayer());

        }
        return targets;

    }
}
