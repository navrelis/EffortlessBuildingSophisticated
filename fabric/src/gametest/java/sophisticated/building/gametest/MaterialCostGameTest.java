package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.MaterialCost;

import java.util.Arrays;
import java.util.Map;

import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/** The material cost list shows what the server charges: all items of a multi-item state, a merge only the added one. */
public class MaterialCostGameTest {

    @ServerTest(batch = "material_cost")
    public void costCountsEveryItemOfAState(ServerTestHelper helper) {
        BlockState bottom = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        BlockState dbl = bottom.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        // Minecraft 1.16 has no candles: three sea pickles are the multi-item state
        BlockState threePickles = Blocks.SEA_PICKLE.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, false).setValue(BlockStateProperties.PICKLES, 3);

        BlockEntry doubleOnAir = entry(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState(), dbl);
        BlockEntry merge = entry(new BlockPos(1, 0, 0), bottom, dbl);
        BlockEntry pickles = entry(new BlockPos(2, 0, 0), Blocks.AIR.defaultBlockState(), threePickles);
        Map<Item, Integer> costs = MaterialCost.tally(Arrays.asList(doubleOnAir, merge, pickles));

        expectEquals(helper, "oak slabs for a double slab on air (2) plus a slab merge (1)", 3, costs.get(Items.OAK_SLAB));
        expectEquals(helper, "sea pickles for three sea pickles on air", 3, costs.get(Items.SEA_PICKLE));
        helper.succeed();
    }

    private static BlockEntry entry(BlockPos pos, BlockState existing, BlockState target) {
        BlockEntry entry = new BlockEntry(pos, target, target.getBlock().asItem());
        entry.existingBlockState = existing;
        return entry;
    }
}
