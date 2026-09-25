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
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BlockSet;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * skipFirst: the client says vanilla handled the first block. The server honours it only while the player is
 * "like vanilla" (no build mode, no Quick Replace); otherwise vanilla was cancelled and the mod places it too.
 * The flag is resolved in placeBlocksDelayed, so these tests use the real delayed path and tick().
 */
public class SkipFirstGameTest implements FabricGameTest {

    private static final BlockPos A = new BlockPos(1, 1, 3);
    private static final BlockPos B = new BlockPos(3, 1, 3);
    private static final BlockPos C = new BlockPos(5, 1, 3);

    private static BlockSet stoneRow(GameTestHelper helper) {
        return set(true,
                place(helper.absolutePos(A), Blocks.STONE.defaultBlockState()),
                place(helper.absolutePos(B), Blocks.STONE.defaultBlockState()),
                place(helper.absolutePos(C), Blocks.STONE.defaultBlockState()));
    }

    private static void placeNow(GameTestHelper helper, ServerPlayer player) {
        SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, stoneRow(helper), helper.getLevel().getGameTime());
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "skip_first", timeoutTicks = 100)
    public void likeVanillaSkipsFirst(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        GameTestSupport.ConfigScope config = ConfigScope.baseline();
        player.inventory.setItem(0, new ItemStack(Items.STONE, 3));
        assertTrue(ServerBuildState.isLikeVanilla(player), "Player should start like vanilla");

        placeNow(helper, player);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.STONE, B))
                .thenIdle(2)
                .thenExecute(() -> {
                    try {
                        helper.assertBlockPresent(Blocks.AIR, A);
                        helper.assertBlockPresent(Blocks.STONE, C);
                        expectEquals(helper, "stone left (the skipped first block is not charged)", 1, count(player, Items.STONE));
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "skip_first", timeoutTicks = 100)
    public void quickReplacingPlacesFirst(GameTestHelper helper) {
        //Quick Replace needs canReplaceBlocks: creative always, survival only with survival replace enabled
        ServerPlayer player = spawnPlayer(helper, GameType.CREATIVE);
        GameTestSupport.ConfigScope config = ConfigScope.baseline();
        ServerBuildState.setIsQuickReplacing(player, true);
        assertTrue(!ServerBuildState.isLikeVanilla(player), "Quick replacing player should not be like vanilla");

        placeNow(helper, player);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, A);
                    helper.assertBlockPresent(Blocks.STONE, B);
                    helper.assertBlockPresent(Blocks.STONE, C);
                })
                .thenExecute(() -> {
                    config.close();
                    removePlayer(player);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "skip_first", timeoutTicks = 100)
    public void buildModePlacesFirst(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        GameTestSupport.ConfigScope config = ConfigScope.baseline();
        player.inventory.setItem(0, new ItemStack(Items.STONE, 3));
        ServerBuildState.setIsUsingBuildMode(player, true);

        placeNow(helper, player);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.STONE, A);
                    helper.assertBlockPresent(Blocks.STONE, B);
                    helper.assertBlockPresent(Blocks.STONE, C);
                })
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "stone left", 0, count(player, Items.STONE));
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }
}
