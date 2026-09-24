package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.FixedStack;

import static sophisticated.building.gametest.GameTestSupport.*;

/** Survival undo restores whole states and charges all their items, all or nothing (double slab = 2 slabs). */
public class UndoGameTest implements FabricGameTest {

    private static final BlockPos REL = new BlockPos(3, 1, 3);
    private static final int AXE_SLOT = 4;

    //Survival breaking needs a tool for anything with hardness > 0 (no empty-hand fallback), so the player gets an axe
    @GameTest(template = EMPTY_STRUCTURE, batch = "undo_item_counts", timeoutTicks = 200)
    public void doubleSlabUndoChargesTwoSlabs(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        player.getInventory().setItem(AXE_SLOT, new ItemStack(Items.IRON_AXE));
        BlockState dbl = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.setBlock(REL, dbl);
        var undo = SophisticatedBuilding.UNDO_REDO;

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, set(breaking(helper.absolutePos(REL))));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, REL))
                .thenExecute(() -> {
                    expectEquals(helper, "oak slabs from breaking the double slab", 2, count(player, Items.OAK_SLAB));
                    expectEquals(helper, "axe damage", 1, player.getInventory().getItem(AXE_SLOT).getDamageValue());
                    FixedStack<BlockSet> stack = undo.undoStacks.get(player.getUUID());
                    helper.assertTrue(stack != null && !stack.isEmpty(), "The break should be on the undo stack");

                    //Only one slab: the double slab costs two, so undo fails entirely and the set is pushed
                    //back onto the undo stack instead of being lost
                    removeOneSlab(player);
                    expectEquals(helper, "oak slabs before undo", 1, count(player, Items.OAK_SLAB));
                    helper.assertTrue(undo.undo(player), "undo() should find the set");
                    helper.assertBlockPresent(Blocks.AIR, REL);
                    expectEquals(helper, "oak slabs after the failed undo", 1, count(player, Items.OAK_SLAB));
                    helper.assertTrue(!stack.isEmpty(), "The failed undo should stay on the undo stack");

                    //Two slabs: restored as a double slab, both charged
                    player.getInventory().add(new ItemStack(Items.OAK_SLAB, 1));
                    expectEquals(helper, "oak slabs before the second undo", 2, count(player, Items.OAK_SLAB));
                    helper.assertTrue(undo.undo(player), "undo() should find the retried set");
                    expectState(helper, REL, dbl);
                    expectEquals(helper, "oak slabs after the undo", 0, count(player, Items.OAK_SLAB));
                    helper.assertTrue(stack.isEmpty(), "The undo stack should be empty after the successful undo");
                })
                .thenExecute(() -> {
                    config.close();
                    removePlayer(player);
                })
                .thenSucceed();
    }

    private static void removeOneSlab(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.OAK_SLAB)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
