package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import sophisticated.building.SophisticatedBuilding;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs {@link PlayerInventoryProvider#runOnBackpacks} of Sophisticated Backpacks 1.16.3 (1.0.0.94), where it is a
 * static method returning void (later builds moved it onto the proxy's provider instance and, from 3.26.0, made it
 * return boolean, which the other branches handle by reflection). 1.0.0.94 is the last Sophisticated Backpacks release
 * for Minecraft 1.16.3, so this branch calls it directly; a {@link LinkageError} (any other installed build) is
 * reported once and disables the backpack scan instead of crashing.
 */
public final class BackpackScanCompat {

    private static final AtomicBoolean REPORTED = new AtomicBoolean(false);

    private BackpackScanCompat() {
    }

    /**
     * Runs consumer over every backpack SophisticatedBackpacks knows for the player (inventory,
     * offhand, armour slot and the Curios handler Sophisticated Backpacks registers itself).
     *
     * @return true if the scan ran, false if the Backpacks API could not be linked (already logged)
     */
    public static boolean forEachBackpack(Player player, PlayerInventoryProvider.BackpackInventorySlotConsumer consumer) {
        try {
            PlayerInventoryProvider.runOnBackpacks(player, consumer);
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
                            + "The Building Upgrade backpack scan is disabled until this mod is rebuilt against that Backpacks build.",
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
