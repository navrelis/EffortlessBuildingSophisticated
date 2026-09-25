package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import sophisticated.building.SophisticatedBuilding;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs {@link PlayerInventoryProvider#runOnBackpacks} of Sophisticated Backpacks 1.16.4 (3.0.0.289), where it is a
 * static method returning void whose consumer gets no slot identifier (the 1.16.5 builds moved it onto the proxy's
 * provider instance and added the identifier; from 3.26.0 it returns boolean, which the other branches handle by
 * reflection). 3.0.0.289 is the last Sophisticated Backpacks release for Minecraft 1.16.4, so this build calls it
 * directly; a {@link LinkageError} (any other installed build) is reported once and disables the backpack scan instead
 * of crashing. The consumer keeps the shape of the 1.16.5 build (the identifier is the inventory handler name), so the
 * callers are the same in both Forge jars.
 */
public final class BackpackScanCompat {

    private static final AtomicBoolean REPORTED = new AtomicBoolean(false);

    private BackpackScanCompat() {
    }

    /** {@code PlayerInventoryProvider.BackpackInventorySlotConsumer} of the 1.16.5 builds; return true to stop. */
    @FunctionalInterface
    public interface BackpackSlotConsumer {
        boolean accept(ItemStack backpack, String inventoryHandlerName, String identifier, int slot);
    }

    /**
     * Runs consumer over every backpack SophisticatedBackpacks knows for the player (inventory, offhand and the
     * inventory handlers other mods registered, e.g. its own Curios handler).
     *
     * @return true if the scan ran, false if the Backpacks API could not be linked (already logged)
     */
    public static boolean forEachBackpack(Player player, BackpackSlotConsumer consumer) {
        try {
            PlayerInventoryProvider.runOnBackpacks(player, (backpack, inventoryHandlerName, slot) ->
                    consumer.accept(backpack, inventoryHandlerName, inventoryHandlerName, slot));
            return true;
        } catch (LinkageError e) {
            reportUnavailable(e);
            return false;
        }
    }

    private static void reportUnavailable(Throwable cause) {
        if (REPORTED.compareAndSet(false, true)) {
            SophisticatedBuilding.logger.warn(
                    "Could not link SophisticatedBackpacks' PlayerInventoryProvider.runOnBackpacks (installed version: {}). "
                            + "The Building Upgrade / Tool Swapper backpack scan is disabled until this mod is rebuilt against that Backpacks build.",
                    installedBackpacksVersion(), cause);
        } else {
            SophisticatedBuilding.logger.debug(
                    "SophisticatedBackpacks' PlayerInventoryProvider.runOnBackpacks is still not linkable (installed version: {}).",
                    installedBackpacksVersion());
        }
    }

    // loader-specific
    private static String installedBackpacksVersion() {
        try {
            Optional<? extends ModContainer> container = ModList.get().getModContainerById("sophisticatedbackpacks");
            return container.map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
