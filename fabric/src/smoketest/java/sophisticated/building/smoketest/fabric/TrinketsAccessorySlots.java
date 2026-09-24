package sophisticated.building.smoketest.fabric;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.smoketest.backpack.SmokeAccessorySlots;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/** The Trinkets "chest/back" slot the Sophisticated Backpacks Fabric port uses, when Trinkets is in the runtime. */
public final class TrinketsAccessorySlots implements SmokeAccessorySlots {

    private static final String GROUP = "chest";
    private static final String SLOT = "back";

    @Override
    public boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("trinkets");
    }

    @Override
    public String describe() {
        return "Trinkets '" + GROUP + "/" + SLOT + "' slot";
    }

    @Nullable
    @Override
    public String equip(ServerPlayer player, ItemStack backpack) {
        Optional<TrinketInventory> inventory = backSlot(player);
        if (inventory.isEmpty()) return "the player has no Trinkets '" + GROUP + "/" + SLOT + "' slot";
        if (inventory.get().getContainerSize() < 1) return "the Trinkets '" + GROUP + "/" + SLOT + "' slot has no room";
        inventory.get().setItem(0, backpack);
        return null;
    }

    @Override
    public ItemStack get(ServerPlayer player) {
        return backSlot(player).map(inventory -> inventory.getItem(0)).orElse(ItemStack.EMPTY);
    }

    private static Optional<TrinketInventory> backSlot(ServerPlayer player) {
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> component.getInventory().getOrDefault(GROUP, Map.of()).get(SLOT));
    }
}
