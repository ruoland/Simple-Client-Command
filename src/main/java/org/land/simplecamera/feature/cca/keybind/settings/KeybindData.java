// src/main/java/org/land/simplecamera/feature/cca/KeybindData.java (새로운 파일)
package org.land.simplecamera.feature.cca.keybind.settings;

import net.minecraft.nbt.NbtCompound;

public class KeybindData {
    private int keycode;
    private String name;    // 예: "점프", "스크린샷", "사용자 정의 바인딩 1" (사용자가 설정한 이름)
    private String command; // 예: "/help 1", "/gamemode creative"

    public KeybindData(int keycode, String name, String command) {
        this.keycode = keycode;
        this.name = name;
        this.command = command;
    }

    // NBT에서 읽어오기 위한 생성자
    public KeybindData(NbtCompound nbt) {
        this.keycode = nbt.getInt("keycode");
        this.name = nbt.getString("name");
        this.command = nbt.getString("command");
    }

    // NBT로 쓰기 위한 메서드
    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("keycode", keycode);
        nbt.putString("name", name);
        nbt.putString("command", command);
        return nbt;
    }

    // Getter 메서드
    public int getKeycode() { return keycode; }
    public String getName() { return name; }
    public String getCommand() { return command; }

    // Setter 메서드 (필요하다면)
    public void setKeycode(int keycode) { this.keycode = keycode; }
    public void setName(String name) { this.name = name; }
    public void setCommand(String command) { this.command = command; }

    @Override
    public String toString() {
        return "KeybindData{" +
               "keycode='" + keycode + '\'' +
               ", name='" + name + '\'' +
               ", command='" + command + '\'' +
               '}';
    }
}