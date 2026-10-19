package sophisticated.building.gametest;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.platform.Services;
import sophisticated.building.systems.BalancedBreaking;
import sophisticated.building.systems.ServerBlockPlacer;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Balanced survival breaking (ServerConfig.balancedBreaking): a survival mass break of 2+ blocks costs the tool the
 * durability multiplier per block and the balanced exhaustion, is refused while the player is too hungry or cooling
 * down, and starts a cooldown kept in the player's saved data (restart, respawn). One-block operations and creative
 * players are exempt; with the option off the 5.0.2 cost applies. Every test spans several ticks (survival breaking is
 * delayed) and changes the global config, so each runs in its own batch.
 */
public class BalancedBreakingGameTest implements FabricGameTest {

    private static final int PICKAXE_SLOT = 0;
    private static final float EPSILON = 1.0e-4f;

    //Survival (or creative) player with an iron pickaxe in the selected slot, full food, no exhaustion
    private static ServerPlayer miner(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = spawnPlayer(helper, gameType);
        player.getInventory().setItem(PICKAXE_SLOT, new ItemStack(Items.IRON_PICKAXE));
        player.getFoodData().setFoodLevel(20);
        setExhaustion(player, 0f);
        return player;
    }

    //Stone at (1..count, 1, 1) of the structure, returned as absolute positions
    private static List<BlockPos> stoneRow(GameTestHelper helper, int x0, int count) {
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockPos relative = new BlockPos(x0 + i, 1, 1);
            helper.setBlock(relative, Blocks.STONE);
            positions.add(helper.absolutePos(relative));
        }
        return positions;
    }

    private static BlockSet breakSet(List<BlockPos> positions) {
        BlockEntry[] entries = new BlockEntry[positions.size()];
        for (int i = 0; i < entries.length; i++) entries[i] = breaking(positions.get(i));
        return set(entries);
    }

    private static boolean allAre(GameTestHelper helper, List<BlockPos> positions, Block block) {
        for (BlockPos pos : positions) {
            if (!helper.getLevel().getBlockState(pos).is(block)) return false;
        }
        return true;
    }

    private static void expectAll(GameTestHelper helper, List<BlockPos> positions, Block block) {
        helper.assertTrue(allAre(helper, positions, block), "Expected " + block + " at every position of " + positions);
    }

    private static int airCount(GameTestHelper helper, List<BlockPos> positions) {
        int count = 0;
        for (BlockPos pos : positions) {
            if (helper.getLevel().getBlockState(pos).isAir()) count++;
        }
        return count;
    }

    private static int pickaxeDamage(ServerPlayer player) {
        return player.getInventory().getItem(PICKAXE_SLOT).getDamageValue();
    }

    //FoodData has no exhaustion accessors since 1.21.2: read and write it through its save data
    private static float exhaustion(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(tag);
        return tag.getFloat("foodExhaustionLevel");
    }

    private static void setExhaustion(ServerPlayer player, float value) {
        CompoundTag tag = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(tag);
        tag.putFloat("foodExhaustionLevel", value);
        player.getFoodData().readAdditionalSaveData(tag);
    }

    private static void expectExhaustion(GameTestHelper helper, ServerPlayer player, double expected) {
        float actual = exhaustion(player);
        helper.assertTrue(Math.abs(actual - expected) < EPSILON, "exhaustion: expected " + expected + " but was " + actual);
    }

    private static boolean hasCooldownData(ServerPlayer player) {
        return Services.PLATFORM.getPersistentData(player).contains(BalancedBreaking.DATA_KEY);
    }

    private static int scheduled(ServerPlayer player) {
        int count = 0;
        for (ServerBlockPlacer.DelayedEntry entry : SophisticatedBuilding.SERVER_BLOCK_PLACER.getDelayedEntries()) {
            if (entry.player() == player) count++;
        }
        return count;
    }

    private static void enable() {
        fabricValue(ServerConfig.balancedBreaking.enabled).set(true);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_cost", timeoutTicks = 200)
    public void balancedCostsMultipliedDurabilityAndExhaustion(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        int multiplier = ServerConfig.balancedBreaking.durabilityMultiplier.get();
        double exhaustion = ServerConfig.balancedBreaking.exhaustionPerBlock.get();
        List<BlockPos> row = stoneRow(helper, 1, 3);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(row));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, row, Blocks.AIR))
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "pickaxe damage (3 blocks x " + multiplier + ")", 3 * multiplier, pickaxeDamage(player));
                        expectExhaustion(helper, player, 3 * exhaustion);
                        expectEquals(helper, "cobblestone received", 3, count(player, Items.COBBLESTONE));
                        long[] stored = BalancedBreaking.load(player).toArray();
                        expectEquals(helper, "stored cooldown entries (end + 1 use)", 2, stored.length);
                        helper.assertTrue(BalancedBreaking.remainingMillis(player) > 0, "the operation should start a cooldown");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_off", timeoutTicks = 200)
    public void offKeepsTheNormalCost(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        double exhaustion = ServerConfig.survivalBreaking.exhaustionPerBlock.get();
        List<BlockPos> row = stoneRow(helper, 1, 3);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(row));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, row, Blocks.AIR))
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "pickaxe damage (one per block)", 3, pickaxeDamage(player));
                        expectExhaustion(helper, player, 3 * exhaustion);
                        helper.assertFalse(hasCooldownData(player), "no cooldown is stored with balanced breaking off");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_hunger", timeoutTicks = 200)
    public void tooHungryIsRefused(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        player.getFoodData().setFoodLevel(ServerConfig.balancedBreaking.minFoodLevel.get() - 1);
        List<BlockPos> row = stoneRow(helper, 1, 3);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(row));
        expectEquals(helper, "operations scheduled for a too hungry player", 0, scheduled(player));

        helper.startSequence()
                .thenIdle(50)
                .thenExecute(() -> {
                    try {
                        expectAll(helper, row, Blocks.STONE);
                        expectEquals(helper, "pickaxe damage", 0, pickaxeDamage(player));
                        helper.assertFalse(hasCooldownData(player), "a refused operation starts no cooldown");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_cooldown", timeoutTicks = 200)
    public void secondOperationIsRefusedDuringTheCooldown(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        int multiplier = ServerConfig.balancedBreaking.durabilityMultiplier.get();
        List<BlockPos> first = stoneRow(helper, 1, 2);
        List<BlockPos> second = new ArrayList<>();

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(first));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, first, Blocks.AIR))
                .thenExecute(() -> {
                    second.addAll(stoneRow(helper, 1, 3));
                    long[] before = BalancedBreaking.load(player).toArray();
                    SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(second));
                    expectEquals(helper, "operations scheduled during the cooldown", 0, scheduled(player));
                    helper.assertTrue(Arrays.equals(before, BalancedBreaking.load(player).toArray()),
                            "a refused operation is not counted as a use");
                })
                .thenIdle(50)
                .thenExecute(() -> {
                    try {
                        expectAll(helper, second, Blocks.STONE);
                        expectEquals(helper, "pickaxe damage (only the first operation)", 2 * multiplier, pickaxeDamage(player));
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    //A restart: the player is written to its save and read back into a new player object; death and respawn build a
    //new player object from the old one (ServerPlayer#restoreFrom)
    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_persistence", timeoutTicks = 200)
    public void cooldownSurvivesARestartAndRespawn(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        long baseMillis = ServerConfig.balancedBreaking.cooldownBaseSeconds.get() * 1000L;
        List<BlockPos> row = stoneRow(helper, 1, 2);
        long before = System.currentTimeMillis();

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(row));

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, row, Blocks.AIR))
                .thenExecute(() -> {
                    try {
                        long[] stored = BalancedBreaking.load(player).toArray();
                        //Real time (epoch millis), so it also runs while the player is offline or the server is stopped
                        helper.assertTrue(stored[0] >= before + baseMillis && stored[0] <= System.currentTimeMillis() + baseMillis,
                                "the cooldown end should be the request's real time + " + baseMillis + " ms, was " + stored[0]);

                        CompoundTag saved = player.saveWithoutId(new CompoundTag());
                        saved.remove("UUID");
                        ServerPlayer restored = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                                new GameProfile(UUID.randomUUID(), "sb-restored"), ClientInformation.createDefault());
                        restored.load(saved);
                        helper.assertTrue(Arrays.equals(stored, BalancedBreaking.load(restored).toArray()),
                                "cooldown after a restart: expected " + Arrays.toString(stored) + " but was "
                                        + Arrays.toString(BalancedBreaking.load(restored).toArray()));
                        helper.assertTrue(BalancedBreaking.remainingMillis(restored) > 0, "still cooling down after a restart");

                        ServerPlayer respawned = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), player.getGameProfile(),
                                ClientInformation.createDefault());
                        respawned.restoreFrom(player, false);
                        helper.assertTrue(Arrays.equals(stored, BalancedBreaking.load(respawned).toArray()),
                                "cooldown after respawn: expected " + Arrays.toString(stored) + " but was "
                                        + Arrays.toString(BalancedBreaking.load(respawned).toArray()));
                        helper.assertTrue(BalancedBreaking.remainingMillis(respawned) > 0, "still cooling down after respawn");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    //Two one-block breaks right after each other: both are vanilla-like (cost 1, normal exhaustion, no cooldown)
    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_one_block", timeoutTicks = 200)
    public void oneBlockOperationsAreExempt(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        double exhaustion = ServerConfig.survivalBreaking.exhaustionPerBlock.get();
        List<BlockPos> first = stoneRow(helper, 1, 1);
        List<BlockPos> second = stoneRow(helper, 3, 1);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(first));
        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(second));
        expectEquals(helper, "one-block operations scheduled", 2, scheduled(player));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    expectAll(helper, first, Blocks.AIR);
                    expectAll(helper, second, Blocks.AIR);
                })
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "pickaxe damage (one per block)", 2, pickaxeDamage(player));
                        expectExhaustion(helper, player, 2 * exhaustion);
                        helper.assertFalse(hasCooldownData(player), "one-block operations start no cooldown");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    //Creative breaking is applied at once, so the second operation right after the first shows there is no cooldown
    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_creative")
    public void creativeIsExempt(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.CREATIVE);
        try (var config = ConfigScope.baseline()) {
            enable();
            List<BlockPos> first = stoneRow(helper, 1, 3);
            SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(first));
            expectAll(helper, first, Blocks.AIR);

            List<BlockPos> second = stoneRow(helper, 1, 3);
            SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(second));
            expectAll(helper, second, Blocks.AIR);
            helper.assertFalse(hasCooldownData(player), "creative breaking starts no cooldown");
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //With stopBeforeToolBreaks a tool must have more uses left than a block costs: 6 uses left mine one block (5 uses)
    //and the other block is skipped instead of breaking the pickaxe. Which of the two is mined first is not fixed: a
    //BlockSet is a HashMap by absolute position, and the test structure's position changes from run to run.
    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_stop_before_break", timeoutTicks = 200)
    public void multiplierNeverBreaksTheTool(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        var config = ConfigScope.baseline();
        enable();
        int multiplier = ServerConfig.balancedBreaking.durabilityMultiplier.get();
        ItemStack pickaxe = player.getInventory().getItem(PICKAXE_SLOT);
        pickaxe.setDamageValue(pickaxe.getMaxDamage() - (multiplier + 1));
        List<BlockPos> row = stoneRow(helper, 1, 2);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, breakSet(row));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(airCount(helper, row) > 0, "waiting for the first mined block"))
                .thenIdle(2)
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "blocks mined (the other one needs more uses than are left)", 1, airCount(helper, row));
                        ItemStack left = player.getInventory().getItem(PICKAXE_SLOT);
                        helper.assertTrue(left.is(Items.IRON_PICKAXE), "the pickaxe must not break, found " + left);
                        expectEquals(helper, "pickaxe uses left", 1, left.getMaxDamage() - left.getDamageValue());
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }

    //Survival replace mining 2 blocks is a balanced operation too: the mined blocks cost the multiplier
    @GameTest(template = EMPTY_STRUCTURE, batch = "balanced_replace", timeoutTicks = 200)
    public void survivalReplaceMiningIsBalanced(GameTestHelper helper) {
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        player.getInventory().setItem(1, new ItemStack(Items.DIRT, 2));
        var config = ConfigScope.baseline();
        enable();
        fabricValue(ServerConfig.survivalReplace.enabled).set(true);
        int multiplier = ServerConfig.balancedBreaking.durabilityMultiplier.get();
        List<BlockPos> row = stoneRow(helper, 1, 2);

        SophisticatedBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, set(
                place(row.get(0), Blocks.DIRT.defaultBlockState()), place(row.get(1), Blocks.DIRT.defaultBlockState())),
                helper.getLevel().getGameTime());

        helper.startSequence()
                .thenWaitUntil(() -> expectAll(helper, row, Blocks.DIRT))
                .thenExecute(() -> {
                    try {
                        expectEquals(helper, "pickaxe damage (2 mined blocks x " + multiplier + ")", 2 * multiplier, pickaxeDamage(player));
                        expectEquals(helper, "dirt left", 0, count(player, Items.DIRT));
                        helper.assertTrue(BalancedBreaking.remainingMillis(player) > 0, "the replace operation should start a cooldown");
                    } finally {
                        config.close();
                        removePlayer(player);
                    }
                })
                .thenSucceed();
    }
}
