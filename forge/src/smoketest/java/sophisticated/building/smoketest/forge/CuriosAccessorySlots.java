package sophisticated.building.smoketest.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import sophisticated.building.smoketest.backpack.SmokeAccessorySlots;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import javax.annotation.Nullable;
import java.util.Optional;

/** The Curios "back" slot Sophisticated Backpacks registers, when Curios is in the runtime (runSmokeClient/Server). */
public final class CuriosAccessorySlots implements SmokeAccessorySlots {

    private static final String SLOT = "back";

    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded("curios");
    }

    @Override
    public String describe() {
        return "Curios '" + SLOT + "' slot";
    }

    @Nullable
    @Override
    public String equip(ServerPlayer player, ItemStack backpack) {
        Optional<ICurioStacksHandler> handler = backSlot(player);
        if (!handler.isPresent()) return "the player has no Curios '" + SLOT + "' slot";
        if (handler.get().getSlots() < 1) return "the Curios '" + SLOT + "' slot has no room";
        handler.get().getStacks().setStackInSlot(0, backpack);
        return null;
    }

    @Override
    public ItemStack get(ServerPlayer player) {
        return backSlot(player).map(handler -> handler.getStacks().getStackInSlot(0)).orElse(ItemStack.EMPTY);
    }

    // Curios 4 (Forge 1.16.5) hands the inventory out as a capability LazyOptional
    private static Optional<ICurioStacksHandler> backSlot(ServerPlayer player) {
        return CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve().flatMap(inventory -> inventory.getStacksHandler(SLOT));
    }
}
