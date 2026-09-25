package sophisticated.building.smoketest.backpack;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.BackpackInventoryHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.BackpackUpgradeHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.util.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.IBackpackWrapper;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;
import java.util.Map;

/**
 * {@link SmokeBackpacks} against the Sophisticated Backpacks API of Minecraft 1.16.3 (1.0.0.94, one of its first
 * releases; only the official Forge build exists): the wrapper, upgrade and inventory handlers live in
 * net.p3pp3rf1y.sophisticatedbackpacks.util, the wrapper comes from the stack's backpack wrapper capability, and there is
 * no Tool Swapper upgrade and no upgrade enable switch (the Building Upgrade keeps its own "enabled" tag).
 */
public final class SophisticatedBackpacksFixture implements SmokeBackpacks {

    /** Five upgrade slots, like on the other branches. */
    private static final ResourceLocation BACKPACK = new ResourceLocation("sophisticatedbackpacks", "diamond_backpack");
    private static final String ENABLED_TAG = "enabled";

    @Override
    public String describe() {
        return "Sophisticated Backpacks API (" + BackpackWrapper.class.getProtectionDomain().getCodeSource() + ")";
    }

    @Override
    public String whyNoToolSwapper() {
        return "Sophisticated Backpacks 1.16.3 (1.0.0.94) has no Tool Swapper upgrade (it arrives with the 1.16.4+ builds)";
    }

    @Override
    public ItemStack createBackpack(int buildingUpgradeTier, boolean buildingUpgradeEnabled, boolean toolSwapper, List<ItemStack> contents) {
        if (toolSwapper) {
            throw new IllegalStateException(whyNoToolSwapper());
        }
        ItemStack backpack = new ItemStack(item(BACKPACK));
        IBackpackWrapper wrapper = wrapper(backpack);
        BackpackInventoryHandler inventory = wrapper.getInventoryHandler();

        if (buildingUpgradeTier > 0) {
            wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(buildingUpgrade(buildingUpgradeTier)));
        }

        int slot = 0;
        for (ItemStack content : contents) {
            while (slot < inventory.getSlots() && !inventory.getStackInSlot(slot).isEmpty()) slot++;
            if (slot >= inventory.getSlots() || content.getCount() > inventory.getSlotLimit(slot)) {
                throw new IllegalStateException("The backpack did not take " + content);
            }
            inventory.setStackInSlot(slot, content.copy());
        }

        if (buildingUpgradeTier > 0 && !buildingUpgradeEnabled) {
            setBuildingUpgradeEnabled(backpack, false);
        }
        return backpack;
    }

    @Override
    public int count(ItemStack backpack, Item item) {
        BackpackInventoryHandler inventory = wrapper(backpack).getInventoryHandler();
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if ((stack.getItem() == item)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public ItemStack find(ItemStack backpack, Item item) {
        BackpackInventoryHandler inventory = wrapper(backpack).getInventoryHandler();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if ((stack.getItem() == item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Writes the upgrade's "enabled" tag, like the toggle of its settings tab (BuildingUpgradeWrapper.setEnabled). */
    @Override
    public void setBuildingUpgradeEnabled(ItemStack backpack, boolean enabled) {
        BackpackUpgradeHandler upgrades = wrapper(backpack).getUpgradeHandler();
        for (Map.Entry<Integer, IUpgradeWrapper> upgrade : upgrades.getSlotWrappers().entrySet()) {
            ItemStack upgradeStack = upgrade.getValue().getUpgradeStack().copy();
            ResourceLocation id = Registry.ITEM.getKey(upgradeStack.getItem());
            if (id.getNamespace().equals(SophisticatedBuilding.MODID) && id.getPath().startsWith("building_upgrade")) {
                upgradeStack.getOrCreateTag().putBoolean(ENABLED_TAG, enabled);
                upgrades.setStackInSlot(upgrade.getKey(), upgradeStack);
                return;
            }
        }
        throw new IllegalStateException("The backpack has no Building Upgrade");
    }

    /** The backpack's wrapper, as the mod looks it up: the backpack wrapper capability of the stack. */
    private static IBackpackWrapper wrapper(ItemStack backpack) {
        return backpack.getCapability(BackpackWrapper.BACKPACK_WRAPPER_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("No backpack wrapper for " + backpack));
    }

    private static Item buildingUpgrade(int tier) {
        String path = tier >= 5 ? "building_upgrade_omega" : "building_upgrade_" + tier;
        return item(SophisticatedBuilding.asResource(path));
    }

    private static Item item(ResourceLocation id) {
        Item item = Registry.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Item " + id + " is not registered");
        }
        return item;
    }
}
