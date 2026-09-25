package sophisticated.building.utilities;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//Server only. Finds the real inventory stack whose data a placed block should get, for one block set.
public class PlacementTemplates {

    /**
     * @param stack      the inventory stack to copy data from; may be a fresh plain stack when the
     *                   inventory has none left (backpack path)
     * @param individual true if this is a stack with data that the caller must consume itself (survival)
     */
    public static final class Template {
        private final ItemStack stack;
        private final boolean individual;

        public Template(ItemStack stack, boolean individual) {
            this.stack = stack;
            this.individual = individual;
        }

        public ItemStack stack() {
            return stack;
        }

        public boolean individual() {
            return individual;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Template)) return false;
            Template that = (Template) o;
            return Objects.equals(stack, that.stack) && individual == that.individual;
        }

        @Override
        public int hashCode() {
            return Objects.hash(stack, individual);
        }
    }

    private final Player player;
    private final TemplateSelector<ItemStack> selector = new TemplateSelector<>(ItemStack::getCount, PlacementTemplates::hasData);
    private final Map<Item, Integer> anchorCounts = new HashMap<>();

    public PlacementTemplates(Player player) {
        this.player = player;
    }

    public static boolean hasData(ItemStack stack) {
        return stack.getTag() != null && !stack.getTag().isEmpty();
    }

    // Search order: main hand, offhand, rest of the main inventory
    public Template find(Item item) {
        Inventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getSelected();
        List<ItemStack> candidates = new ArrayList<>();
        addIfMatching(candidates, mainHand, item);
        addIfMatching(candidates, inventory.offhand.get(0), item);
        for (int i = 0; i < inventory.items.size(); i++) {
            if (i == inventory.selected) continue;
            addIfMatching(candidates, inventory.items.get(i), item);
        }

        if (player.isCreative()) {
            return new Template(candidates.isEmpty() ? new ItemStack(item) : candidates.get(0), false);
        }

        int anchorCount = anchorCounts.computeIfAbsent(item, i -> InventoryHelper.getReservedHeldCount(player, i));
        ItemStack selected = selector.select(candidates, mainHand, anchorCount);
        if (selected == null) {
            return new Template(new ItemStack(item), false);
        }
        return new Template(selected, hasData(selected));
    }

    private static void addIfMatching(List<ItemStack> candidates, ItemStack stack, Item item) {
        if (!stack.isEmpty() && stack.getItem() == item) {
            candidates.add(stack);
        }
    }
}
