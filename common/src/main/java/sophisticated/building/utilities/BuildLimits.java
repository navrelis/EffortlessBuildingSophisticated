package sophisticated.building.utilities;

/**
 * Pure checks the server applies to a build or break request from a client, with the limits of the player's power level
 * (the same values the client uses to build the set): the start position within reach, the build mode's extent per
 * axis, the number of blocks, and every block within what the build mode and the player's modifiers can reach. No
 * Minecraft imports so this class is directly unit-testable (see {@code BuildLimitsTest}).
 */
public final class BuildLimits {

    /** Blocks the player may have moved between building the set on the client and its arrival on the server. */
    public static final int TOLERANCE = 3;

    private BuildLimits() {
    }

    /**
     * Farthest start position (first click) a legit client can send: its placement reach, at least the vanilla block
     * interaction range the client uses when it looks at a nearby block.
     */
    public static double startReach(int placementReach, double interactionRange) {
        return Math.max(placementReach, Math.ceil(interactionRange) + 1);
    }

    /** Whether the start position (squared block distance, as the client measures it) is within reach. */
    public static boolean startWithinReach(double distanceSquared, double startReach) {
        double max = startReach + TOLERANCE;
        return distanceSquared <= max * max;
    }

    /** Whether the build mode's first and last click span at most {@code maxPerAxis} blocks on every axis. */
    public static boolean extentWithinAxisLimit(int dx, int dy, int dz, int maxPerAxis) {
        return Math.abs(dx) + 1 <= maxPerAxis && Math.abs(dy) + 1 <= maxPerAxis && Math.abs(dz) + 1 <= maxPerAxis;
    }

    /**
     * Farthest (per axis) any block of a set can be from the player: the build mode's reach and extent plus what the
     * modifiers can add (an array its extent, a mirror twice its radius), plus the tolerance.
     */
    public static int maxBlockDistance(int buildModeReach, double startReach, int maxPerAxis, int modifierReach) {
        return (int) Math.ceil(Math.max(buildModeReach, startReach)) + maxPerAxis + modifierReach + TOLERANCE;
    }

    /** The number of entries a request may apply: its own size, at most the player's limit (0 = none). */
    public static int allowedCount(int requested, int maxPlacedAtOnce) {
        return Math.max(0, Math.min(requested, maxPlacedAtOnce));
    }

    /** An array's count cut so its extent (largest offset times count) stays within the blocks-per-axis limit. */
    public static int arrayCount(int count, int largestOffset, int maxPerAxis) {
        if (largestOffset <= 0) return count;
        return Math.max(0, Math.min(count, maxPerAxis / largestOffset));
    }
}
