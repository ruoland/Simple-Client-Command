package org.land.simplecamera.feature.cca.camera;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

public class AbsoluteSettings {
    private Vec3d position = new Vec3d(0, 0, 0);

    // Default constructor needed for NBT/Gson
    public AbsoluteSettings() {}

    // Getters
    public Vec3d getPosition() { return position; }

    // Setters (Component will call these and then sync)
    public void setPosition(Vec3d absolutePosition) { this.position = absolutePosition; }

    // NBT Methods for persistence and sync
    public void writeToNbt(NbtCompound nbt) {
        nbt.putDouble("x", this.position.x);
        nbt.putDouble("y", this.position.y);
        nbt.putDouble("z", this.position.z);
    }

    public void readFromNbt(NbtCompound nbt) {
         // Doubles default to 0.0 if missing
        double x = nbt.getDouble("x");
        double y = nbt.getDouble("y");
        double z = nbt.getDouble("z");
        this.position = new Vec3d(x, y, z);
    }
}