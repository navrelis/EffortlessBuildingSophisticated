package sophisticated.building.utilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * The server's view of a player's build modifiers (the NBT the client sends with its modifier settings): the settings
 * are capped to the player's limits before the server stores them, and they tell how far the modifiers can move blocks
 * away from the build mode's shape. Works on the NBT only (the modifier classes are client classes).
 */
public final class ModifierLimits {

    public static final String LIST_KEY = "modifierSettingsList";
    // Tag id of a compound (Tag.TAG_COMPOUND from 1.17 on)
    private static final int COMPOUND = 10;

    private ModifierLimits() {
    }

    /** Caps every array to the blocks-per-axis limit and every mirror to the mirror radius limit (in place). */
    public static CompoundTag cap(CompoundTag modifiers, int maxPerAxis, int maxMirrorRadius) {
        ListTag list = modifiers.getList(LIST_KEY, COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag modifier = list.getCompound(i);
            String type = modifier.getString("type");
            if (type.equals("Array")) {
                modifier.putInt("count", BuildLimits.arrayCount(modifier.getInt("count"), largestOffset(modifier), maxPerAxis));
            } else if (type.equals("Mirror") || type.equals("RadialMirror")) {
                modifier.putInt("radius", Math.max(0, Math.min(modifier.getInt("radius"), maxMirrorRadius)));
            }
        }
        return modifiers;
    }

    /** How far (per axis) the enabled modifiers can move a block: an array its extent, a mirror twice its radius. */
    public static int reach(CompoundTag modifiers, int maxPerAxis, int maxMirrorRadius) {
        ListTag list = modifiers.getList(LIST_KEY, COMPOUND);
        int reach = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag modifier = list.getCompound(i);
            if (!modifier.getBoolean("enabled")) continue;
            String type = modifier.getString("type");
            if (type.equals("Array")) {
                int offset = largestOffset(modifier);
                reach += offset * BuildLimits.arrayCount(modifier.getInt("count"), offset, maxPerAxis);
            } else if (type.equals("Mirror") || type.equals("RadialMirror")) {
                reach += 2 * Math.max(0, Math.min(modifier.getInt("radius"), maxMirrorRadius));
            }
        }
        return reach;
    }

    private static int largestOffset(CompoundTag array) {
        int[] offset = array.getIntArray("offset");
        int largest = 0;
        for (int value : offset) largest = Math.max(largest, Math.abs(value));
        return largest;
    }
}
