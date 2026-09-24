package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import sophisticated.building.utilities.InventoryHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/** #2: the single-item removal overload must shrink the existing stack, not drop its data (custom name...). */
public class InventoryHelperGameTest implements FabricGameTest {

    private static final String NAME = "Keepsake Stone";

    @GameTest(template = EMPTY_STRUCTURE)
    public void removeFromInventoryKeepsStackData(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            ItemStack named = new ItemStack(Items.STONE, 10);
            named.setHoverName(Component.literal(NAME));
            int selected = player.getInventory().selected;
            player.getInventory().setItem(selected, named);

            InventoryHelper.removeFromInventory(player, Items.STONE, 3);

            ItemStack remaining = player.getInventory().getItem(selected);
            expectEquals(helper, "stone left in the selected slot", 7, remaining.getCount());
            helper.assertTrue(remaining.hasCustomHoverName() && NAME.equals(remaining.getHoverName().getString()),
                    "The remaining stack should keep its custom name, has " + remaining);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
