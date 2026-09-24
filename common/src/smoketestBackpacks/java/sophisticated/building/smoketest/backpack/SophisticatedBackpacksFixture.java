package sophisticated.building.smoketest.backpack;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;
import java.util.Optional;

/**
 * {@link SmokeBackpacks} against the Sophisticated Backpacks/Core API. The same source compiles against the official
 * Forge builds and the unofficial Fabric port for Minecraft 1.20.1 (their wrapper/upgrade APIs match, except the
 * wrapper lookup, see wrapper, the name of the inventory slot count, see slotCount, and inserting: the Fabric port's
 * inventory only takes items through the Fabric Transfer API, so the contents are put into empty slots with
 * setStackInSlot, which both have); a port whose Sophisticated Backpacks API differs copies this class into its
 * loader's smoke source set and adapts it.
 */
public final class SophisticatedBackpacksFixture implements SmokeBackpacks {

    /** Five upgrade slots, so a Building Upgrade and a Tool Swapper fit next to each other. */
    private static final ResourceLocation BACKPACK = new ResourceLocation("sophisticatedbackpacks", "diamond_backpack");
    private static final ResourceLocation TOOL_SWAPPER = new ResourceLocation("sophisticatedbackpacks", "tool_swapper_upgrade");

    @Override
    public String describe() {
        return "Sophisticated Backpacks API (" + BackpackWrapper.class.getProtectionDomain().getCodeSource() + ")";
    }

    @Override
    public ItemStack createBackpack(int buildingUpgradeTier, boolean buildingUpgradeEnabled, boolean toolSwapper, List<ItemStack> contents) {
        ItemStack backpack = new ItemStack(item(BACKPACK));
        IBackpackWrapper wrapper = wrapper(backpack);
        // The inventory first: it gives the new backpack its storage UUID. Without one the wrapper hands out (and
        // keeps) an empty no-op upgrade handler.
        InventoryHandler inventory = wrapper.getInventoryHandler();

        int upgradeSlot = 0;
        if (buildingUpgradeTier > 0) {
            wrapper.getUpgradeHandler().setStackInSlot(upgradeSlot++, new ItemStack(buildingUpgrade(buildingUpgradeTier)));
        }
        if (toolSwapper) {
            wrapper.getUpgradeHandler().setStackInSlot(upgradeSlot, new ItemStack(item(TOOL_SWAPPER)));
        }

        int slot = 0;
        for (ItemStack content : contents) {
            while (slot < slotCount(inventory) && !inventory.getStackInSlot(slot).isEmpty()) slot++;
            if (slot >= slotCount(inventory) || content.getCount() > inventory.getSlotLimit(slot)) {
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
        InventoryHandler inventory = wrapper(backpack).getInventoryHandler();
        int total = 0;
        for (int slot = 0; slot < slotCount(inventory); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public ItemStack find(ItemStack backpack, Item item) {
        InventoryHandler inventory = wrapper(backpack).getInventoryHandler();
        for (int slot = 0; slot < slotCount(inventory); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setBuildingUpgradeEnabled(ItemStack backpack, boolean enabled) {
        IBackpackWrapper wrapper = wrapper(backpack);
        for (IUpgradeWrapper upgrade : wrapper.getUpgradeHandler().getSlotWrappers().values()) {
            ResourceLocation id = Registry.ITEM.getKey(upgrade.getUpgradeStack().getItem());
            if (id.getNamespace().equals(SophisticatedBuilding.MODID) && id.getPath().startsWith("building_upgrade")) {
                upgrade.setEnabled(enabled);
                return;
            }
        }
        throw new IllegalStateException("The backpack has no Building Upgrade");
    }

    /**
     * The backpack's wrapper, as the mod looks it up: {@code BackpackWrapperLookup.get(stack)} on the Fabric port, the
     * backpack wrapper capability of the stack on Forge. Both return a LazyOptional (Porting Lib's or Forge's) with a
     * {@code resolve()} to an Optional; resolved by reflection so this class compiles against both loaders.
     */
    private static IBackpackWrapper wrapper(ItemStack backpack) {
        try {
            Object lazy;
            try {
                Class<?> lookup = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.common.BackpackWrapperLookup");
                lazy = lookup.getMethod("get", ItemStack.class).invoke(null, backpack);
            } catch (ClassNotFoundException e) {
                Object capability = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper")
                        .getMethod("getCapabilityInstance").invoke(null);
                lazy = ItemStack.class.getMethod("getCapability", Class.forName("net.minecraftforge.common.capabilities.Capability"))
                        .invoke(backpack, capability);
            }
            Optional<?> wrapper = (Optional<?>) lazy.getClass().getMethod("resolve").invoke(lazy);
            return (IBackpackWrapper) wrapper.orElseThrow(() -> new IllegalStateException("No backpack wrapper for " + backpack));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** int getSlots() on Forge (IItemHandler), int getSlotCount() on the Fabric port (Porting Lib; its getSlots() is a list). */
    private static int slotCount(InventoryHandler inventory) {
        for (String name : new String[] {"getSlotCount", "getSlots"}) {
            try {
                java.lang.reflect.Method method = inventory.getClass().getMethod(name);
                if (method.getReturnType() != int.class) continue;
                return (int) method.invoke(inventory);
            } catch (NoSuchMethodException ignored) {
                // the other loader's name
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("InventoryHandler has neither getSlots() nor getSlotCount()");
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
