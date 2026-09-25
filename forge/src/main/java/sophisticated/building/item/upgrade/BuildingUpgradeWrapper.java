package sophisticated.building.item.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedbackpacks.util.BackpackInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.IBackpackWrapper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Sophisticated Backpacks 1.16.3 (1.0.0.94) creates upgrade wrappers from the upgrade stack alone (no backpack wrapper
 * argument) and has no enable/disable switch for upgrades. The backpack this wrapper is installed in is linked by
 * {@link BuildingUpgradeHelper} when it looks the upgrade up, and the enabled state is kept in the upgrade stack under
 * the same "enabled" tag later Sophisticated Backpacks builds use (default enabled).
 */
public class BuildingUpgradeWrapper extends UpgradeWrapperBase<BuildingUpgradeWrapper, BuildingUpgradeItem> {
    private static final String ENABLED_TAG = "enabled";

    @Nullable
    private IBackpackWrapper backpackWrapper;

    public BuildingUpgradeWrapper(ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(upgrade, upgradeSaveHandler);
    }

    /** Links the backpack this upgrade was found in (its upgrade handler created this wrapper). */
    void setBackpackWrapper(IBackpackWrapper backpackWrapper) {
        this.backpackWrapper = backpackWrapper;
    }

    public boolean isEnabled() {
        CompoundTag tag = upgrade.getTag();
        return tag == null || !tag.contains(ENABLED_TAG) || tag.getBoolean(ENABLED_TAG);
    }

    public void setEnabled(boolean enabled) {
        upgrade.getOrCreateTag().putBoolean(ENABLED_TAG, enabled);
        save();
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
     * @return The backpack wrapper this upgrade is installed in, null before BuildingUpgradeHelper linked it
     */
    @Nullable
    public IBackpackWrapper getBackpackWrapper() {
        return backpackWrapper;
    }

    /**
     * Attempts to extract a specific item from the backpack inventory.
     */
    public ItemStack extractItem(ItemStack item, int amount, boolean simulate) {
        if (!isEnabled() || backpackWrapper == null) {
            return ItemStack.EMPTY;
        }

        BackpackInventoryHandler inventoryHandler = backpackWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlots();
        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < slots && totalExtracted < amount; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && (ItemStack.isSame(slotStack, item) && ItemStack.tagMatches(slotStack, item))) {
                int wantFromSlot = Math.min(amount - totalExtracted, slotStack.getCount());

                int extractedFromSlot = 0;
                while (extractedFromSlot < wantFromSlot) {
                    int toExtractThisCall = Math.min(wantFromSlot - extractedFromSlot, item.getMaxStackSize());
                    ItemStack extractedStack = inventoryHandler.extractItem(i, toExtractThisCall, simulate);
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
     * @return The total count of the item in the backpack
     */
    public int countItem(ItemStack item) {
        if (!isEnabled() || backpackWrapper == null) {
            return 0;
        }

        BackpackInventoryHandler inventoryHandler = backpackWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlots();
        int count = 0;

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && (ItemStack.isSame(slotStack, item) && ItemStack.tagMatches(slotStack, item))) {
                count += slotStack.getCount();
            }
        }

        return count;
    }

    /**
     * @return true if there are placeable blocks in the backpack
     */
    public boolean hasPlaceableBlocks() {
        if (!isEnabled() || backpackWrapper == null) {
            return false;
        }

        BackpackInventoryHandler inventoryHandler = backpackWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return true;
            }
        }

        return false;
    }
}
