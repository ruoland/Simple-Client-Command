package org.land.simplecamera.feature.cca;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;
import org.land.simplecamera.feature.cca.control.ControlSettings;

public interface ControlComponent extends Component, RespawnableComponent<PlayerControlComponent> {

    ControlSettings getControlSettings();

    void setControlSettings(ControlSettings settings);

    void sync();
}
