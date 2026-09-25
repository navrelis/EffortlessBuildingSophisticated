package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/** "Filtered by offhand" with a Randomizer Bag in the offhand accepts exactly the bag's blocks, as its description says. */
public class OffhandBagFilterGameTest {

    @GameTest
    public void bagFilterAcceptsOnlyItsBlocks(GameTestHelper helper) {
        ItemStack bag = new ItemStack(SophisticatedBuilding.RANDOMIZER_BAG_ITEM.get());
        SophisticatedBuilding.RANDOMIZER_BAG_ITEM.get().getBagInventory(bag).setStackInSlot(0, new ItemStack(Items.STONE, 3));

        expectTrue(helper, CompatHelper.containsBlock(bag, Blocks.STONE), "The bag's stone should be accepted");
        expectTrue(helper, !(CompatHelper.containsBlock(bag, Blocks.DIRT)), "Dirt is not in the bag and should not be accepted");
        expectTrue(helper, !(CompatHelper.containsBlock(bag, Blocks.AIR)), "Air is not in the bag and should not be accepted");
        helper.succeed();
    }
}
