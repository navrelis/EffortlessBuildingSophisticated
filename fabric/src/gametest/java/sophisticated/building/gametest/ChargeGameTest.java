package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Survival pays for what is really placed: a placement the server refuses or that changes nothing costs nothing, and a
 * state made of several items (three sea pickles, a double slab) costs all of them, never one. Minecraft 1.16 has no
 * candles (the other branches use three candles): three sea pickles are the multi-item state.
 */
public class ChargeGameTest {

    //Like vanilla, water cannot be placed in an ultra warm dimension: the kelp is refused, so it is neither charged nor dropped
    @ServerTest(batch = "charge")
    public void refusedPlacementIsNotCharged(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        BlockPos pos = new BlockPos(0, 100, 0);
        try (ConfigScope config = ConfigScope.baseline()) {
            nether.getChunk(pos);
            nether.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            // Entity#setLevel: 1.20+ ServerPlayer#setServerLevel
            player.setLevel(nether);
            player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
            player.inventory.setItem(0, new ItemStack(Items.KELP, 1));

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(pos, Blocks.KELP.defaultBlockState())));

            assertTrue(nether.getBlockState(pos).isAir(), "The kelp should not be placed in the nether");
            expectEquals(helper, "kelp left (the refused placement is not charged)", 1, count(player, Items.KELP));
            expectEquals(helper, "kelp items dropped at the refused position", 0,
                    nether.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).size());
        } finally {
            player.setLevel(overworld);
            removePlayer(player);
        }
        helper.succeed();
    }

    //Placing the state that is already there changes nothing, so it costs nothing (grass: 1.20.3+ short grass)
    @ServerTest(batch = "charge")
    public void unchangedPlacementIsNotCharged(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos rel = new BlockPos(3, 2, 3);
            helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
            helper.setBlock(rel, Blocks.GRASS);
            player.inventory.setItem(0, new ItemStack(Items.GRASS, 1));

            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                    set(place(helper.absolutePos(rel), Blocks.GRASS.defaultBlockState())));

            helper.assertBlockPresent(Blocks.GRASS, rel);
            expectEquals(helper, "grass left", 1, count(player, Items.GRASS));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //A set can carry a multi-item state onto air (the client saw a pickle there that is gone by now): all of its items
    //are charged, all or nothing, like undo and redo
    @ServerTest(batch = "charge")
    public void multiItemStateCostsAllItsItems(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockState three = Blocks.SEA_PICKLE.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, false).setValue(BlockStateProperties.PICKLES, 3);
            BlockPos relA = new BlockPos(2, 2, 3);
            BlockPos relB = new BlockPos(5, 2, 3);
            helper.setBlock(relA.below(), Blocks.DIRT);
            helper.setBlock(relB.below(), Blocks.DIRT);

            //Two sea pickles cannot pay for three: nothing placed, nothing charged
            player.inventory.setItem(0, new ItemStack(Items.SEA_PICKLE, 2));
            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(relA), three)));
            helper.assertBlockPresent(Blocks.AIR, relA);
            expectEquals(helper, "sea pickles left after a refused three-pickle block", 2, count(player, Items.SEA_PICKLE));

            //Three sea pickles pay for it
            player.inventory.setItem(1, new ItemStack(Items.SEA_PICKLE, 1));
            SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, set(place(helper.absolutePos(relB), three)));
            expectState(helper, relB, three);
            expectEquals(helper, "sea pickles left after placing a three-pickle block", 0, count(player, Items.SEA_PICKLE));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
