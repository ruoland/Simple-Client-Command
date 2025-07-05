package org.land.simplecamera.feature.cca.camera;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.camera.settings.AbsoluteSettings;
import org.land.simplecamera.feature.cca.camera.settings.CommonSettings;
import org.land.simplecamera.feature.cca.camera.settings.RelativeSettings;

public class PlayerCameraComponent implements AutoSyncedComponent, CameraComponent {

    private final PlayerEntity player;
    // 컴포넌트 데이터
    private CommonSettings commonSettings = new CommonSettings();
    private RelativeSettings relativeSettings = new RelativeSettings();
    private AbsoluteSettings absoluteSettings = new AbsoluteSettings();
    public PlayerCameraComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public CommonSettings getCommonSettings() { return commonSettings; }
    @Override
    public RelativeSettings getRelativeSettings() { return relativeSettings; }
    @Override
    public AbsoluteSettings getAbsoluteSettings() { return absoluteSettings; }

    @Override
    public void setCommonSettings(CommonSettings settings) {
        this.commonSettings = settings;
        sync(); // Sync after changing the whole object
    }
    @Override
    public void setRelativeSettings(RelativeSettings settings) {
        this.relativeSettings = settings;
        sync(); // Sync after changing the whole object
    }
    @Override
    public void setAbsoluteSettings(AbsoluteSettings settings) {
        this.absoluteSettings = settings;
        sync(); // Sync after changing the whole object
    }

    @Override
    public void sync() {
        // Uses the registered ComponentKey to sync this component
        SimpleComponents.getCameraDataKey().sync(this.player);
    }
    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {

        commonSettings.readFromNbt(nbtCompound);
        relativeSettings.readFromNbt(nbtCompound);
        absoluteSettings.readFromNbt(nbtCompound);
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        commonSettings.writeToNbt(nbtCompound);
        relativeSettings.writeToNbt(nbtCompound);
        absoluteSettings.writeToNbt(nbtCompound);
    }


}
