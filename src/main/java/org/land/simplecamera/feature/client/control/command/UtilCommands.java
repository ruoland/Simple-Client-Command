package org.land.simplecamera.feature.client.control.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text; // 채팅 메시지를 보내기 위해 필요

// java.awt 관련 임포트는 클라이언트에서도 사용하지 않는다면 제거하는 것이 좋습니다.
// import java.awt.*;
// import java.awt.datatransfer.Clipboard;
// import java.awt.datatransfer.StringSelection;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class UtilCommands {

    public static void init() {
        System.out.println("[SimpleCamera DEBUG] UtilCommands.init() 호출됨. 명령어 등록 시작."); // 디버깅용 메시지

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            System.out.println("[SimpleCamera DEBUG] CommandRegistrationCallback 이벤트 실행 중."); // 디버깅용 메시지

            dispatcher.register(literal("clip")

                    .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2)) // 기본 권한 (모두 사용 가능)

                    .then(argument("target", EntityArgumentType.player())
                            .then(literal("pos")
                                            .then(argument("yawPitch", BoolArgumentType.bool())
                                                    .executes(UtilCommands::clipPos) // 최종 실행 메서드
                                            )
                                     .executes(context -> {
                                         context.getSource().sendFeedback(() -> Text.literal("Usage: /clip <player> pos <true/false>"), false);
                                        return 1;
                                     })
                            )
                    )
            );
            System.out.println("[SimpleCamera DEBUG] /clip 명령어 등록 완료."); // 디버깅용 메시지
        });
    }

    private static int clipPos(CommandContext<ServerCommandSource> context) {
        System.out.println("[SimpleCamera DEBUG] clipPos 메서드 실행됨."); // 디버깅용 메시지

        PlayerEntity target;
        boolean isYawPitch;

        try {
            // BoolArgumentType.getBool은 NoSuchElementException이 아닌 IllegalArgumentException 등을 던질 수 있으므로
            // 명시적으로 try-catch 블록 안에 넣는 것이 좋습니다.
            isYawPitch = BoolArgumentType.getBool(context, "yawPitch");

            target = EntityArgumentType.getPlayer(context, "target");

            String coordinates = target.getX() + " " + target.getY() + " " + target.getZ() +
                    (isYawPitch ? " " + target.getYaw() + " " + target.getPitch() : "");

            StringSelection stringSelection = new StringSelection(target.getX() + " " + target.getY() + " " + target.getZ() + (isYawPitch ? target.getYaw() +" " + target.getPitch() : ""));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, stringSelection);

            // 서버 콘솔에도 출력하여 로그 확인 용이
            System.out.println("[SimpleCamera INFO] 복사 요청된 좌표: " + coordinates + " (요청자: " + context.getSource().getName() + ")");

        } catch (CommandSyntaxException e) {
            // 명령어 구문 예외 (예: 플레이어를 찾을 수 없음)
            e.printStackTrace(); // 스택 트레이스는 서버 콘솔에 출력
            context.getSource().sendError(Text.literal("명령어 오류: " + e.getMessage())); // 플레이어에게 에러 메시지 전송
            return 1; // 오류 발생 시 1 반환
        } catch (IllegalArgumentException e) {
            // BoolArgumentType.getBool에서 잘못된 인수를 받았을 때 (거의 발생하지 않음, Brigader가 먼저 체크)
            e.printStackTrace();
            context.getSource().sendError(Text.literal("잘못된 인자: " + e.getMessage()));
            return 1;
        } catch (Exception e) {
            // 그 외 예측치 못한 모든 예외
            e.printStackTrace();
            context.getSource().sendError(Text.literal("예상치 못한 오류 발생: " + e.getMessage()));
            return 1;
        }

        return 0; // 성공 시 0 반환
    }
}