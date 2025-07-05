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
                            .then(literal("rotation")
                                    .then(literal("reset")
                                            .executes(ControlCommands::executesRotationReset))
                                    .then(literal("set")
                                            .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                                    .suggests((commandContext, suggestionsBuilder) -> PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesRotationSet)))
                                    .then(literal("add")
                                            .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                                    .suggests((commandContext, suggestionsBuilder) -> PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder))
                                                    .executes(ControlCommands::executesRotationAdd))))
                            .then(literal("mouse")
                                    .then(argument("lock", LockUnlockArgument.lockUnlock())
                                            .suggests((commandContext, suggestionsBuilder) -> LockUnlockArgument.lockUnlock().listSuggestions(commandContext, suggestionsBuilder))
                                            .executes(ControlCommands::executesMouseLock)))
                            .then(literal("move")
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
                                    .then(literal("reset")
                                            .executes(ControlCommands::executesMoveReset))
                            )
                    )
            );
        });
    }

    private static int executeHelpCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<Supplier<Text>> helpTexts = Arrays.asList(
                // 헬프 메시지를 translatable text로 변경
                () -> Text.translatable("command.simplecamera.control.help.title").formatted(Formatting.GOLD, Formatting.BOLD),
                () -> Text.translatable("command.simplecamera.control.help.usage").formatted(Formatting.YELLOW),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.control.help.rotation_title").formatted(Formatting.LIGHT_PURPLE),
                () -> Text.translatable("command.simplecamera.control.help.rotation.set"),
                () -> Text.translatable("command.simplecamera.control.help.rotation.add"),
                () -> Text.translatable("command.simplecamera.control.help.rotation.reset"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.control.help.mouse_title").formatted(Formatting.LIGHT_PURPLE),
                () -> Text.translatable("command.simplecamera.control.help.mouse.lock"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.control.help.move_title").formatted(Formatting.LIGHT_PURPLE),
                () -> Text.translatable("command.simplecamera.control.help.move.all"),
                () -> Text.translatable("command.simplecamera.control.help.move.forward"),
                () -> Text.translatable("command.simplecamera.control.help.move.sideways"),
                () -> Text.translatable("command.simplecamera.control.help.move.reset"),
                Text::empty // 빈 줄
        );

        helpTexts.forEach(textSupplier -> source.sendFeedback(textSupplier, false)); // Supplier에서 Text 객체 가져오기
        return 1;
    }

    // Lock/Unlock 상태를 현지화된 텍스트로 가져오는 헬퍼 메서드
    private static Text getLockUnlockText(boolean lock) {
        return Text.translatable(lock ? "simplecamera.lock" : "simplecamera.unlock");
    }

    private static int executesMoveAllLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.ALL, lock);
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.move.all.feedback.set", getLockUnlockText(lock));
        return 1;
    }

    private static int executesMoveForwardLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.FORWARD, lock);
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.move.forward.feedback.set", getLockUnlockText(lock));
        return 1;
    }

    private static int executesMoveSidewaysLock(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        boolean lock = LockUnlockArgument.getLockState(context, "lock");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.SIDEWAYS, lock);
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.move.sideways.feedback.set", getLockUnlockText(lock));
        return 1;
    }

    // 이동 제한 모두 해제 명령
    private static int executesMoveReset(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handleMoveStyle(player, EnumMoveStyle.ALL, false);
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.move.reset.feedback.success");
        return 1;
    }


    // 플레이어 회전 목표 설정 (마우스 잠금 상태일 때 유효)
    public static int executesRotationSet(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, rotation.getYaw(player), rotation.getPitch(player));
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.rotation.set.feedback.success");
        return 1;
    }

    // 플레이어 회전 목표에 값 추가 (마우스 잠금 상태일 때 유효)
    public static int executesRotationAdd(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation");
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, player.getYaw() +rotation.getYaw(player), player.getPitch() +rotation.getPitch(player));
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.rotation.add.feedback.success");
        return 1;
    }

    // 플레이어 마우스 제어 복구 및 회전 목표 초기화 (control mouse unlock과 동일 효과)
    public static int executesRotationReset(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCAControlManager.handlePlayerRotation(player, player.getYaw(), player.getPitch() );
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.rotation.reset.feedback.success");
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

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.control.mouse.lock.feedback.set", getLockUnlockText(lock));
        return 1;
    }

    // sendFeedback 및 sendError 헬퍼 메서드를 Text.translatable을 사용하도록 수정
    private static void sendFeedback(CommandContext<ServerCommandSource> context, String key, Object... args) {
        // Text.translatable을 사용하여 번역 가능한 Text 컴포넌트 생성
        context.getSource().sendFeedback(() -> Text.translatable(key, args), false);
    }

    private static void sendError(CommandContext<ServerCommandSource> context, String key, Object... args) {
        // Text.translatable을 사용하여 번역 가능한 Text 컴포넌트 생성
        context.getSource().sendError(Text.translatable(key, args));
    }
}