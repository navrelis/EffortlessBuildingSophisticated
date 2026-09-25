package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
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
import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import sophisticated.building.systems.UndoRedo;

import java.util.Arrays;
import java.util.List;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Undo of a merge (one more item placed onto the same block, like vanilla) puts the block back to its old state and
 * gives back exactly the item the merge charged; redo charges it again. No loss, no dupe, for every merge kind and
 * whatever the survival replace setting (the block is never mined). Minecraft 1.16.3 merges: snow layers, slabs, sea
 * pickles and turtle eggs (no candles before 1.17, no pink petals before 1.19.4).
 */
public class MergeUndoGameTest {

    private static final class Merge {
        final String name;
        final Item item;
        final BlockState before;
        final BlockState after;

        Merge(String name, Item item, BlockState before, BlockState after) {
            this.name = name;
            this.item = item;
            this.before = before;
            this.after = after;
        }
    }

    private static List<Merge> merges() {
        BlockState pickle = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
        BlockState egg = Blocks.TURTLE_EGG.defaultBlockState();
        BlockState snow = Blocks.SNOW.defaultBlockState();
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        return Arrays.asList(
                new Merge("snow layers", Items.SNOW, snow, snow.setValue(BlockStateProperties.LAYERS, 2)),
                new Merge("oak slab", Items.OAK_SLAB, slab, slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE)),
                new Merge("sea pickles", Items.SEA_PICKLE, pickle, pickle.setValue(BlockStateProperties.PICKLES, 2)),
                new Merge("turtle eggs", Items.TURTLE_EGG, egg, egg.setValue(BlockStateProperties.EGGS, 2)));
    }

    @ServerTest
    public void survivalUndoRefundsTheMergedItem(ServerTestHelper helper) {
        runMerges(helper, false);
    }

    //Survival replace on must not change it: undo of a merge never mines (turtle eggs would drop nothing)
    @ServerTest
    public void survivalUndoRefundsTheMergedItemWithReplaceOn(ServerTestHelper helper) {
        runMerges(helper, true);
    }

    private static void runMerges(ServerTestHelper helper, boolean replaceOn) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            if (replaceOn) fabricValue(ServerConfig.survivalReplace.enabled).set(true);
            UndoRedo undoRedo = SophisticatedBuilding.UNDO_REDO;
            List<Merge> merges = merges();
            for (int i = 0; i < merges.size(); i++) {
                Merge merge = merges.get(i);
                BlockPos rel = new BlockPos(1 + i, 2, 3);
                helper.setBlock(rel.below(), Blocks.DIRT);
                helper.setBlock(rel, merge.before);
                undoRedo.clear(player);
                player.inventory.clearContent();
                player.inventory.setItem(0, new ItemStack(merge.item, 1));

                SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(rel), merge.after)));
                expectState(helper, rel, merge.after);
                expectEquals(helper, merge.name + " after the merge", 0, count(player, merge.item));

                assertTrue(undoRedo.undo(player), merge.name + ": the merge should be on the undo stack");
                expectState(helper, rel, merge.before);
                expectEquals(helper, merge.name + " given back by undo", 1, count(player, merge.item));

                assertTrue(undoRedo.redo(player), merge.name + ": the undo should be on the redo stack");
                expectState(helper, rel, merge.after);
                expectEquals(helper, merge.name + " charged again by redo", 0, count(player, merge.item));

                assertTrue(undoRedo.undo(player), merge.name + ": the redo should be on the undo stack");
                expectState(helper, rel, merge.before);
                expectEquals(helper, merge.name + " given back by the second undo", 1, count(player, merge.item));
            }
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //Creative: the block goes back, nothing is given
    @ServerTest
    public void creativeUndoRestoresWithoutRefund(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            BlockPos rel = new BlockPos(3, 2, 3);
            BlockState one = Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false);
            BlockState two = one.setValue(BlockStateProperties.PICKLES, 2);
            helper.setBlock(rel.below(), Blocks.DIRT);
            helper.setBlock(rel, one);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(rel), two)));
            expectState(helper, rel, two);
            assertTrue(SophisticatedBuilding.UNDO_REDO.undo(player), "the merge should be on the undo stack");
            expectState(helper, rel, one);
            expectEquals(helper, "sea pickles in the creative inventory", 0, count(player, Items.SEA_PICKLE));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
