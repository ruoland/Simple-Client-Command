// src/main/java/org/land/simplecamera/feature/client/control/command/CCAKeybindManager.java
package org.land.simplecamera.manager;

import net.minecraft.server.network.ServerPlayerEntity;
import org.land.simplecamera.feature.cca.keybind.KeybindComponent;

import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.keybind.settings.KeybindData;

import java.util.Map;

public class CCAKeybindManager {

    public static void setKeybind(ServerPlayerEntity player, int keycode, String name, String command) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        KeybindData data = new KeybindData(keycode, name, command);
        keybindComponent.setKeybindData(data);
    }

    public static String getCommandByName(ServerPlayerEntity player, String name) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        KeybindData data = keybindComponent.getKeybindDataByName(name);
        return data != null ? data.getCommand() : null;
    }

    // 특정 키 이름에 해당하는 전체 KeybindData를 가져옵니다.
    public static KeybindData getKeybindDataByName(ServerPlayerEntity player, String name) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        return keybindComponent.getKeybindDataByName(name);
    }

    // 특정 키코드에 바인딩된 command를 가져옵니다. (클라이언트에서 키 눌림 감지 시 사용)
    public static String getCommandByKeyCode(ServerPlayerEntity player, int keycode) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        KeybindData data = keybindComponent.getKeybindDataByKeyCode(keycode);
        return data != null ? data.getCommand() : null;
    }

    // 특정 키 이름의 바인딩을 제거합니다.
    public static void removeKeybind(ServerPlayerEntity player, String name) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        keybindComponent.removeKeybindByName(name);
    }

    public static Map<String, KeybindData> getAllKeybinds(ServerPlayerEntity player) {
        KeybindComponent keybindComponent = SimpleComponents.getKeybindDataKey().get(player);
        return keybindComponent.getAllKeybinds();
    }
}