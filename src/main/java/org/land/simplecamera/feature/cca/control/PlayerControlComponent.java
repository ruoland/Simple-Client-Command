package org.land.simplecamera.feature.cca.control;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.control.settings.ControlSettings;

public class PlayerControlComponent implements AutoSyncedComponent, ControlComponent{

    private ControlSettings settings = new ControlSettings();
    private final PlayerEntity player;
    public PlayerControlComponent(PlayerEntity player) {

        this.player = player;
    }
    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {

        settings.readFromNbt(nbtCompound);
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {

        settings.writeToNbt(nbtCompound);
    }

    @Override
    public ControlSettings getControlSettings() {
        return settings;
    }

    @Override
    public void setControlSettings(ControlSettings settings) {

        this.settings = settings;
        sync();
    }

    @Override
    public void sync() {
        SimpleComponents.getControlDataKey().sync(this.player);
    }
}
