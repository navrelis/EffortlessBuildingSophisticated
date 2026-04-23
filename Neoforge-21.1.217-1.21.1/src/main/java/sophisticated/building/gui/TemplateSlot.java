package sophisticated.building.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A slot that restricts max stack size to 1 and prevents duplicate block types.
 * Used in Randomizer Bags to hold template blocks (just 1 of each block type as a reference).
 * Each unique block type can only occupy ONE slot in the bag.
 */
public class TemplateSlot extends SlotItemHandler {

    private final IItemHandler bagInventory;
    private final int bagSize;

    public TemplateSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, int totalBagSize) {
        super(itemHandler, index, xPosition, yPosition);
        this.bagInventory = itemHandler;
        this.bagSize = totalBagSize;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        
        // Check if this item type already exists in another slot of the bag
        for (int i = 0; i < bagSize; i++) {
            if (i == this.getSlotIndex()) {
                // Skip checking our own slot
                continue;
            }
            ItemStack existingStack = bagInventory.getStackInSlot(i);
            if (!existingStack.isEmpty() && ItemStack.isSameItem(existingStack, stack)) {
                // This item type already exists in another slot - reject it
                return false;
            }
        }
        
        return super.mayPlace(stack);
    }
}
