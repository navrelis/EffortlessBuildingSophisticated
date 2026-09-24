package sophisticated.building.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class ItemStackHandler implements IItemHandler {
    protected final NonNullList<ItemStack> stacks;

    public ItemStackHandler(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack existing = stacks.get(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                return stack.copy();
            }
            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack.copy();
        }

        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                ItemStack inserted = stack.copy();
                inserted.setCount(toInsert);
                stacks.set(slot, inserted);
            } else {
                existing.grow(toInsert);
            }
            onContentsChanged(slot);
        }

        if (stack.getCount() == toInsert) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(toInsert);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);

        if (!simulate) {
            existing.shrink(toExtract);
            if (existing.isEmpty()) {
                stacks.set(slot, ItemStack.EMPTY);
            }
            onContentsChanged(slot);
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (stack.isEmpty()) {
            stacks.set(slot, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(Math.min(copy.getCount(), getSlotLimit(slot)));
            stacks.set(slot, copy);
        }
        onContentsChanged(slot);
    }

    protected void onContentsChanged(int slot) {
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
        }
    }

    public static class BagItemStackHandler extends ItemStackHandler {
        private static final ItemContainerContents EMPTY_CONTENTS = ItemContainerContents.fromItems(NonNullList.create());

        private final ItemStack bagStack;
        private boolean loading;

        public BagItemStackHandler(ItemStack bagStack, int size) {
            super(size);
            this.bagStack = bagStack;
            loadFromBag();
        }

        private void loadFromBag() {
            loading = true;
            ItemContainerContents container = bagStack.getOrDefault(DataComponents.CONTAINER, EMPTY_CONTENTS);
            NonNullList<ItemStack> loaded = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
            container.copyInto(loaded);
            for (int i = 0; i < getSlots(); i++) {
                ItemStack loadedStack = i < loaded.size() ? loaded.get(i) : ItemStack.EMPTY;
                stacks.set(i, loadedStack.copy());
            }
            loading = false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (loading) {
                return;
            }

            NonNullList<ItemStack> toSave = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < getSlots(); i++) {
                toSave.set(i, stacks.get(i).copy());
            }
            bagStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(toSave));
        }
    }
}
