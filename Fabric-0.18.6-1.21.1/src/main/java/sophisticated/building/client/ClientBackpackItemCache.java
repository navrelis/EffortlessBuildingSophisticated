package sophisticated.building.client;

import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight client cache for backpack item counts, updated via network packets.
 */
public class ClientBackpackItemCache {
    private static final Map<Item, Integer> COUNTS = new ConcurrentHashMap<>();

    public static void setCount(Item item, int count) {
        if (count <= 0) {
            COUNTS.remove(item);
        } else {
            COUNTS.put(item, count);
        }
    }

    public static int getCount(Item item) {
        return COUNTS.getOrDefault(item, 0);
    }

    public static void clear() {
        COUNTS.clear();
    }
}
