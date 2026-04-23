package sophisticated.building.inventory;

import net.minecraft.world.item.ItemStack;

public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    void setStackInSlot(int slot, ItemStack stack);

    default boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }
}
