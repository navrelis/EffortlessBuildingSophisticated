package sophisticated.building.smoketest.backpack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;

/**
 * {@link SmokeBackpacks} against the Sophisticated Backpacks/Core API. The same source compiles against the official
 * NeoForge builds and the unofficial Fabric port for Minecraft 1.21.1 (their wrapper/upgrade APIs match, except the
 * name of the inventory slot count, see slotCount); on Minecraft 1.21.4 and later only NeoForge has
 * Sophisticated Backpacks, and since 1.21.9 the backpack is filled through the NeoForge transfer API. A port whose
 * Sophisticated Backpacks API differs copies this class into its loader's smoke source set and adapts it.
 */
public final class SophisticatedBackpacksFixture implements SmokeBackpacks {

    /** Five upgrade slots, so a Building Upgrade and a Tool Swapper fit next to each other. */
    private static final Identifier BACKPACK = Identifier.fromNamespaceAndPath("sophisticatedbackpacks", "diamond_backpack");
    private static final Identifier TOOL_SWAPPER = Identifier.fromNamespaceAndPath("sophisticatedbackpacks", "tool_swapper_upgrade");

    @Override
    public String describe() {
        return "Sophisticated Backpacks API (" + BackpackWrapper.class.getProtectionDomain().getCodeSource() + ")";
    }

    @Override
    public ItemStack createBackpack(int buildingUpgradeTier, boolean buildingUpgradeEnabled, boolean toolSwapper, List<ItemStack> contents) {
        ItemStack backpack = new ItemStack(item(BACKPACK));
        IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
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

        for (ItemStack content : contents) {
            ItemStack rest = insert(inventory, content);
            if (!rest.isEmpty()) {
                throw new IllegalStateException("The backpack did not take " + content + " (left " + rest + ")");
            }
        }

        if (buildingUpgradeTier > 0 && !buildingUpgradeEnabled) {
            setBuildingUpgradeEnabled(backpack, false);
        }
        return backpack;
    }

    /** Inserts {@code content} anywhere in the backpack; returns what did not fit (transfer API of NeoForge 21.9+). */
    private static ItemStack insert(InventoryHandler inventory, ItemStack content) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = inventory.insert(ItemResource.of(content), content.getCount(), transaction);
            transaction.commit();
            return content.copyWithCount(content.getCount() - inserted);
        }
    }

    @Override
    public int count(ItemStack backpack, Item item) {
        InventoryHandler inventory = BackpackWrapper.fromStack(backpack).getInventoryHandler();
        int total = 0;
        for (int slot = 0; slot < slotCount(inventory); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    @Override
    public ItemStack find(ItemStack backpack, Item item) {
        InventoryHandler inventory = BackpackWrapper.fromStack(backpack).getInventoryHandler();
        for (int slot = 0; slot < slotCount(inventory); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setBuildingUpgradeEnabled(ItemStack backpack, boolean enabled) {
        buildingUpgradeWrapper(backpack).setEnabled(enabled);
    }

    @Override
    public boolean isBuildingUpgradeEnabled(ItemStack backpack) {
        return buildingUpgradeWrapper(backpack).isEnabled();
    }

    private static IUpgradeWrapper buildingUpgradeWrapper(ItemStack backpack) {
        IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpack);
        for (IUpgradeWrapper upgrade : wrapper.getUpgradeHandler().getSlotWrappers().values()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(upgrade.getUpgradeStack().getItem());
            if (id.getNamespace().equals(SophisticatedBuilding.MODID) && id.getPath().startsWith("building_upgrade")) {
                return upgrade;
            }
        }
        throw new IllegalStateException("The backpack has no Building Upgrade");
    }

    /**
     * int size() on NeoForge 21.9+ (ResourceHandler), int getSlots() on older NeoForge (IItemHandler), int getSlotCount()
     * on the Fabric port (Porting Lib).
     */
    private static int slotCount(InventoryHandler inventory) {
        for (String name : new String[] {"size", "getSlotCount", "getSlots"}) {
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
        throw new IllegalStateException("InventoryHandler has none of size(), getSlots() and getSlotCount()");
    }

    private static Item buildingUpgrade(int tier) {
        String path = tier >= 5 ? "building_upgrade_omega" : "building_upgrade_" + tier;
        return item(SophisticatedBuilding.asResource(path));
    }

    private static Item item(Identifier id) {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Item " + id + " is not registered");
        }
        return item;
    }
}
