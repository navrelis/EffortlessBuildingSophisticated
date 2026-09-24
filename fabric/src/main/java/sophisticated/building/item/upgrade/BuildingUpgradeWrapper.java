package sophisticated.building.item.upgrade;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class BuildingUpgradeWrapper extends UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem> {

    public BuildingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    public int getTier() {
        return upgradeItem.getTier();
    }

    public int getMaxBlocks() {
        return upgradeItem.getMaxBlocks();
    }

    public IStorageWrapper getStorageWrapper() {
        return storageWrapper;
    }

    public ItemStack extractItem(ItemStack item, int amount, boolean simulate) {
        if (!isEnabled()) {
            return ItemStack.EMPTY;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlotCount();
        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < slots && totalExtracted < amount; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, item)) {
                int wantFromSlot = Math.min(amount - totalExtracted, slotStack.getCount());

                int extractedFromSlot = 0;
                while (extractedFromSlot < wantFromSlot) {
                    int toExtractThisCall = Math.min(wantFromSlot - extractedFromSlot, item.getMaxStackSize());
                    ItemStack extractedStack = extractFromSlot(inventoryHandler, i, toExtractThisCall, simulate);
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

    // The 1.20.4 Fabric port's inventory is a Fabric Transfer API storage: extract the slot's variant in a transaction
    // that is only committed when not simulating (what extractItem(slot, amount, simulate) does on the other builds).
    private static ItemStack extractFromSlot(InventoryHandler inventoryHandler, int slot, int amount, boolean simulate) {
        ItemVariant variant = ItemVariant.of(inventoryHandler.getStackInSlot(slot));
        if (variant.isBlank()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = inventoryHandler.extractSlot(slot, variant, amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted > 0 ? variant.toStack((int) extracted) : ItemStack.EMPTY;
        }
    }

    public int countItem(ItemStack item) {
        if (!isEnabled()) {
            return 0;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlotCount();
        int count = 0;

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, item)) {
                count += slotStack.getCount();
            }
        }

        return count;
    }

    public boolean hasPlaceableBlocks() {
        if (!isEnabled()) {
            return false;
        }

        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlotCount();

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
