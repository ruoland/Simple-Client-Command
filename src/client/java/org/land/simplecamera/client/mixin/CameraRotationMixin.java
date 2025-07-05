package org.land.simplecamera.client.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4fStack;
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

    @Inject(method = "render", at = @At("RETURN"))
    private void setFirstPerson(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();

    }
}
