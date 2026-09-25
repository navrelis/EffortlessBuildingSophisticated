package sophisticated.building.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import sophisticated.building.SophisticatedBuilding;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Survival pays for what is really placed: a placement the server refuses or that changes nothing costs nothing, and a
 * state made of several items (three candles, a double slab) costs all of them, never one.
 */
public class ChargeGameTest implements FabricGameTest {

    //Like vanilla, water cannot be placed in an ultra warm dimension: the kelp is refused, so it is neither charged nor dropped
    @GameTest(template = EMPTY_STRUCTURE)
    public void refusedPlacementIsNotCharged(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        BlockPos pos = new BlockPos(0, 100, 0);
        try (var config = ConfigScope.baseline()) {
            nether.getChunk(pos);
            nether.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            player.setServerLevel(nether);
            player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
            player.getInventory().setItem(0, new ItemStack(Items.KELP, 1));

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(pos, Blocks.KELP.defaultBlockState())));

            helper.assertTrue(nether.getBlockState(pos).isAir(), "The kelp should not be placed in the nether");
            expectEquals(helper, "kelp left (the refused placement is not charged)", 1, count(player, Items.KELP));
            expectEquals(helper, "kelp items dropped at the refused position", 0,
                    nether.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).size());
        } finally {
            player.setServerLevel(overworld);
            removePlayer(player);
        }
        helper.succeed();
    }

    //Placing the state that is already there changes nothing, so it costs nothing
    @GameTest(template = EMPTY_STRUCTURE)
    public void unchangedPlacementIsNotCharged(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (var config = ConfigScope.baseline()) {
            BlockPos rel = new BlockPos(3, 2, 3);
            helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
            helper.setBlock(rel, Blocks.GRASS);
            player.getInventory().setItem(0, new ItemStack(Items.GRASS, 1));

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.GRASS.defaultBlockState())));

            helper.assertBlockPresent(Blocks.GRASS, rel);
            expectEquals(helper, "grass left", 1, count(player, Items.GRASS));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //A set can carry a multi-item state onto air (the client saw a candle there that is gone by now): all of its items
    //are charged, all or nothing, like undo and redo
    @GameTest(template = EMPTY_STRUCTURE)
    public void multiItemStateCostsAllItsItems(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (var config = ConfigScope.baseline()) {
            BlockState three = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3);
            BlockPos relA = new BlockPos(2, 2, 3);
            BlockPos relB = new BlockPos(5, 2, 3);
            helper.setBlock(relA.below(), Blocks.DIRT);
            helper.setBlock(relB.below(), Blocks.DIRT);

            //Two candles cannot pay for three: nothing placed, nothing charged
            player.getInventory().setItem(0, new ItemStack(Items.CANDLE, 2));
            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(relA), three)));
            helper.assertBlockPresent(Blocks.AIR, relA);
            expectEquals(helper, "candles left after a refused three-candle block", 2, count(player, Items.CANDLE));

            //Three candles pay for it
            player.getInventory().setItem(1, new ItemStack(Items.CANDLE, 1));
            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(relB), three)));
            expectState(helper, relB, three);
            expectEquals(helper, "candles left after placing a three-candle block", 0, count(player, Items.CANDLE));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
