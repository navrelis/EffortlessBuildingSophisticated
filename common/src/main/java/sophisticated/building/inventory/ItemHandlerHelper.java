package sophisticated.building.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.platform.Services;

public final class ItemHandlerHelper {
    private ItemHandlerHelper() {
    }

    /** Puts the stack into the player's inventory, dropping what does not fit (see {@link Services#PLATFORM}). */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        Services.PLATFORM.giveItemToPlayer(player, stack);
    }
}
