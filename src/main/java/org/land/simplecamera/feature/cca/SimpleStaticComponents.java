package org.land.simplecamera.feature.cca;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.StaticComponentInitializer;

import java.util.Arrays;
import java.util.Collection;

public class SimpleStaticComponents implements StaticComponentInitializer {

    @Override
    public @NotNull Collection<Identifier> getSupportedComponentKeys() {
        return Arrays.asList(SimpleComponents.CAMERA_ID, SimpleComponents.CONTROL_ID);
    }

    @Override
    public void finalizeStaticBootstrap() {

    }

}