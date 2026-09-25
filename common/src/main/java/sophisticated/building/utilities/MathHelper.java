package sophisticated.building.utilities;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class MathHelper {

    public static Vec3i with(Vec3i vec, int index, int value) {
        switch (index) {
            case 0:
                return new Vec3i(value, vec.getY(), vec.getZ());
            case 1:
                return new Vec3i(vec.getX(), value, vec.getZ());
            case 2:
                return new Vec3i(vec.getX(), vec.getY(), value);
            default:
                throw new IllegalArgumentException("Index must be between 0 and 2");
        }
    }
    
    public static Vec3 with(Vec3 vec, int index, double value) {
        switch (index) {
            case 0:
                return new Vec3(value, vec.y, vec.z);
            case 1:
                return new Vec3(vec.x, value, vec.z);
            case 2:
                return new Vec3(vec.x, vec.y, value);
            default:
                throw new IllegalArgumentException("Index must be between 0 and 2");
        }
    }

    public static int get(Vec3i vec, int index) {
        switch (index) {
            case 0:
                return vec.getX();
            case 1:
                return vec.getY();
            case 2:
                return vec.getZ();
            default:
                throw new IllegalArgumentException("Index must be between 0 and 2");
        }
    }
    
    public static double get(Vec3 vec, int index) {
        switch (index) {
            case 0:
                return vec.x;
            case 1:
                return vec.y;
            case 2:
                return vec.z;
            default:
                throw new IllegalArgumentException("Index must be between 0 and 2");
        }
    }
}
