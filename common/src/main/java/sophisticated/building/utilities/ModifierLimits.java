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

    private ModifierLimits() {
    }

    /** Caps every array to the blocks-per-axis limit and every mirror to the mirror radius limit (in place). */
    public static CompoundTag cap(CompoundTag modifiers, int maxPerAxis, int maxMirrorRadius) {
        ListTag list = modifiers.getListOrEmpty(LIST_KEY);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag modifier = list.getCompoundOrEmpty(i);
            switch (modifier.getStringOr("type", "")) {
                case "Array" -> modifier.putInt("count", BuildLimits.arrayCount(modifier.getIntOr("count", 0), largestOffset(modifier), maxPerAxis));
                case "Mirror", "RadialMirror" -> modifier.putInt("radius", Math.max(0, Math.min(modifier.getIntOr("radius", 0), maxMirrorRadius)));
                default -> {
                }
            }
        }
        return modifiers;
    }

    /** How far (per axis) the enabled modifiers can move a block: an array its extent, a mirror twice its radius. */
    public static int reach(CompoundTag modifiers, int maxPerAxis, int maxMirrorRadius) {
        ListTag list = modifiers.getListOrEmpty(LIST_KEY);
        int reach = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag modifier = list.getCompoundOrEmpty(i);
            if (!modifier.getBooleanOr("enabled", false)) continue;
            switch (modifier.getStringOr("type", "")) {
                case "Array" -> {
                    int offset = largestOffset(modifier);
                    reach += offset * BuildLimits.arrayCount(modifier.getIntOr("count", 0), offset, maxPerAxis);
                }
                case "Mirror", "RadialMirror" -> reach += 2 * Math.max(0, Math.min(modifier.getIntOr("radius", 0), maxMirrorRadius));
                default -> {
                }
            }
        }
        return reach;
    }

    private static int largestOffset(CompoundTag array) {
        int[] offset = array.getIntArray("offset").orElse(new int[0]);
        int largest = 0;
        for (int value : offset) largest = Math.max(largest, Math.abs(value));
        return largest;
    }
}
