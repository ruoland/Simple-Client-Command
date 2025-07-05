package org.land.simplecamera.feature.cca.keybind;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.entity.player.PlayerEntity;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.land.simplecamera.feature.cca.SimpleComponents;
import org.land.simplecamera.feature.cca.keybind.settings.KeybindData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeybindComponentImpl implements AutoSyncedComponent, KeybindComponent {

    private final PlayerEntity provider;
    // Map의 키를 'key name'으로 변경합니다.
    private final Map<String, KeybindData> keybinds = new ConcurrentHashMap<>();

    public KeybindComponentImpl(PlayerEntity player) {
        this.provider = player;
    }
    @Override
    public void sync() {
        // Uses the registered ComponentKey to sync this component
        SimpleComponents.getKeybindDataKey().sync(this.provider);
    }
    @Override
    public KeybindData getKeybindDataByName(String name) {
        return keybinds.get(name.toLowerCase()); // 키 이름은 소문자로 통일하여 검색 (대소문자 구분 없이)
    }

    @Override
    public KeybindData getKeybindDataByKeyCode(int keycode) {

         for(KeybindData data : keybinds.values()) {
            if (data.getKeycode() == keycode) {
                return data;
            }
        }
        return null;
    }

    @Override
    public void setKeybindData(KeybindData data) {
        if (data != null) {
            keybinds.put(data.getName().toLowerCase(), data); // 키 이름은 소문자로 통일하여 저장
        }
    }

    @Override
    public void removeKeybindByName(String name) {
        keybinds.remove(name.toLowerCase());
    }

    @Override
    public Map<String, KeybindData> getAllKeybinds() {
        return new HashMap<>(keybinds);
    }

    @Override
    public void readFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        keybinds.clear();
        NbtList keybindsList = nbt.getList("keybinds", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : keybindsList) {
            if (element instanceof NbtCompound keybindNbt) {
                KeybindData data = new KeybindData(keybindNbt);
                keybinds.put(data.getName().toLowerCase(), data); // Key Name을 키로 사용하여 저장
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList keybindsList = new NbtList();
        for (KeybindData data : keybinds.values()) {
            keybindsList.add(data.writeToNbt());
        }
        nbt.put("keybinds", keybindsList);
    }
}