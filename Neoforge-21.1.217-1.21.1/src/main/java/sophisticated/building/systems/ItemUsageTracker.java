package sophisticated.building.systems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import sophisticated.building.utilities.InventoryHelper;

import java.util.HashMap;
import java.util.Map;

//Common, both client and server have an instance of this
public class ItemUsageTracker {

    //How many blocks we want to place
    public Map<Item, Integer> total = new HashMap<>();

    //How many blocks we have in inventory in total
    public Map<Item, Integer> inInventory = new HashMap<>();

    //How many blocks we can place or have placed
    public Map<Item, Integer> placed = new HashMap<>();

    //How many blocks are missing from our inventory
    public Map<Item, Integer> missing = new HashMap<>();

    public void initialize() {
        total.clear();
        inInventory.clear();
        placed.clear();
        missing.clear();
    }

    //returns if we have enough items in inventory to use count more
    public boolean increaseUsageCount(Item item, int count, Player player) {
        if (item == null) return true;
        int newValue = total.getOrDefault(item, 0) + count;
        total.put(item, newValue);

        if (player.isCreative()) return true;
        int have = 0;
        if (inInventory.containsKey(item)) {
            have = inInventory.get(item);
        } else {
            have = InventoryHelper.findTotalItemsInInventory(player, item);
            inInventory.put(item, have);
        }

        return have >= newValue;
    }

    public void calculateMissingItems(Player player) {
        if (player.isCreative()) return;
        for (Item item : total.keySet()) {
            int used = total.get(item);
            int have = inInventory.getOrDefault(item, 0);
            placed.put(item, Math.min(used, have));
            if (used > have) {
                missing.put(item, used - have);
            }
        }
    }

    public int getValidCount(Item item) {
        return total.getOrDefault(item, 0) - missing.getOrDefault(item, 0);
    }

    public int getMissingCount(Item item) {
        return missing.getOrDefault(item, 0);
    }
}
