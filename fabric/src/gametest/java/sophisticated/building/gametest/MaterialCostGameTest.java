package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.MaterialCost;

import java.util.List;
import java.util.Map;

import static sophisticated.building.gametest.GameTestSupport.*;

/** The material cost list shows what the server charges: all items of a multi-item state, a merge only the added one. */
public class MaterialCostGameTest {

    @GameTest
    public void costCountsEveryItemOfAState(GameTestHelper helper) {
        BlockState bottom = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        BlockState dbl = bottom.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        BlockState threeCandles = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3);

        BlockEntry doubleOnAir = entry(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState(), dbl);
        BlockEntry merge = entry(new BlockPos(1, 0, 0), bottom, dbl);
        BlockEntry candles = entry(new BlockPos(2, 0, 0), Blocks.AIR.defaultBlockState(), threeCandles);
        Map<Item, Integer> costs = MaterialCost.tally(List.of(doubleOnAir, merge, candles));

        expectEquals(helper, "oak slabs for a double slab on air (2) plus a slab merge (1)", 3, costs.get(Items.OAK_SLAB));
        expectEquals(helper, "candles for three candles on air", 3, costs.get(Items.CANDLE));
        helper.succeed();
    }

    private static BlockEntry entry(BlockPos pos, BlockState existing, BlockState target) {
        BlockEntry entry = new BlockEntry(pos, target, target.getBlock().asItem());
        entry.existingBlockState = existing;
        return entry;
    }
}
