package sophisticated.building.item.upgrade;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class BuildingUpgradeWrapper extends UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem> {

    public BuildingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    /**
     * @return The tier of this building upgrade (1-5, where 5 is omega)
     */
    public int getTier() {
        return upgradeItem.getTier();
    }

    /**
     * @return Maximum blocks that can be placed at once with this upgrade
     */
    public int getMaxBlocks() {
        return upgradeItem.getMaxBlocks();
    }

    /**
     * @return The storage wrapper (backpack) this upgrade is installed in
     */
    public IStorageWrapper getStorageWrapper() {
        return storageWrapper;
    }

    /**
     * Attempts to extract a specific item from the backpack inventory.
     */
    public ItemStack extractItem(ItemStack item, int amount, boolean simulate) {
        if (!isEnabled()) {
            return ItemStack.EMPTY;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.size();
        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < slots && totalExtracted < amount; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, item)) {
                int wantFromSlot = Math.min(amount - totalExtracted, slotStack.getCount());

                int extractedFromSlot = 0;
                while (extractedFromSlot < wantFromSlot) {
                    int toExtractThisCall = Math.min(wantFromSlot - extractedFromSlot, item.getMaxStackSize());
                    ItemStack extractedStack = extract(inventoryHandler, i, toExtractThisCall, simulate);
                    if (extractedStack.isEmpty()) {
                        break;
                    }

                    if (result.isEmpty()) {
                        result = extractedStack.copy();
                    } else {
                        result.grow(extractedStack.getCount());
                    }
                    extractedFromSlot += extractedStack.getCount();
                    totalExtracted += extractedStack.getCount();

                    if (extractedStack.getCount() < toExtractThisCall) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Extracts up to {@code amount} of the stack in {@code slot} (at most one stack, as the item handler extraction did
     * before SophisticatedCore adopted the NeoForge 21.9 transfer API); {@code simulate} leaves the backpack unchanged.
     */
    private static ItemStack extract(InventoryHandler inventoryHandler, int slot, int amount, boolean simulate) {
        ItemResource resource = inventoryHandler.getResource(slot);
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = inventoryHandler.extract(slot, resource, Math.min(amount, resource.getMaxStackSize()), transaction);
            if (!simulate) {
                transaction.commit();
            }
            return resource.toStack(extracted);
        }
    }

    /**
     * @return The total count of the item in the backpack
     */
    public int countItem(ItemStack item) {
        if (!isEnabled()) {
            return 0;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.size();
        int count = 0;

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, item)) {
                count += slotStack.getCount();
            }
        }

        return count;
    }

    /**
     * @return true if there are placeable blocks in the backpack
     */
    public boolean hasPlaceableBlocks() {
        if (!isEnabled()) {
            return false;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.size();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canBeDisabled() {
        return true;
    }

    @Override
    public boolean hideSettingsTab() {
        return false;
    }
}
