package org.land.simplecamera.feature.cca.keybind;

import org.ladysnake.cca.api.v3.component.Component;
import org.land.simplecamera.feature.cca.keybind.settings.KeybindData;

import java.util.Map;

public interface KeybindComponent extends Component {

    KeybindData getKeybindDataByName(String name);

    KeybindData getKeybindDataByKeyCode(int keycode);

    // KeybindData 객체를 추가하거나 업데이트합니다. (키 이름이 고유 식별자)
    void setKeybindData(KeybindData data);

    // 특정 키 이름의 바인딩을 제거합니다.
    void removeKeybindByName(String name);

    // 모든 키 바인드를 Map (name -> KeybindData) 형태로 가져옵니다.
    Map<String, KeybindData> getAllKeybinds();
    void sync();
}