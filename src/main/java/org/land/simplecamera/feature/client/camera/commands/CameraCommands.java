package org.land.simplecamera.feature.client.camera.commands;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode; // Brigadier의 LiteralCommandNode import 충돌 방지
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

// import org.land.simplecamera.feature.client.camera.packet.CameraBooleanPayload; // 이제 follow는 CameraFollowPayload 사용

import org.land.simplecamera.feature.client.camera.EnumResetType;

import org.land.simplecamera.feature.client.control.EnumMoveStyle; // EnumMoveStyle import
import org.land.simplecamera.manager.CCACameraManager;
import org.land.simplecamera.manager.Perspective;

import java.io.IOException; // IOException import
import java.nio.file.Files; // Files import
import java.nio.file.Path; // Path import
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument; // static import
import static net.minecraft.server.command.CommandManager.literal; // static import
import static org.land.simplecamera.common.command.CommandUtil.executeTarget;

public class CameraCommands {
    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<ServerCommandSource> cameraNode = dispatcher.register(literal("camera")
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
                                    // === rotate 하위 명령어 ===
                                    // set/add rotate 명령은 이제 특정 회전값으로 '고정'하는 명령으로 간주
                                    .then(literal("rotate") // roll 단일 -> rotate (yaw, pitch, roll 포함 가능성)
                                            .then(literal("reset")
                                                    .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_ROTATION))) // 회전 고정 해제 (플레이어 따라가기)
                                            .then(literal("set") // 특정 yaw, pitch, roll 값으로 회전 고정
                                                    .then(argument("yaw", FloatArgumentType.floatArg())
                                                            .then(argument("pitch", FloatArgumentType.floatArg())
                                                                    .then(argument("roll", FloatArgumentType.floatArg())
                                                                            .executes(CameraCommands::executeCameraSetRotateCommand)))))
                                            .then(literal("add") // 현재 고정된 회전 값에 더함
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
                                    // follow 명령은 Relative PlacementType, 특정 MoveStyle, 그리고 lockRotation에 따라 RotationStyle을 설정
                                    .then(literal("follow")
                                            .then(literal("reset")
                                                    .executes(context -> executeCameraResetCommand(context, EnumResetType.RESET_FOLLOW_STYLE))) // follow 관련 모두 리셋

                                            // follow forward [lockRotation=false]
                                            .then(literal("forward")
                                                    .executes(context -> executeFollowStyle(context, EnumMoveStyle.FORWARD))) // lockRotation 기본값 false (플레이어 회전 따라감)))

                                            // follow sideways [lockRotation=false]
                                            .then(literal("sideways")
                                                    .executes(context -> executeFollowStyle(context, EnumMoveStyle.SIDEWAYS)) // lockRotation 기본값 false
                                                            ))

                                     // follow 그룹 끝
                                    // === profile 하위 명령어 ===
                                    .then(literal("profile")
                                            .then(literal("save")
                                                    .then(argument("profile", StringArgumentType.word())
                                                            .executes(CameraCommands::executeCameraSaveCommand)))
                                            .then(literal("load")
                                                    .then(argument("profile", StringArgumentType.word())

                                                            .executes(CameraCommands::executeCameraLoadCommand))))
                                    // === profile 하위 명령어 ===
                                    .then(literal("profile")
                                            .then(literal("save")
                                                    .then(argument("profile", StringArgumentType.word())
                                                            .executes(CameraCommands::executeCameraSaveCommand)))
                                            .then(literal("load")
                                                    .then(argument("profile", StringArgumentType.word())
                                                            .executes(CameraCommands::executeCameraLoadCommand))))
                                    // === 기타 명령어 ===
                                    // lock 명령은 플레이어의 현재 시점을 기준으로 FIXED RotationStyle을 설정
                                    .then(literal("lock")
                                            // PlayerYawPitchArgumentType은 명령 실행 시점의 플레이어 Yaw/Pitch를 가져옴
                                            .then(argument("rotation", PlayerYawPitchArgumentType.playerYawPitch())
                                                    .suggests((commandContext, suggestionsBuilder) -> (PlayerYawPitchArgumentType.playerYawPitch().listSuggestions(commandContext, suggestionsBuilder)))
                                                    .executes(CameraCommands::executeCameraLockCommand)))
                                    // unlock 명령은 RotationStyle을 PLAYER_CONTROLLED로 리셋
                                    .then(literal("unlock")
                                            .executes(CameraCommands::executeCameraUnlockCommand))
                                    // reset 명령은 모든 설정 초기화
                                    .then(literal("reset")
                                            .executes(ctx -> executeCameraResetCommand(ctx, EnumResetType.RESET_ALL)))

                                    .then(literal("pov")
                                            .then(argument("pov", StringArgumentType.string())
                                                    .suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(new String[]{"FIRST_PERSON", "THIRD_PERSON_BACK", "THIRD_PERSON_FRONT"}, suggestionsBuilder))
                                                    .executes(CameraCommands::executeCameraPOVCommand))))

                    // === 타겟 미지정 시 실행자 대상 ===
            );
        });

    }


    private static int executeHelpCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<Supplier<Text>> helpTexts = Arrays.asList(
                () -> Text.literal("카메라 명령어 도움말:").formatted(Formatting.GOLD, Formatting.BOLD),
                () -> Text.literal("카메라 위치 조정:").formatted(Formatting.YELLOW),
                () -> Text.literal("  /camera <대상> offset set <x> <y> <z>: 카메라의 상대적 위치 오프셋을 설정합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> offset add <x> <y> <z>: 카메라의 현재 오프셋에 값을 더합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> offset reset : 카메라 오프셋을 초기화 합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> pos set <x> <y> <z>: 카메라의 절대적 위치를 설정합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> pos add <x> <y> <z>: 카메라의 현재 절대 위치에 값을 더합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> pos reset 카메라의 절대 위치를 초기화 합니다.").formatted(Formatting.GRAY),
                () -> Text.literal(" "),
                () -> Text.literal("카메라 회전 조정 (고정 회전):").formatted(Formatting.YELLOW),
                // 도움말 업데이트: rotate set/add는 이제 yaw, pitch, roll 모두 포함
                () -> Text.literal("  /camera <대상> rotate set <yaw> <pitch> <roll>: 카메라 회전을 특정 값으로 고정합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> rotate add <yaw> <pitch> <roll>: 카메라 회전 고정 값에 더합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> rotate reset : 카메라 회전 고정 설정을 초기화 합니다 (플레이어 시점 따라가기).").formatted(Formatting.GRAY),
                () -> Text.literal(" "),
                () -> Text.literal("카메라 움직임 설정:").formatted(Formatting.YELLOW),
                // 도움말 업데이트: lockRotation 인자 추가
                () -> Text.literal("  /camera <대상> follow forward: 앞뒤 움직임 따라가기.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> follow sideways: 좌우 움직임 따라가기.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> follow reset : follow 설정을 모두 초기화합니다.").formatted(Formatting.GRAY),
                () -> Text.literal(" "),
                () -> Text.literal("카메라 프로필 관리:").formatted(Formatting.YELLOW),
                () -> Text.literal("  /camera <대상> profile save <이름>: 현재 카메라 설정을 저장합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> profile load <이름>: 저장된 카메라 설정을 불러옵니다.").formatted(Formatting.GRAY),
                () -> Text.literal(" "),
                () -> Text.literal("기타 카메라 설정:").formatted(Formatting.YELLOW),
                // lock 명령 설명 업데이트
                () -> Text.literal("  /camera <대상> lock <yaw> <pitch>: 카메라 회전을 현재 플레이어 시점에 고정합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> unlock: 카메라 회전 고정을 해제하고 플레이어 시점을 따라갑니다.").formatted(Formatting.GRAY), // 설명 업데이트
                () -> Text.literal("  /camera <대상> reset: 카메라 설정을 모두 기본값으로 초기화합니다.").formatted(Formatting.GRAY),
                () -> Text.literal("  /camera <대상> pov <FIRST_PERSON|THIRD_PERSON_BACK|THIRD_PERSON_FRONT>: 카메라 시점을 변경합니다.").formatted(Formatting.GRAY)
        );

        helpTexts.forEach(textSupplier -> source.sendFeedback(textSupplier, false));
        return 1;
    }



    private static int executeCameraPOVCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String povString = StringArgumentType.getString(context, "pov");
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handlePerspective(player, Perspective.valueOf(povString));
        }
        sendFeedback(context, "카메라 시점이 %s으로 변경되었습니다.", povString.toUpperCase());
        return 1;
    }


    private static int executeCameraResetCommand(CommandContext<ServerCommandSource> context, EnumResetType packetType) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleReset(player, packetType);
        }

        // 피드백 메시지 개선
        if(packetType == EnumResetType.RESET_ALL)
            sendFeedback(context, "카메라 위치와 회전 설정이 모두 초기화되었습니다.");
        else if (packetType == EnumResetType.RESET_POS)
            sendFeedback(context, "카메라 위치 설정이 초기화되었습니다.");
        else if (packetType == EnumResetType.RESET_ROTATION)
            sendFeedback(context, "카메라 회전 설정이 초기화되었습니다 (플레이어 시점 따라가기).");
        else if (packetType == EnumResetType.RESET_OFFSET)
            sendFeedback(context, "카메라 오프셋 설정이 초기화되었습니다.");
        else if(packetType == EnumResetType.RESET_FOLLOW_STYLE)
            sendFeedback(context, "카메라 움직임 제한 설정이 초기화되었습니다.");
        return 1;
    }


    // 카메라 잠금 (플레이어의 현재 시점에 고정)
    private static int executeCameraLockCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        PlayerYawPitchArgument rotation = PlayerYawPitchArgumentType.getPlayerYawPitch(context, "rotation"); // Yaw, Pitch 가져옴
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleLock(player, true, rotation.getYaw(player), rotation.getPitch(player));
        }

        sendFeedback(context, "카메라 회전이 현재 시점에 고정되었습니다.");
        return 1;
    }

    // 카메라 회전 잠금 해제 (플레이어 시점 따라가기)
    private static int executeCameraUnlockCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleLock(player, false, 0,0);
        }

        sendFeedback(context, "카메라 회전 고정이 해제 되었습니다 (플레이어 시점 따라 가기).");
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
        sendFeedback(context, "카메라 회전이 다음 값으로 고정 되었습니다: Yaw=%.2f, Pitch=%.2f, Roll=%.2f", yaw, pitch, roll);
        return 1;
    }

    // 카메라 회전 값 추가 (고정 값에 더함)
    private static int executeCameraAddRotateCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        float roll = FloatArgumentType.getFloat(context, "roll");

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleRotate(player, true, new Vector3f(yaw, pitch, roll));
        }

        sendFeedback(context, "카메라 회전 고정 값에 다음 값을 더했습니다: Yaw=%.2f, Pitch=%.2f, Roll=%.2f", yaw, pitch, roll);
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

        sendFeedback(context, "카메라 오프셋이 설정 되었습니다: %s", pos.toString());
        return 1;
    }

    // 카메라 오프셋 추가 (Relative 모드)
    private static int executeCameraAddOffsetCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        float x = FloatArgumentType.getFloat(context, "x");
        float y = FloatArgumentType.getFloat(context, "y");
        float z = FloatArgumentType.getFloat(context, "z");

        Vec3d pos = new Vec3d(x, y, z);
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleOffset(player, true, pos);
        }


        sendFeedback(context, "카메라 오프셋에 값이 추가되었습니다: %s", pos.toString());
        return 1;
    }

    // 카메라 위치 설정 (Absolute 모드)
    private static int executeCameraSetPosCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleAbsolute(player, false, pos);
        }

        sendFeedback(context, "카메라 절대 위치가 설정되었습니다: %s", pos.toString());
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
        sendFeedback(context,
                "카메라 절대 위치에 값이 추가되었습니다:"+ pos.toString());

        return 1;
    }


    // === follow 스타일 명령 처리 (새로운 Payload 사용) ===
    private static int executeFollowStyle(CommandContext<ServerCommandSource> context, EnumMoveStyle moveStyle) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);

        for(ServerPlayerEntity player : targets) {
            CCACameraManager.handleFollowStyle(player, true, moveStyle);
        }

        String moveStyleName = switch (moveStyle) {
            case ALL -> "제한 없음";
            case FORWARD -> "앞뒤";
            case SIDEWAYS -> "좌우";
        };
        sendFeedback(context, "카메라 움직임 따라가기 모드가 %s 방향으로 활성화되었습니다.", moveStyleName);

        return 1;
    }

    // === 프로필 저장 명령어 처리 ===
    private static int executeCameraSaveCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String profileName = StringArgumentType.getString(context, "profile");

        if (targets.isEmpty()) {
            sendError(context, "대상 플레이어를 찾을 수 없습니다.");
            return 0;
        }

        int successCount = 0;
        for(ServerPlayerEntity player : targets) {
            if (CCACameraManager.savePlayerSettings(player, profileName)) {
                sendFeedback(context, "플레이어 '%s'의 카메라 설정이 프로필 '%s'에 저장되었습니다.", player.getName().getString(), profileName);
                successCount++;
            } else {
                sendError(context, "플레이어 '%s'의 카메라 설정 저장 실패!", player.getName().getString());
            }
        }

        return successCount; // 성공적으로 저장된 플레이어 수 반환
    }
    // === 프로필 불러오기 명령어 처리 ===
    private static int executeCameraLoadCommand(CommandContext<ServerCommandSource> context) {
        Collection<ServerPlayerEntity> targets = executeTarget(context);
        String profileName = StringArgumentType.getString(context, "profile");

        if (targets.isEmpty()) {
            sendError(context, "대상 플레이어를 찾을 수 없습니다.");
            return 0;
        }

        // 먼저 프로필 파일을 읽어옵니다.
        NbtCompound profileNbt = CCACameraManager.loadProfileNbt(context.getSource(), profileName);

        if (profileNbt == null) {
            sendError(context, "존재하지 않거나 불러올 수 없는 프로필입니다: '%s'", profileName);
            return 0;
        }

        int successCount = 0;
        // 불러온 NBT 데이터를 각 대상 플레이어에게 적용합니다.
        for(ServerPlayerEntity player : targets) {
            if (CCACameraManager.applyProfileNbt(player, profileNbt)) {
                sendFeedback(context, "플레이어 '%s'에게 프로필 '%s'을 적용했습니다.", player.getName().getString(), profileName);
                successCount++;
            } else {
                sendError(context, "플레이어 '%s'에게 프로필 '%s' 적용 실패!", player.getName().getString(), profileName);
            }
        }

        return successCount; // 성공적으로 적용된 플레이어 수 반환
    }

    // 공통 피드백 메서드
    private static void sendFeedback(CommandContext<ServerCommandSource> context, String messageFormat, Object... args) {
        // toText를 사용하여 String.format 결과를 Text로 변환
        context.getSource().sendFeedback(() -> Text.literal(String.format(messageFormat, args)), true);
    }

    // 공통 에러 메서드
    private static void sendError(CommandContext<ServerCommandSource> context, String messageFormat, Object... args) {
        // toText를 사용하여 String.format 결과를 Text로 변환
        context.getSource().sendError(Text.literal(String.format(messageFormat, args)));
    }
}