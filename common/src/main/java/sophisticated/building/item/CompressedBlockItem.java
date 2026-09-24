package sophisticated.building.item;

import net.minecraft.world.item.Item;
import sophisticated.building.SophisticatedBuilding;

/**
 * Simple item representing a compressed block material.
 * Used as crafting ingredients for building upgrades.
 */
public class CompressedBlockItem extends Item {
    public CompressedBlockItem() {
        super(new Item.Properties().stacksTo(64).tab(SophisticatedBuilding.CREATIVE_TAB));
    }
}
