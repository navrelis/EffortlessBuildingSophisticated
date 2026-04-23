package sophisticated.building.utilities;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class MathHelper {

    public static Vec3i with(Vec3i vec, int index, int value) {
        return switch (index) {
            case 0 -> new Vec3i(value, vec.getY(), vec.getZ());
            case 1 -> new Vec3i(vec.getX(), value, vec.getZ());
            case 2 -> new Vec3i(vec.getX(), vec.getY(), value);
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }
    
    public static Vec3 with(Vec3 vec, int index, double value) {
        return switch (index) {
            case 0 -> new Vec3(value, vec.y, vec.z);
            case 1 -> new Vec3(vec.x, value, vec.z);
            case 2 -> new Vec3(vec.x, vec.y, value);
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }

    public static int get(Vec3i vec, int index) {
        return switch (index) {
            case 0 -> vec.getX();
            case 1 -> vec.getY();
            case 2 -> vec.getZ();
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }
    
    public static double get(Vec3 vec, int index) {
        return switch (index) {
            case 0 -> vec.x;
            case 1 -> vec.y;
            case 2 -> vec.z;
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }
}
