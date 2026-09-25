package sophisticated.building.smoketest.backpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.smoketest.SmokeTest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Loader glue for an accessory slot mod (Curios, Trinkets, Accessories) that a backpack can be worn in, registered in
 * {@code META-INF/services} by the loader's smoke source set. Every method runs on the server thread.
 */
public interface SmokeAccessorySlots {

    /** The first registered implementation whose mod is loaded, or empty. */
    static Optional<SmokeAccessorySlots> find() {
        return SmokeTest.allServices(SmokeAccessorySlots.class).stream()
                .filter(SmokeAccessorySlots::isAvailable)
                .findFirst();
    }

    /** Why {@link #find()} is empty: the registered implementations and whether their mod is loaded. */
    static String whyUnavailable() {
        List<String> providers = SmokeTest.allServices(SmokeAccessorySlots.class).stream()
                .map(slots -> slots.getClass().getSimpleName() + (slots.isAvailable() ? "" : " (its mod is not loaded)"))
                .collect(Collectors.toList());
        return providers.isEmpty() ? "no accessory slot mod glue (Curios/Trinkets/Accessories) in this runtime"
                : "no accessory slot mod in this runtime: " + providers;
    }

    /** The accessory mod is loaded in this runtime. */
    boolean isAvailable();

    /** Name of the slot, e.g. "Curios back slot". */
    String describe();

    /** Puts the stack into the slot (EMPTY clears it). @return null on success, otherwise the reason */
    @Nullable
    String equip(ServerPlayer player, ItemStack backpack);

    ItemStack get(ServerPlayer player);
}
