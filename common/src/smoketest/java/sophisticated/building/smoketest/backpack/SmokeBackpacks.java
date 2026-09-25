package sophisticated.building.smoketest.backpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.smoketest.SmokeTest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Test fixture for the Sophisticated Backpacks scenarios: builds real backpacks with real upgrades through the
 * Sophisticated Backpacks/Core API. Implemented once in {@code common/src/smoketestBackpacks} (compiled by every loader
 * build that has Sophisticated Backpacks) and registered in {@code META-INF/services}; absent on loaders without
 * Sophisticated Backpacks. Every method must run on the server thread.
 */
public interface SmokeBackpacks {

    /** The fixture of this loader build, or empty when it has no Sophisticated Backpacks. */
    static Optional<SmokeBackpacks> find() {
        return SmokeTest.firstService(SmokeBackpacks.class);
    }

    /** Human readable description of the Sophisticated Backpacks build under test. */
    String describe();

    /**
     * A new backpack (enough upgrade slots for two upgrades) with a Building Upgrade of the given tier (0 = none),
     * enabled or disabled, optionally a Tool Swapper upgrade, and the given contents.
     */
    ItemStack createBackpack(int buildingUpgradeTier, boolean buildingUpgradeEnabled, boolean toolSwapper, List<ItemStack> contents);

    /** Items of this type inside the backpack. */
    int count(ItemStack backpack, Item item);

    /** The first stack of this item inside the backpack, or empty. */
    ItemStack find(ItemStack backpack, Item item);

    /** Enables or disables the backpack's Building Upgrade (like its toggle button in the backpack screen). */
    void setBuildingUpgradeEnabled(ItemStack backpack, boolean enabled);

    /** Whether the backpack's Building Upgrade is enabled (what its settings tab toggles). */
    boolean isBuildingUpgradeEnabled(ItemStack backpack);

    /**
     * Puts the backpack into an accessory slot (Curios on NeoForge, Trinkets on Fabric) when such a mod is present.
     *
     * @return null on success, otherwise why it is not possible in this runtime
     */
    @Nullable
    default String equipInAccessorySlot(ServerPlayer player, ItemStack backpack) {
        Optional<SmokeAccessorySlots> slots = SmokeAccessorySlots.find();
        // Not Optional.map: equip returns null on success
        return slots.isPresent() ? slots.get().equip(player, backpack) : SmokeAccessorySlots.whyUnavailable();
    }

    /** The backpack in the accessory slot {@link #equipInAccessorySlot} used, or empty. */
    default ItemStack getFromAccessorySlot(ServerPlayer player) {
        return SmokeAccessorySlots.find().map(slots -> slots.get(player)).orElse(ItemStack.EMPTY);
    }
}
