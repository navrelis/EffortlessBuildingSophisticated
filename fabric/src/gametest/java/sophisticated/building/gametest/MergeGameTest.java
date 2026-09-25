package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import sophisticated.building.SophisticatedBuilding;

import static sophisticated.building.gametest.GameTestSupport.*;

/** Survival placement onto the same block: only a vanilla-style one-step merge is placed, for one item. */
public class MergeGameTest implements FabricGameTest {

    private static final BlockPos REL = new BlockPos(3, 1, 3);

    @GameTest(template = EMPTY_STRUCTURE)
    public void slabMergesToDouble(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.OAK_SLAB, 3));
            BlockState bottom = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            BlockState dbl = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
            helper.setBlock(REL, bottom);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(REL), dbl)));

            expectState(helper, REL, dbl);
            expectEquals(helper, "oak slabs left", 2, count(player, Items.OAK_SLAB));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void stairsOtherFacingUnchanged(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (GameTestSupport.ConfigScope config = ConfigScope.baseline()) {
            player.getInventory().setItem(0, new ItemStack(Items.OAK_STAIRS, 3));
            BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            BlockState east = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            helper.setBlock(REL, north);
            BlockState before = helper.getBlockState(REL);

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(REL), east)));

            expectState(helper, REL, before);
            expectEquals(helper, "oak stairs left", 3, count(player, Items.OAK_STAIRS));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
