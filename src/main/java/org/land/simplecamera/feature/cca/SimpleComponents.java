package org.land.simplecamera.feature.cca;

import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.land.simplecamera.SimpleCamera;
import org.land.simplecamera.feature.cca.camera.CameraComponent;
import org.land.simplecamera.feature.cca.camera.PlayerCameraComponent;
import org.land.simplecamera.feature.cca.control.ControlComponent;
import org.land.simplecamera.feature.cca.control.PlayerControlComponent;
import org.land.simplecamera.feature.cca.keybind.KeybindComponent;
import org.land.simplecamera.feature.cca.keybind.KeybindComponentImpl;


public class SimpleComponents implements EntityComponentInitializer {
    public static final Identifier CAMERA_ID = Identifier.of(SimpleCamera.MOD_ID, "player_camera");
    public static final Identifier CONTROL_ID = Identifier.of(SimpleCamera.MOD_ID, "player_control");
    public static final Identifier KEYBIND_ID = Identifier.of(SimpleCamera.MOD_ID, "keybind");

    private static ComponentKey<CameraComponent> CAMERA_DATA_KEY;
    private static ComponentKey<ControlComponent> CONTROL_DATA_KEY;
    private static ComponentKey<KeybindComponent> KEY_DATA_KEY;
    public static ComponentKey<CameraComponent> getCameraDataKey() {
        if (CAMERA_DATA_KEY == null) {
            CAMERA_DATA_KEY = ComponentRegistryV3.INSTANCE.getOrCreate(CAMERA_ID, CameraComponent.class);
        }
        return CAMERA_DATA_KEY;
    }

    public static ComponentKey<ControlComponent> getControlDataKey() {
        if (CONTROL_DATA_KEY == null) {
            CONTROL_DATA_KEY = ComponentRegistryV3.INSTANCE.getOrCreate(CONTROL_ID, ControlComponent.class);
        }
        return CONTROL_DATA_KEY;
    }

    public static ComponentKey<KeybindComponent> getKeybindDataKey() {
        if(KEY_DATA_KEY == null){
            KEY_DATA_KEY = ComponentRegistryV3.INSTANCE.getOrCreate(KEYBIND_ID, KeybindComponent.class);
        }
        return KEY_DATA_KEY;
    }
    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(getCameraDataKey(), PlayerCameraComponent::new, RespawnCopyStrategy.ALWAYS_COPY); // 지연 초기화된 키 사용
        registry.registerForPlayers(getControlDataKey(), PlayerControlComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        //registry.registerForPlayers(getKeybindDataKey(), KeybindComponentImpl::new, RespawnCopyStrategy.ALWAYS_COPY); // RespawnCopyStrategy도 설정
    }

}