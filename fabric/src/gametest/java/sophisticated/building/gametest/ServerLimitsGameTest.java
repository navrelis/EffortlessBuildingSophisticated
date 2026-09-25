package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.systems.ServerBlockPlacer;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sophisticated.building.smoketest.servertest.ServerTest;
import sophisticated.building.smoketest.servertest.ServerTestHelper;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * The server applies the player's power level limits to build requests itself (a modified client can send anything):
 * a start position out of reach and a build mode extent over the blocks-per-axis limit are rejected, more blocks than
 * the player may place at once are cut to the limit. Survival power level 0 with the default common config: placement
 * reach 0 (the vanilla interaction range applies), 8 blocks per axis, 128 blocks at once. The players hold no blocks, so
 * nothing is placed; the tests look at what the server scheduled.
 */
public class ServerLimitsGameTest {

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();

    @ServerTest(batch = "server_limits")
    public void legitRequestIsScheduled(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
            List<BlockEntry> line = new ArrayList<>();
            for (int i = 0; i < 5; i++) line.add(place(start.east(i), STONE));
            SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(start, start.east(4), line), gameTime(helper));
            expectEquals(helper, "blocks scheduled for a 5 block line next to the player", 5, scheduled(player));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @ServerTest(batch = "server_limits")
    public void startOutOfReachIsRejected(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos far = player.blockPosition().above(60);
            SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(far, far, Arrays.asList(place(far, STONE))), gameTime(helper));
            expectEquals(helper, "blocks scheduled for a start position 60 blocks away (reach " + CommonConfig.reach.level0.get() + ")",
                    0, scheduled(player));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @ServerTest(batch = "server_limits")
    public void extentOverTheAxisLimitIsRejected(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
            //A line from the start 20 blocks along x (limit 8); only its near end is sent, like a trimmed crafted request
            BlockPos end = start.east(19);
            SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(start, end, Arrays.asList(place(start, STONE), place(start.east(1), STONE))),
                    gameTime(helper));
            expectEquals(helper, "blocks scheduled for a 20 block extent (limit " + CommonConfig.maxBlocksPerAxis.level0.get() + " per axis)",
                    0, scheduled(player));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @ServerTest(batch = "server_limits")
    public void blocksFarFromTheBuildAreRejected(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
            //A legit start, plus a block no build mode or modifier of this player could reach
            BlockPos far = player.blockPosition().above(100);
            SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(start, start, Arrays.asList(place(start, STONE), place(far, STONE))),
                    gameTime(helper));
            expectEquals(helper, "blocks scheduled for a set with a block 100 blocks away", 0, scheduled(player));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @ServerTest(batch = "server_limits")
    public void tooManyBlocksAreCappedToTheLimit(ServerTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try (ConfigScope config = ConfigScope.baseline()) {
            BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
            List<BlockEntry> floor = new ArrayList<>();
            //A 200 block floor within the player's reach, sent as one set (limit 128 at once)
            for (int x = 0; x < 8; x++) {
                for (int z = 0; z < 8; z++) {
                    for (int y = 0; y < 4; y++) {
                        if (floor.size() < 200) floor.add(place(start.offset(x, y, z), STONE));
                    }
                }
            }
            SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(start, start, floor), gameTime(helper));
            expectEquals(helper, "blocks scheduled for a " + floor.size() + " block set (limit "
                    + CommonConfig.maxBlocksPlacedAtOnce.level0.get() + " at once)", CommonConfig.maxBlocksPlacedAtOnce.level0.get(), scheduled(player));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    private static BlockSet set(BlockPos first, BlockPos last, List<BlockEntry> entries) {
        return new BlockSet(entries, first, last, false);
    }

    private static long gameTime(ServerTestHelper helper) {
        return helper.getLevel().getGameTime();
    }

    /** Blocks of this player's sets the server has scheduled (not yet applied). */
    private static int scheduled(ServerPlayer player) {
        int count = 0;
        for (ServerBlockPlacer.DelayedEntry entry : SophisticatedBuilding.SERVER_BLOCK_PLACER.getDelayedEntries()) {
            if (entry.player() == player) count += entry.blocks().size();
        }
        return count;
    }
}
