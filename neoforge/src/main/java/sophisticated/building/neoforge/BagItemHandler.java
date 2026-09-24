package sophisticated.building.neoforge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import sophisticated.building.inventory.ItemStackHandler;

/**
 * The randomizer bags' item handler capability: the bag's {@code Items} tag through the common
 * {@link ItemStackHandler.BagItemStackHandler}. Like the item handler over the container component that the 1.21
 * builds use, it only accepts items that fit inside container items (no shulker boxes).
 */
final class BagItemHandler implements IItemHandlerModifiable {

    private final ItemStackHandler.BagItemStackHandler inventory;

    BagItemHandler(ItemStack bag, int size) {
        this.inventory = new ItemStackHandler.BagItemStackHandler(bag, size);
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!stack.isEmpty() && !isItemValid(slot, stack)) {
            return stack;
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }
}
