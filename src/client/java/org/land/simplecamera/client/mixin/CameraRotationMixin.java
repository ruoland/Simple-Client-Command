package org.land.simplecamera.client.mixin;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.land.simplecamera.feature.cca.SimpleComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(GameRenderer.class)
public abstract class CameraRotationMixin {

    @Shadow public abstract MinecraftClient getClient();

    @Inject(method = "tiltViewWhenHurt", at = @At("RETURN"))
    private void addCustomZRotation(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(SimpleComponents.getCameraDataKey().get(getClient().player).getCommonSettings().getCameraRoll()));
    }

}
