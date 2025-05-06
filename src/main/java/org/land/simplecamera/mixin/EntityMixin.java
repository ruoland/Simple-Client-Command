package org.land.simplecamera.mixin;


import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;


import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.control.ControlSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Vec3d getPos();

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChange(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (((Entity) (Object) this) instanceof PlayerEntity player) {
            ControlSettings controlSettings = SimpleComponents.getControlDataKey().get(player).getControlSettings();
            if (controlSettings.isMouseRotationLocked()) {
                ci.cancel();
                player.setPitch(controlSettings.getPlayerPitch());
                player.setYaw(controlSettings.getPlayerYaw());

            }
        }
    }


}
