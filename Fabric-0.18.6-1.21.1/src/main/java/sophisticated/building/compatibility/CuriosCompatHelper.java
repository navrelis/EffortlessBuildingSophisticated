package sophisticated.building.compatibility;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class CuriosCompatHelper {
    private static Boolean curiosLoaded;

    private CuriosCompatHelper() {
    }

    public static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = FabricLoader.getInstance().isModLoaded("curios") || FabricLoader.getInstance().isModLoaded("trinkets");
        }
        return curiosLoaded;
    }

    public static List<ItemStack> getBackpacksFromCurios(Player player) {
        return List.of();
    }

    public static void forEachCuriosBackpack(Player player, Consumer<ItemStack> backpackConsumer) {
        // No-op.
    }
}
