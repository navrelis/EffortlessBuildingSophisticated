package sophisticated.building.client;

/**
 * Client-side holder for the player's Building Upgrade state, populated only by
 * {@code BuildingUpgradeStatePacket} from the server. The client must never derive this from a
 * local backpack/upgrade wrapper lookup (see 03_ROOT_CAUSE_BUILDING_UPGRADE.md RC2): the client's
 * copy of a backpack's upgrade inventory is not reliably synced outside of an open backpack menu.
 */
public class ClientBuildingUpgradeState {
    private static volatile int tier = 0;
    private static volatile int maxBlocks = 0;

    private ClientBuildingUpgradeState() {
    }

    public static void set(int newTier, int newMaxBlocks) {
        tier = newTier;
        maxBlocks = newMaxBlocks;
    }

    public static int getTier() {
        return tier;
    }

    public static int getMaxBlocks() {
        return maxBlocks;
    }

    public static boolean hasUpgrade() {
        return tier > 0 && maxBlocks > 0;
    }

    public static void clear() {
        tier = 0;
        maxBlocks = 0;
    }
}
