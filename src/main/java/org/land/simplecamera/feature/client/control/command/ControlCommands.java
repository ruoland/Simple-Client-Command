package org.land.simplecamera.feature.client.control.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.land.simplecamera.common.command.argument.LockUnlockArgument;
import org.land.simplecamera.common.command.argument.PlayerYawPitchArgument;
import org.land.simplecamera.common.command.argument.PlayerYawPitchArgumentType;
import org.land.simplecamera.feature.client.control.EnumMoveStyle;

import org.land.simplecamera.manager.CCAControlManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.land.simplecamera.common.command.CommandUtil.executeTarget;

public class ControlCommands {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
             dispatcher.register(literal("control")
                    .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
                    .then(literal("help")
                            .executes(ControlCommands::executeHelpCommand))
                    .then(argument("targets", EntityArgumentType.players())
                            .then(literal("rotation") // 플레이어 회전 제어 (마우스 잠금 상태일 때 유효)
                                    .then(literal("reset") // 플레이어 마우스 제어 복구 (control mouse unlock과 동일 효과)
                                            .executes(ControlCommands::executesRotationReset))
                                    .then(literal("set") // 플레이어 회전 목표값 설정 (KeyboardManager에 저장됨)
                                            .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                                    .suggests((commandContext, suggestionsBuilder) -> PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesRotationSet)))
                                    .then(literal("add") // 플레이어 회전 목표값에 더함 (KeyboardManager에 저장됨)
                                            .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                                    .suggests((commandContext, suggestionsBuilder) -> PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesRotationAdd))))
                            .then(literal("mouse") // 플레이어 마우스 입력 잠금/해제 (카메라 제어 중이면 무시됨)
                                    .then(argument("lock", LockUnlockArgument.lockUnlock())
                                            .suggests((commandContext, suggestionsBuilder) -> LockUnlockArgument.lockUnlock().listSuggestions(commandContext, suggestionsBuilder))
                                            .executes(ControlCommands::executesMouseLock)))
                            .then(literal("move") // 플레이어 이동 제어
                                    .then(literal("all")
                                            .then(argument("lock", LockUnlockArgument.lockUnlock())
                                                    .suggests((commandContext, suggestionsBuilder) -> LockUnlockArgument.lockUnlock().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesMoveAllLock)))
                                    .then(literal("forward")
                                            .then(argument("lock", LockUnlockArgument.lockUnlock())
                                                    .suggests((commandContext, suggestionsBuilder) -> LockUnlockArgument.lockUnlock().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesMoveForwardLock)))
                                    .then(literal("sideways")
                                            .then(argument("lock", LockUnlockArgument.lockUnlock())
                                                    .suggests((commandContext, suggestionsBuilder) -> LockUnlockArgument.lockUnlock().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesMoveSidewaysLock)))
                                    .then(literal("reset") // 이동 제한 모두 해제
                                            .executes(ControlCommands::executesMoveReset))
                            )
                    )
            );
        });
    }

    private static int executeHelpCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<Supplier<Text>> helpTexts = Arrays.asList(
                () -> Text.literal("제어 명령어 도움말:").formatted(Formatting.GOLD, Formatting.BOLD),
                () -> Text.literal("기본 형식: /control <대상> [명령어]").formatted(Formatting.YELLOW),

                () -> Text.literal("회전 제어 (카메라 제어 중에는 무시):").formatted(Formatting.LIGHT_PURPLE), // 설명 추가
                () -> Text.literal("  /control <대상> rotation set <값>: 플레이어가 바라 보는 방향 설정 "),
                () -> Text.literal("  /control <대상> rotation add <값>: 현재 방향 값에 추가"),
                // rotation reset 설명 업데이트
                () -> Text.literal("  /control <대상> rotation reset : 플레이어 마우스 제어 복구 및 회전 목표 초기화"),

                () -> Text.literal("  "),

                // mouse 제어 설명 업데이트
                () -> Text.literal("마우스 제어 (카메라 제어 중에는 무시):").formatted(Formatting.LIGHT_PURPLE),
                () -> Text.literal("  /control <대상> mouse <lock|unlock>: 마우스 입력 잠금/해제"),
                () -> Text.literal("  "),

                () -> Text.literal("이동 제어:").formatted(Formatting.LIGHT_PURPLE), // 이동 제어는 카메라에 의해 직접 override 될 수 있지만, control 자체는 독립적
                () -> Text.literal("  /control <대상> move all <lock|unlock>: 전체 이동 잠금"),
                () -> Text.literal("  /control <대상> move forward <lock|unlock>: 전진 이동 잠금"),
                () -> Text.literal("  /control <대상> move sideways <lock|unlock>: 측면 이동 잠금"),
                () -> Text.literal("  /control <대상> move reset: 모든 이동 제한 해제"),
                () -> Text.literal("  ")
        );

        helpTexts.forEach(textSupplier -> source.sendFeedback(textSupplier, false));
        return 1;
    }


    private static int executesMoveAllLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.ALL, lock);
        }

        sendFeedback(context, "플레이어 전체 이동 %s 상태로 설정", lock ? "잠금" : "해제");
        return 1;
    }

    private static int executesMoveForwardLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.FORWARD, lock);
        }
        sendFeedback(context, "플레이어 전후방 이동 %s 상태로 설정", lock ? "잠금" : "해제");
        return 1;
    }

    private static int executesMoveSidewaysLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.SIDEWAYS, lock);
        }
        sendFeedback(context, "플레이어 좌우 이동 %s 상태로 설정", lock ? "잠금" : "해제");
        return 1;
    }

    // 이동 제한 모두 해제 명령
    private static int executesMoveReset(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.ALL, false);

        }
        sendFeedback(context, "플레이어 이동 제한이 해제되었습니다.");
        return 1;
    }


    // 플레이어 회전 목표 설정 (마우스 잠금 상태일 때 유효)
    public static int executesRotationSet(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, rotation.getYaw(player), rotation.getPitch(player));

        }

        sendFeedback(context, "플레이어 회전 목표가 설정되었습니다.");
        return 1;
    }

    // 플레이어 회전 목표에 값 추가 (마우스 잠금 상태일 때 유효)
    public static int executesRotationAdd(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, player.getYaw() +rotation.getYaw(player), player.getPitch() +rotation.getPitch(player));
        }
        sendFeedback(context, "플레이어 회전 목표에 값이 추가되었습니다.");
        return 1;
    }

    // 플레이어 마우스 제어 복구 및 회전 목표 초기화 (control mouse unlock과 동일 효과)
    public static int executesRotationReset(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, player.getYaw(), player.getPitch() );
        }
        sendFeedback(context, "플레이어 마우스 제어가 복구되었습니다.");
        return 1;
    }

    // 마우스 입력 잠금/해제 (카메라 제어 중이면 무시됨)
    private static int executesMouseLock(CommandContext<ServerCommandSource> context){
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            // 클라이언트에 ControlMouseLockPayload를 보내고, 클라이언트 핸들러에서 카메라 상태를 확인하여 적용
            CCAControlManager.handleMouseLock(player, lock);

        }

        sendFeedback(context, "플레이어 마우스 입력을 %s 상태로 설정했습니다.", lock ? "잠금" : "해제");
        return 1;
    }

    private static void sendFeedback(CommandContext<ServerCommandSource> context, String format, Object... args) {
        context.getSource().sendFeedback(() -> Text.literal(String.format(format, args)), false);
    }


}