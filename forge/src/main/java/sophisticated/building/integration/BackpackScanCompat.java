package sophisticated.building.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.ReturnTypeAgnosticInvoker;

import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs {@link PlayerInventoryProvider#runOnBackpacks} regardless of whether the installed
 * SophisticatedBackpacks build declares it {@code void} (&lt;= 3.25.x, all Fabric ports) or
 * {@code boolean} (&gt;= 3.26.0). The compiled descriptor of a direct call bakes in one return
 * type, so a mismatched installed build fails to link with {@code NoSuchMethodError}
 * (see ANALYSIS_AND_INSTRUCTIONS/10_UPSTREAM_API_BREAK_4.1.1.md). Resolving the method by
 * reflection and invoking it through a {@link MethodHandle} as a statement ignores the return
 * type entirely, so both variants work against the one compiled jar.
 */
public final class BackpackScanCompat {

    private static final AtomicBoolean REPORTED = new AtomicBoolean(false);

    private static volatile Optional<MethodHandle> handle;

    private BackpackScanCompat() {
    }

    private static Optional<MethodHandle> handle() {
        Optional<MethodHandle> local = handle;
        if (local == null) {
            synchronized (BackpackScanCompat.class) {
                local = handle;
                if (local == null) {
                    local = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
                            PlayerInventoryProvider.class,
                            "runOnBackpacks",
                            Player.class,
                            PlayerInventoryProvider.BackpackInventorySlotConsumer.class);
                    handle = local;
                }
            }
        }
        return local;
    }

    /**
     * Runs consumer over every backpack SophisticatedBackpacks knows for the player (inventory,
     * offhand, armour slot, Trinkets/Accessories/Curios handlers). Works with Backpacks builds where
     * runOnBackpacks returns void (&lt;= 3.25.x, all Fabric ports) and boolean (&gt;= 3.26.0).
     *
     * @return true if the scan ran, false if the Backpacks API could not be linked (already logged)
     */
    public static boolean forEachBackpack(Player player, PlayerInventoryProvider.BackpackInventorySlotConsumer consumer) {
        Optional<MethodHandle> methodHandle = handle();
        if (methodHandle.isEmpty()) {
            reportUnavailable(null);
            return false;
        }

        try {
            // Statement form: the call-site return type is void, so a boolean-returning handle has
            // its result dropped by asType (JLS 15.12.3). Checked exceptions cannot come out of
            // runOnBackpacks, so any unexpected checked exception is wrapped rather than declared.
            methodHandle.get().invoke(PlayerInventoryProvider.get(), player, consumer);
            return true;
        } catch (LinkageError e) {
            reportUnavailable(e);
            return false;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
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
            var container = ModList.get().getModContainerById("sophisticatedbackpacks");
            return container.map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
