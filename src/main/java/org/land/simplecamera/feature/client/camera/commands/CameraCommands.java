package org.land.simplecamera.feature.client.camera.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import org.land.simplecamera.common.command.argument.PlayerYawPitchArgument;
import org.land.simplecamera.common.command.argument.PlayerYawPitchArgumentType;


import org.land.simplecamera.feature.client.camera.EnumResetType;

import org.land.simplecamera.feature.client.control.EnumMoveStyle;
import org.land.simplecamera.manager.CCACameraManager;
import org.land.simplecamera.manager.Perspective;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.land.simplecamera.common.command.CommandUtil.executeTarget;

public class CameraCommands {
    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("camera")
                    .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
                    .then(literal("help")
                            .executes(CameraCommands::executeHelpCommand))
                    .then(argument("targets", EntityArgumentType.players())
                            // === offset 하위 명령어 ===
                            .then(literal("offset")
                                    .then(literal("reset")
                                            .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_OFFSET)))
                                    .then(literal("set")
                                            .then(argument("x", FloatArgumentType.floatArg())
                                                    .then(argument("y", FloatArgumentType.floatArg())
                                                            .then(argument("z", FloatArgumentType.floatArg())
                                                                    .executes(CameraCommands::executeCameraSetOffsetCommand)))))
                                    .then(literal("add")
                                            .then(argument("x", FloatArgumentType.floatArg())
                                                    .then(argument("y", FloatArgumentType.floatArg())
                                                            .then(argument("z", FloatArgumentType.floatArg())
                                                                    .executes(CameraCommands::executeCameraAddOffsetCommand))))))
                            .then(literal("rotate")
                                    .then(literal("reset")
                                            .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_ROTATION)))
                                    .then(literal("set")
                                            .then(argument("yaw", FloatArgumentType.floatArg())
                                                    .then(argument("pitch", FloatArgumentType.floatArg())
                                                            .then(argument("roll", FloatArgumentType.floatArg())
                                                                    .executes(CameraCommands::executeCameraSetRotateCommand)))))
                                    .then(literal("add")
                                            .then(argument("yaw", FloatArgumentType.floatArg())
                                                    .then(argument("pitch", FloatArgumentType.floatArg())
                                                            .then(argument("roll", FloatArgumentType.floatArg())
                                                                    .executes(CameraCommands::executeCameraAddRotateCommand))))))

                            // === pos 하위 명령어 ===
                            .then(literal("pos")
                                    .then(literal("reset")
                                            .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_POS)))
                                    .then(literal("set")
                                            .then(argument("pos", Vec3ArgumentType.vec3())
                                                    .executes(CameraCommands::executeCameraSetPosCommand)))
                                    .then(literal("add")
                                            .then(argument("x", FloatArgumentType.floatArg())
                                                    .then(argument("y", FloatArgumentType.floatArg())
                                                            .then(argument("z", FloatArgumentType.floatArg())
                                                                    .executes(CameraCommands::executeCameraAddPosCommand))))))
                            // === follow 하위 명령어 ===
                            .then(literal("follow")
                                    .then(literal("reset")
                                            .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_FOLLOW_STYLE)))
                                    .then(literal("forward")
                                            .executes(context -> executeFollowStyle(context, EnumMoveStyle.FORWARD)))
                                    .then(literal("sideways")
                                            .executes(context -> executeFollowStyle(context, EnumMoveStyle.SIDEWAYS))))

                            // === profile 하위 명령어 ===
                            .then(literal("profile")
                                    .then(literal("save")
                                            .then(argument("profile", StringArgumentType.word())
                                                    .executes(CameraCommands::executeCameraSaveCommand)))
                                    .then(literal("load")
                                            .then(argument("profile", StringArgumentType.word())
                                                    .executes(CameraCommands::executeCameraLoadCommand))))
                            // === 기타 명령어 ===
                            .then(literal("lock")
                                    .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                            .suggests((commandContext, suggestionsBuilder) -> (PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder)))
                                            .executes(CameraCommands::executeCameraLockCommand)))
                            .then(literal("unlock")
                                    .executes(CameraCommands::executeCameraUnlockCommand))
                            .then(literal("reset")
                                    .executes(ctx -> executeCameraResetCommand(ctx, EnumResetType.RESET_ALL)))

                            .then(literal("pov")
                                    .then(argument("pov", StringArgumentType.string())
                                            .suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"FIRST_PERSON", "THIRD_PERSON_BACK", "THIRD_PERSON_FRONT", "reset"}, suggestionsBuilder))
                                            .executes(CameraCommands::executeCameraPOVCommand))))
            );
        });
    }

    private static int executeHelpCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<Supplier<Text>> helpTexts = Arrays.asList(
                // 헬프 메시지를 translatable text로 변경 (서식 코드도 언어 파일에 포함)
                () -> Text.translatable("command.simplecamera.camera.help.title"),
                () -> Text.translatable("command.simplecamera.camera.help.pos_adjust_title"),
                () -> Text.translatable("command.simplecamera.camera.help.offset.set"),
                () -> Text.translatable("command.simplecamera.camera.help.offset.add"),
                () -> Text.translatable("command.simplecamera.camera.help.offset.reset"),
                () -> Text.translatable("command.simplecamera.camera.help.pos.set"),
                () -> Text.translatable("command.simplecamera.camera.help.pos.add"),
                () -> Text.translatable("command.simplecamera.camera.help.pos.reset"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.camera.help.rotation_adjust_title"),
                () -> Text.translatable("command.simplecamera.camera.help.rotate.set"),
                () -> Text.translatable("command.simplecamera.camera.help.rotate.add"),
                () -> Text.translatable("command.simplecamera.camera.help.rotate.reset"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.camera.help.movement_title"),
                () -> Text.translatable("command.simplecamera.camera.help.follow.forward"),
                () -> Text.translatable("command.simplecamera.camera.help.follow.sideways"),
                () -> Text.translatable("command.simplecamera.camera.help.follow.reset"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.camera.help.profile_title"),
                () -> Text.translatable("command.simplecamera.camera.help.profile.save"),
                () -> Text.translatable("command.simplecamera.camera.help.profile.load"),
                Text::empty, // 빈 줄
                () -> Text.translatable("command.simplecamera.camera.help.misc_title"),
                () -> Text.translatable("command.simplecamera.camera.help.lock"),
                () -> Text.translatable("command.simplecamera.camera.help.unlock"),
                () -> Text.translatable("command.simplecamera.camera.help.reset"),
                () -> Text.translatable("command.simplecamera.camera.help.pov")
        );

        helpTexts.forEach(textSupplier -> source.sendFeedback(textSupplier, false)); // Supplier에서 Text 객체 가져오기
        return 1;
    }


    private static int executeCameraPOVCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String povString = StringArgumentType.getString(context, "pov");
        // "reset"은 Perspective enum에 없으므로 특별 처리 필요
        if (povString.equalsIgnoreCase("reset")) {
            for(ServerPlayerEntity player : targets) {
                // reset logic (default perspective)
                CCACameraManager.handlePerspective(player, Perspective.FIRST_PERSON); // 또는 기본 시점으로 리셋하는 다른 로직
            }
            sendFeedback(context, "command.simplecamera.camera.pov.feedback.set", "기본"); // "기본"으로 리셋됨 메시지
        } else {
            try {
                Perspective perspective = Perspective.valueOf(povString.toUpperCase());
                for(ServerPlayerEntity player : targets) {
                    CCACameraManager.handlePerspective(player, perspective);
                }
                // 피드백 메시지를 translatable text로 변경
                sendFeedback(context, "command.simplecamera.camera.pov.feedback.set", povString.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 시점 이름일 경우 에러 메시지
                sendError(context, "유효하지 않은 시점 이름입니다: %s. 사용 가능한 시점: FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT, reset", povString); // 이 에러 메시지도 언어 파일에 추가하면 좋음
                return 0; // 에러 발생 시 0 반환
            }
        }
        return 1; // 성공 시 1 반환
    }


    private static int executeCameraResetCommand(CommandContext<ServerCommandSource> context, EnumResetType resetType) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleReset(player, resetType);
        }

        // 피드백 메시지를 translatable text로 변경 (resetType에 따라 다른 키 사용)
        switch (resetType) {
            case RESET_ALL:
                sendFeedback(context, "command.simplecamera.camera.reset.feedback.all");
                break;
            case RESET_POS:
                sendFeedback(context, "command.simplecamera.camera.reset.feedback.pos");
                break;
            case RESET_ROTATION:
                sendFeedback(context, "command.simplecamera.camera.reset.feedback.rotation");
                break;
            case RESET_OFFSET:
                sendFeedback(context, "command.simplecamera.camera.reset.feedback.offset");
                break;
            case RESET_FOLLOW_STYLE:
                sendFeedback(context, "command.simplecamera.camera.reset.feedback.follow_style");
                break;
        }
        return 1;
    }


    // 카메라 잠금 (플레이어의 현재 시점에 고정)
    private static int executeCameraLockCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        // PlayerYawPitchArgumentType은 이미 명령 실행 시점의 플레이어 Yaw/Pitch를 가져와서 반환함
        // argument("rotation", PlayerYawPitchArgumentType.playerYawPitch()) 에서 PlayerYawPitchArgument 객체로 파싱됨
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation");

        for(ServerPlayerEntity player : targets) {
            float yaw = rotation.getYaw(player); // 플레이어의 현재 yaw
            float pitch = rotation.getPitch(player); // 플레이어의 현재 pitch

            CCACameraManager.handleLock(player, true, yaw, pitch); // 고정할 yaw, pitch 값을 넘김
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.lock.feedback.success");
        return 1;
    }

    // 카메라 회전 잠금 해제 (플레이어 시점 따라가기)
    private static int executeCameraUnlockCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            // unlock은 고정된 회전값을 무시하고 플레이어 회전을 따라가게 함
            CCACameraManager.handleLock(player, false, 0,0); // lock = false 로 설정
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.unlock.feedback.success");
        return 1;
    }


    // 카메라 회전 설정 (특정 값으로 고정)
    private static int executeCameraSetRotateCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        float roll = FloatArgumentType.getFloat(context, "roll");

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleRotate(player, false, new Vector3f(yaw, pitch, roll));
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.rotate.set.feedback.success", yaw, pitch, roll);
        return 1;
    }

    private static int executeCameraAddRotateCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        float roll = FloatArgumentType.getFloat(context, "roll");

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleRotate(player, true, new Vector3f(yaw, pitch, roll));
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.rotate.add.feedback.success", yaw, pitch, roll);
        return 1;
    }

    // 카메라 오프셋 설정 (Relative 모드)
    private static int executeCameraSetOffsetCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float x = FloatArgumentType.getFloat(context, "x");
        float y = FloatArgumentType.getFloat(context, "y");
        float z = FloatArgumentType.getFloat(context, "z");

        Vec3d pos = new Vec3d(x, y, z);
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleOffset(player, false, pos);
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.offset.set.feedback.success", pos.toString()); // Vec3d의 toString 사용
        return 1;
    }

    // 카메라 오프셋 추가 (Relative 모드)
    private static int executeCameraAddOffsetCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float x = FloatArgumentType.getFloat(context, "x");
        float y = FloatArgumentType.getFloat(context, "y");
        float z = FloatArgumentType.getFloat(context, "z");

        Vec3d pos = new Vec3d(x, y, z); // 추가할 값
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleOffset(player, true, pos);
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.offset.add.feedback.success", pos.toString()); // Vec3d의 toString 사용

        return 1;
    }

    // 카메라 위치 설정 (Absolute 모드)
    private static int executeCameraSetPosCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleAbsolute(player, false, pos);
        }

        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.pos.set.feedback.success", pos.toString()); // Vec3d의 toString 사용
        return 1;
    }

    // 카메라 위치 추가 (Absolute 모드)
    private static int executeCameraAddPosCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float x = FloatArgumentType.getFloat(context, "x");
        float y = FloatArgumentType.getFloat(context, "y");
        float z = FloatArgumentType.getFloat(context, "z");
        Vec3d pos = new Vec3d(x, y, z); // 추가할 값

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleAbsolute(player, true, pos);
        }
        // 피드백 메시지를 translatable text로 변경
        sendFeedback(context, "command.simplecamera.camera.pos.add.feedback.success", pos.toString()); // Vec3d의 toString 사용

        return 1;
    }


    // === follow 스타일 명령 처리 (새로운 Payload 사용) ===
    private static int executeFollowStyle(CommandContext<ServerCommandSource> context, EnumMoveStyle moveStyle) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleFollowStyle(player, true, moveStyle);
        }

        // EnumMoveStyle에 해당하는 현지화된 텍스트를 가져옴
        Text moveStyleText = switch (moveStyle) {
            case ALL -> Text.translatable("enum.simplecamera.movestyle.all");
            case FORWARD -> Text.translatable("enum.simplecamera.movestyle.forward");
            case SIDEWAYS -> Text.translatable("enum.simplecamera.movestyle.sideways");
        };
        sendFeedback(context, "command.simplecamera.camera.follow.feedback.success", moveStyleText);

        return 1;
    }

    // === 프로필 저장 명령어 처리 ===
    private static int executeCameraSaveCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String profileName = StringArgumentType.getString(context, "profile");

        if (targets.isEmpty()) {
            // 에러 메시지를 translatable text로 변경
            sendError(context, "command.simplecamera.error.no_targets");
            return 0;
        }

        int successCount = 0;
        for(ServerPlayerEntity player : targets) {
            if (CCACameraManager.savePlayerSettings(player, profileName)) {
                // 피드백 메시지를 translatable text로 변경
                sendFeedback(context, "command.simplecamera.camera.profile.save.feedback.success", player.getName().getString(), profileName);
                successCount++;
            } else {
                // 에러 메시지를 translatable text로 변경
                sendError(context, "command.simplecamera.camera.profile.save.error.failed", player.getName().getString());
            }
        }

        return successCount; // 성공적으로 저장된 플레이어 수 반환
    }
    // === 프로필 불러오기 명령어 처리 ===
    private static int executeCameraLoadCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String profileName = StringArgumentType.getString(context, "profile");

        if (targets.isEmpty()) {
            // 에러 메시지를 translatable text로 변경
            sendError(context, "command.simplecamera.error.no_targets");
            return 0;
        }

        // 먼저 프로필 파일을 읽어옵니다.
        NbtCompound profileNbt = CCACameraManager.loadProfileNbt(context.getSource(), profileName);

        if (profileNbt == null) {
            // 에러 메시지를 translatable text로 변경
            sendError(context, "command.simplecamera.camera.profile.load.error.not_found", profileName);
            return 0;
        }

        int successCount = 0;
        // 불러온 NBT 데이터를 각 대상 플레이어에게 적용합니다.
        for(ServerPlayerEntity player : targets) {
            if (CCACameraManager.applyProfileNbt(player, profileNbt)) {
                // 피드백 메시지를 translatable text로 변경
                sendFeedback(context, "command.simplecamera.camera.profile.load.feedback.success", player.getName().getString(), profileName);
                successCount++;
            } else {
                // 에러 메시지를 translatable text로 변경
                sendError(context, "command.simplecamera.camera.profile.load.error.failed", player.getName().getString(), profileName);
            }
        }

        return successCount; // 성공적으로 적용된 플레이어 수 반환
    }

    // sendFeedback 및 sendError 헬퍼 메서드를 Text.translatable을 사용하도록 수정
    private static void sendFeedback(CommandContext<ServerCommandSource> context, String key, Object... args) {
        // Text.translatable을 사용하여 번역 가능한 Text 컴포넌트 생성
        context.getSource().sendFeedback(() -> Text.translatable(key, args), true);
    }

    private static void sendError(CommandContext<ServerCommandSource> context, String key, Object... args) {
        // Text.translatable을 사용하여 번역 가능한 Text 컴포넌트 생성
        context.getSource().sendError(Text.translatable(key, args));
    }
}