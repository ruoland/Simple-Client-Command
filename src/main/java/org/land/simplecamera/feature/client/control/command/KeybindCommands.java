package org.land.simplecamera.feature.client.control.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.land.simplecamera.feature.cca.keybind.settings.KeybindData;
import org.land.simplecamera.manager.CCAKeybindManager;

import java.util.Collection;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class KeybindCommands {
    public static void init(){
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(literal("keybind")
                    // 'set' 명령어로 변경: 새로운 키바인드를 추가하거나 기존 키바인드를 업데이트합니다.
                    .then(literal("set")
                            .then(argument("targets", EntityArgumentType.players())
                                    .then(argument("keyname", StringArgumentType.string()) // 키 이름이 이제 필수 인자
                                            .then(argument("keycode", IntegerArgumentType.integer()) // 물리적인 키코드
                                                    .then(argument("commands", StringArgumentType.greedyString()) // 실행할 명령어
                                                            .executes(KeybindCommands::executesSetKeybind) // executes 메서드 이름도 변경
                                                    )
                                            )
                                    )
                            )
                    )
                    // 기존 바인딩 제거 명령어 추가
                    .then(literal("remove")
                            .then(argument("targets", EntityArgumentType.players())
                                    .then(argument("keyname", StringArgumentType.string()) // 키 이름으로 제거
                                            .executes(KeybindCommands::executesRemoveKeybind)
                                    )
                            )
                    )
                    // 키바인드 목록 보기 명령어 추가 (클라이언트 동기화 이후 유용)
                    .then(literal("list")
                            .then(argument("target", EntityArgumentType.player()) // 특정 플레이어의 키바인드 목록 보기
                                    .executes(KeybindCommands::executesListKeybinds)
                            )
                    )
            );
        });
    }

    private static int executesSetKeybind(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets;
        try {
            targets = EntityArgumentType.getPlayers(context, "targets");
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("§c대상 플레이어를 가져오는 중 오류가 발생했습니다: " + e.getMessage()));
            return 0;
        }

        if (targets.isEmpty()) {
            context.getSource().sendError(Text.literal("§c명령어를 설정할 플레이어를 찾지 못했습니다. 대상(들)을 지정해주세요."));
            return 0;
        }

        String keyname = StringArgumentType.getString(context, "keyname"); // 키 이름이 가장 중요
        int keycode = IntegerArgumentType.getInteger(context, "keycode");
        String commandToExecute = StringArgumentType.getString(context, "commands");

        int successCount = 0;
        for (ServerPlayerEntity targetPlayer : targets) {
            CCAKeybindManager.setKeybind(targetPlayer, keycode, keyname, commandToExecute); // setKeybind 호출
            successCount++;

            if (!targetPlayer.equals(context.getSource().getPlayer())) {
                targetPlayer.sendMessage(Text.literal("§a[키바인드] '" + keyname + "'에 키코드 '" + keycode + "'와 명령어 '" + commandToExecute + "'가 당신에게 설정되었습니다."));
            }
        }

        context.getSource(). sendMessage(
                Text.literal("§a" + successCount + "명의 플레이어에게 키바인드 '" + keyname + "'(키코드: " + keycode + ", 명령어: " + commandToExecute + ")를 설정했습니다.")
        );

        return successCount;
    }

    private static int executesRemoveKeybind(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets;
        try {
            targets = EntityArgumentType.getPlayers(context, "targets");
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("§c대상 플레이어를 가져오는 중 오류가 발생했습니다: " + e.getMessage()));
            return 0;
        }

        if (targets.isEmpty()) {
            context.getSource().sendError(Text.literal("§c명령어를 제거할 플레이어를 찾지 못했습니다. 대상(들)을 지정해주세요."));
            return 0;
        }

        String keyname = StringArgumentType.getString(context, "keyname");

        int successCount = 0;
        for (ServerPlayerEntity targetPlayer : targets) {
            CCAKeybindManager.removeKeybind(targetPlayer, keyname);
            successCount++;

            if (!targetPlayer.equals(context.getSource().getPlayer())) {
                targetPlayer.sendMessage(Text.literal("§c[키바인드] '" + keyname + "' 바인딩이 당신에게서 제거되었습니다."));
            }
        }

        context.getSource().sendMessage(
                Text.literal("§a" + successCount + "명의 플레이어에게 키바인드 '" + keyname + "'를 제거했습니다.")
        );

        return successCount;
    }

    private static int executesListKeybinds(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity targetPlayer;
        try {
            targetPlayer = EntityArgumentType.getPlayer(context, "target");
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("§c대상 플레이어를 찾지 못했습니다: " + e.getMessage()));
            return 0;
        }

        Map<String, KeybindData> keybinds = CCAKeybindManager.getAllKeybinds(targetPlayer);

        if (keybinds.isEmpty()) {
            context.getSource().sendMessage(Text.literal("§e" + targetPlayer.getName().getString() + "에게 설정된 키바인드가 없습니다."));
            return 0;
        }

        context.getSource().sendMessage(Text.literal("§a" + targetPlayer.getName().getString() + "의 키바인드 목록:"));
        for (Map.Entry<String, KeybindData> entry : keybinds.entrySet()) {
            KeybindData data = entry.getValue();
            context.getSource().sendMessage(Text.literal("§7- 이름: §b" + data.getName() + "§7, 키코드: §e" + data.getKeycode() + "§7, 명령어: §9" + data.getCommand()));
        }

        return 1;
    }
}