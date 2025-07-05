package org.land.simplecamera.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;

import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.control.settings.ControlSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ControlSettings controlSettings = SimpleComponents.getControlDataKey().get(MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player : null).getControlSettings();
        if (controlSettings.isDisableMoveSideways()) {
            KeyboardInput input = (KeyboardInput) (Object) this;
            input.movementSideways = 0; // 좌우 이동 취소
        }
        if (controlSettings.isDisableMoveForwards()) {
            KeyboardInput input = (KeyboardInput) (Object) this;
            input.movementForward = 0; // 앞뒤 이동 취소
        }

        if (controlSettings.isDisableAllMove()) {
            KeyboardInput input = (KeyboardInput) (Object) this;
            input.movementSideways = 0; // 좌우 이동 취소
            input.movementForward = 0; // 앞뒤 이동 취소
        }

    }
}
