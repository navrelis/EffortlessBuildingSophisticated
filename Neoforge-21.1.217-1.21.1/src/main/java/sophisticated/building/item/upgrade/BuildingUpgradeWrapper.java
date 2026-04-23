package sophisticated.building.item.upgrade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;

import java.util.function.Consumer;

public class BuildingUpgradeWrapper implements IUpgradeWrapper {
    
    private final IStorageWrapper storageWrapper;
    private final ItemStack upgrade;
    private final BuildingUpgradeItem upgradeItem;
    private final Consumer<ItemStack> upgradeSaveHandler;
    private boolean enabled = true;
    
    public BuildingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        this.storageWrapper = storageWrapper;
        this.upgrade = upgrade;
        this.upgradeItem = (BuildingUpgradeItem) upgrade.getItem();
        this.upgradeSaveHandler = upgradeSaveHandler;

        CustomData customData = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("enabled")) {
            this.enabled = tag.getBoolean("enabled");
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        CustomData customData = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean("enabled", enabled);
        upgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        save();
    }
    
    @Override
    public ItemStack getUpgradeStack() {
        return upgrade;
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
        int slots = inventoryHandler.getSlots();
        int totalExtracted = 0;
        ItemStack result = ItemStack.EMPTY;
        
        for (int i = 0; i < slots && totalExtracted < amount; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, item)) {
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
        if (!isEnabled()) {
            return 0;
        }
        
        var inventoryHandler = storageWrapper.getInventoryHandler();
        int slots = inventoryHandler.getSlots();
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
        int slots = inventoryHandler.getSlots();
        
        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                return true;
            }
        }
        
        return false;
    }
    
    private void save() {
        upgradeSaveHandler.accept(upgrade);
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
