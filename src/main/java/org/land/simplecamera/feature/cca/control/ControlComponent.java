package org.land.simplecamera.feature.cca.control;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.entity.RespawnableComponent;
import org.land.simplecamera.feature.cca.control.settings.ControlSettings;

public interface ControlComponent extends Component, RespawnableComponent<PlayerControlComponent> {

    ControlSettings getControlSettings();

    void setControlSettings(ControlSettings settings);

    void sync();
}
