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
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Survival replace (placing onto a block that has to be mined first), behind ServerConfig.survivalReplace.enabled.
 * Survival mining never uses the empty hand for a block with hardness > 0 (ToolSelector.select returns -2 without a
 * tool candidate), so stone without a pickaxe, like bedrock, is skipped with nothing mined or consumed.
 */
public class SurvivalReplaceGameTest implements FabricGameTest {

    private static final BlockPos REL = new BlockPos(3, 1, 3);
    private static final int PICKAXE_SLOT = 4;

    //Survival player with 4 dirt in hand and, optionally, an iron pickaxe in another hotbar slot
    private static ServerPlayer builder(GameTestHelper helper, boolean withPickaxe) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 4));
        if (withPickaxe) player.getInventory().setItem(PICKAXE_SLOT, new ItemStack(Items.IRON_PICKAXE));
        return player;
    }

    private static void applyDirt(GameTestHelper helper, ServerPlayer player) {
        SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player,
                set(place(helper.absolutePos(REL), Blocks.DIRT.defaultBlockState())));
    }

    private static void expectNothingUsed(GameTestHelper helper, ServerPlayer player, boolean withPickaxe) {
        expectEquals(helper, "dirt left", 4, count(player, Items.DIRT));
        expectEquals(helper, "cobblestone received", 0, count(player, Items.COBBLESTONE));
        if (withPickaxe) {
            expectEquals(helper, "pickaxe damage", 0, player.getInventory().getItem(PICKAXE_SLOT).getDamageValue());
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void offRejectsOverwrite(GameTestHelper helper) {
        ServerPlayer player = builder(helper, true);
        try (var config = ConfigScope.baseline()) {
            fabricValue(ServerConfig.survivalReplace.enabled).set(false);
            helper.setBlock(REL, Blocks.STONE);

            applyDirt(helper, player);

            helper.assertBlockPresent(Blocks.STONE, REL);
            expectNothingUsed(helper, player, true);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onMinesWithPickaxe(GameTestHelper helper) {
        ServerPlayer player = builder(helper, true);
        try (var config = ConfigScope.baseline()) {
            fabricValue(ServerConfig.survivalReplace.enabled).set(true);
            helper.setBlock(REL, Blocks.STONE);

            applyDirt(helper, player);

            helper.assertBlockPresent(Blocks.DIRT, REL);
            expectEquals(helper, "dirt left", 3, count(player, Items.DIRT));
            expectEquals(helper, "cobblestone received", 1, count(player, Items.COBBLESTONE));
            ItemStack pickaxe = player.getInventory().getItem(PICKAXE_SLOT);
            assertTrue(pickaxe.is(Items.IRON_PICKAXE), "Pickaxe should still be in its slot, found " + pickaxe);
            expectEquals(helper, "pickaxe damage", 1, pickaxe.getDamageValue());
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onSkipsBedrock(GameTestHelper helper) {
        ServerPlayer player = builder(helper, true);
        try (var config = ConfigScope.baseline()) {
            fabricValue(ServerConfig.survivalReplace.enabled).set(true);
            helper.setBlock(REL, Blocks.BEDROCK);

            applyDirt(helper, player);

            helper.assertBlockPresent(Blocks.BEDROCK, REL);
            expectNothingUsed(helper, player, true);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onSkipsStoneWithoutPickaxe(GameTestHelper helper) {
        ServerPlayer player = builder(helper, false);
        try (var config = ConfigScope.baseline()) {
            fabricValue(ServerConfig.survivalReplace.enabled).set(true);
            helper.setBlock(REL, Blocks.STONE);

            applyDirt(helper, player);

            helper.assertBlockPresent(Blocks.STONE, REL);
            expectNothingUsed(helper, player, false);
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //The real network path: placeBlocksDelayed waits for the mining time (iron pickaxe on stone: 8 ticks), then tick() applies
    @GameTest(template = EMPTY_STRUCTURE, batch = "survival_replace_delayed", timeoutTicks = 200)
    public void onDelayedWaitsForMining(GameTestHelper helper) {
        ServerPlayer player = builder(helper, true);
        var config = ConfigScope.baseline();
        fabricValue(ServerConfig.survivalReplace.enabled).set(true);
        helper.setBlock(REL, Blocks.STONE);
        long start = helper.getLevel().getGameTime();

        SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player,
                set(place(helper.absolutePos(REL), Blocks.DIRT.defaultBlockState())), start);

        helper.assertBlockPresent(Blocks.STONE, REL);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.DIRT, REL))
                .thenExecute(() -> {
                    try {
                        long elapsed = helper.getLevel().getGameTime() - start;
                        assertTrue(elapsed >= 8, "Replace should wait for the 8 mining ticks, placed after " + elapsed);
                        expectEquals(helper, "dirt left", 3, count(player, Items.DIRT));
                        expectEquals(helper, "cobblestone received", 1, count(player, Items.COBBLESTONE));
                        expectEquals(helper, "pickaxe damage", 1, player.getInventory().getItem(PICKAXE_SLOT).getDamageValue());
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }
}
