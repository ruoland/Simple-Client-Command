package org.land.simplecamera.feature.cca;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;
import org.land.simplecamera.feature.cca.camera.*;

public interface CameraComponent extends Component, RespawnableComponent<PlayerCameraComponent> {
    CommonSettings getCommonSettings();
    RelativeSettings getRelativeSettings();
    AbsoluteSettings getAbsoluteSettings();

    void setCommonSettings(CommonSettings settings);
    void setRelativeSettings(RelativeSettings settings);
    void setAbsoluteSettings(AbsoluteSettings settings);

    void sync();

}
