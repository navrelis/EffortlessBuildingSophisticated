package sophisticated.building.gametest;

import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;

import static sophisticated.building.gametest.GameTestSupport.assertTrue;

/** "Filtered by offhand" with a Randomizer Bag in the offhand accepts exactly the bag's blocks, as its description says. */
public class OffhandBagFilterGameTest {

    @ServerTest
    public void bagFilterAcceptsOnlyItsBlocks(ServerTestHelper helper) {
        ItemStack bag = new ItemStack(SophisticatedBuilding.RANDOMIZER_BAG_ITEM.get());
        SophisticatedBuilding.RANDOMIZER_BAG_ITEM.get().getBagInventory(bag).setStackInSlot(0, new ItemStack(Items.STONE, 3));

        assertTrue(CompatHelper.containsBlock(bag, Blocks.STONE), "The bag's stone should be accepted");
        assertTrue(!CompatHelper.containsBlock(bag, Blocks.DIRT), "Dirt is not in the bag and should not be accepted");
        assertTrue(!CompatHelper.containsBlock(bag, Blocks.AIR), "Air is not in the bag and should not be accepted");
        helper.succeed();
    }
}
