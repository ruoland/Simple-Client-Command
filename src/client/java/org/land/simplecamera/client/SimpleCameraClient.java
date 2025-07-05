package org.land.simplecamera.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.land.simplecamera.feature.cca.keybind.KeybindComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.keybind.settings.KeybindData;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SimpleCameraClient implements ClientModInitializer {
    private static Map<String, KeyBinding> keyBindings = new HashMap<>();
    @Override
    public void onInitializeClient() {

//        ClientTickEvents.END_WORLD_TICK.register(clientWorld -> {
//            MinecraftClient mc = MinecraftClient.getInstance();
//
//            KeybindComponent keybindComponent  = SimpleComponents.getKeybindDataKey().get(mc.player != null ? mc.player : null);
//
//            for(KeybindData keybindData : keybindComponent.getAllKeybinds().values()){
//                if(keyBindings.containsKey(keybindData.getName()))
//                    continue;
//                KeyBinding keyBinding = new KeyBinding(
//                        keybindData.getName(),
//                        InputUtil.Type.KEYSYM,                   // 키 타입 (KEYSYM은 키보드 키, MOUSE는 마우스 버튼)
//                        GLFW.GLFW_KEY_E,                   // 기본 키 (설정되지 않음). GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_F10 등으로 초기값 지정 가능
//                        "category.simplecamera.custom_keybinds"  // 키 바인딩이 표시될 카테고리 (lang 파일에서 정의해야 합니다.)
//                );
//
//
//
//                addKeyBindingToGameOptions(mc.options, keyBinding);
//                keyBindings.put(keybindData.getName(), keyBinding);
//                KeyBinding[] keyBindings = ArrayUtils.clone(mc.options.allKeys);
//                Arrays.sort(keyBindings);
//            }
//
//
//            keybindComponent.getAllKeybinds();
//        });
    }
    public static void addKeyBindingToGameOptions(GameOptions options, KeyBinding newKeyBinding) {
        try {
            // 1. GameOptions 클래스에서 allKeys 필드를 가져옵니다.
            Field allKeysField = GameOptions.class.getDeclaredField("allKeys");
            // private 필드에 접근할 수 있도록 설정합니다.
            allKeysField.setAccessible(true);
            // 3. 현재 allKeys 배열을 가져옵니다.
            KeyBinding[] currentKeys = (KeyBinding[]) allKeysField.get(options);

            // 4. 새로운 배열을 만듭니다. (새로운 키 바인딩을 추가하여)
            ArrayList<KeyBinding> updatedKeysList = new ArrayList<>(Arrays.asList(currentKeys));
            updatedKeysList.add(newKeyBinding); // 새로운 키 바인딩 추가

            // 5. GameOptions 객체의 allKeys 필드를 새 배열로 설정합니다.
            allKeysField.set(options, updatedKeysList.toArray(new KeyBinding[0]));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Failed to modify GameOptions.allKeys using reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
