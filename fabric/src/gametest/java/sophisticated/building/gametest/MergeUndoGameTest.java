package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;

import java.util.List;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Undo of a merge (one more item placed onto the same block, like vanilla) puts the block back to its old state and
 * gives back exactly the item the merge charged; redo charges it again. No loss, no dupe, for every merge kind and
 * whatever the survival replace setting (the block is never mined).
 */
public class MergeUndoGameTest implements FabricGameTest {

    private record Merge(String name, Item item, BlockState before, BlockState after) {
    }

    private static List<Merge> merges() {
        BlockState candle = Blocks.CANDLE.defaultBlockState();
        BlockState pickle = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        BlockState egg = Blocks.TURTLE_EGG.defaultBlockState();
        BlockState snow = Blocks.SNOW.defaultBlockState();
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        return List.of(
                new Merge("snow layers", Items.SNOW, snow, snow.setValue(BlockStateProperties.LAYERS, 2)),
                new Merge("oak slab", Items.OAK_SLAB, slab, slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE)),
                new Merge("candles", Items.CANDLE, candle, candle.setValue(BlockStateProperties.CANDLES, 2)),
                new Merge("sea pickles", Items.SEA_PICKLE, pickle, pickle.setValue(BlockStateProperties.PICKLES, 2)),
                new Merge("turtle eggs", Items.TURTLE_EGG, egg, egg.setValue(BlockStateProperties.EGGS, 2)));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void survivalUndoRefundsTheMergedItem(GameTestHelper helper) {
        runMerges(helper, false);
    }

    //Survival replace on must not change it: undo of a merge never mines (turtle eggs would drop nothing)
    @GameTest(template = EMPTY_STRUCTURE)
    public void survivalUndoRefundsTheMergedItemWithReplaceOn(GameTestHelper helper) {
        runMerges(helper, true);
    }

    private static void runMerges(GameTestHelper helper, boolean replaceOn) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (var config = ConfigScope.baseline()) {
            if (replaceOn) fabricValue(ServerConfig.survivalReplace.enabled).set(true);
            var placer = SophisticatedBuilding.SERVER_BLOCK_PLACER;
            var undoRedo = SophisticatedBuilding.UNDO_REDO;
            List<Merge> merges = merges();
            for (int i = 0; i < merges.size(); i++) {
                Merge merge = merges.get(i);
                BlockPos rel = new BlockPos(1 + i, 2, 3);
                helper.setBlock(rel.below(), Blocks.DIRT);
                helper.setBlock(rel, merge.before());
                undoRedo.clear(player);
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(merge.item(), 1));

                placer.applyBlockSet(player, set(place(helper.absolutePos(rel), merge.after())));
                expectState(helper, rel, merge.after());
                expectEquals(helper, merge.name() + " after the merge", 0, count(player, merge.item()));

                assertTrue(undoRedo.undo(player), merge.name() + ": the merge should be on the undo stack");
                expectState(helper, rel, merge.before());
                expectEquals(helper, merge.name() + " given back by undo", 1, count(player, merge.item()));

                assertTrue(undoRedo.redo(player), merge.name() + ": the undo should be on the redo stack");
                expectState(helper, rel, merge.after());
                expectEquals(helper, merge.name() + " charged again by redo", 0, count(player, merge.item()));

                assertTrue(undoRedo.undo(player), merge.name() + ": the redo should be on the undo stack");
                expectState(helper, rel, merge.before());
                expectEquals(helper, merge.name() + " given back by the second undo", 1, count(player, merge.item()));
            }
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //Creative: the block goes back, nothing is given
    @GameTest(template = EMPTY_STRUCTURE)
    public void creativeUndoRestoresWithoutRefund(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        try (var config = ConfigScope.baseline()) {
            BlockPos rel = new BlockPos(3, 2, 3);
            BlockState one = Blocks.CANDLE.defaultBlockState();
            BlockState two = one.setValue(BlockStateProperties.CANDLES, 2);
            helper.setBlock(rel.below(), Blocks.DIRT);
            helper.setBlock(rel, one);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(rel), two)));
            expectState(helper, rel, two);
            assertTrue(SophisticatedBuilding.UNDO_REDO.undo(player), "the merge should be on the undo stack");
            expectState(helper, rel, one);
            expectEquals(helper, "candles in the creative inventory", 0, count(player, Items.CANDLE));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
