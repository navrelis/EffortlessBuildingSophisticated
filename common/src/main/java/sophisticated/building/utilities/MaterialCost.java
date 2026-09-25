package sophisticated.building.utilities;

import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/** The items a set of block entries costs, per item (the material cost list of the HUD). */
public final class MaterialCost {

    private MaterialCost() {
    }

    /** Items per item type, counted like the server charges them ({@link BlockUtilities#placementCost}). */
    public static Map<Item, Integer> tally(Iterable<BlockEntry> entries) {
        Map<Item, Integer> costs = new LinkedHashMap<>();
        for (BlockEntry entry : entries) {
            if (entry.newBlockState == null || entry.newBlockState.isAir()) continue;
            Item item = entry.newBlockState.getBlock().asItem();
            int cost = BlockUtilities.placementCost(entry.existingBlockState, entry.newBlockState);
            if (item != null && cost > 0) {
                costs.merge(item, cost, Integer::sum);
            }
        }
        return costs;
    }
}
