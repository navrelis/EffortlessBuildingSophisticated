package sophisticated.building.gametest;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import sophisticated.building.utilities.InventoryHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/** #2: the single-item removal overload must shrink the existing stack, not drop its data (custom name...). */
public class InventoryHelperGameTest {

    private static final String NAME = "Keepsake Stone";

    @ServerTest
    public void removeFromInventoryKeepsStackData(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            ItemStack named = new ItemStack(Items.STONE, 10);
            named.setHoverName(new TextComponent(NAME));
            int selected = player.inventory.selected;
            player.inventory.setItem(selected, named);

            InventoryHelper.removeFromInventory(player, Items.STONE, 3);

            ItemStack remaining = player.inventory.getItem(selected);
            expectEquals(helper, "stone left in the selected slot", 7, remaining.getCount());
            assertTrue(remaining.hasCustomHoverName() && NAME.equals(remaining.getHoverName().getString()),
                    "The remaining stack should keep its custom name, has " + remaining);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
