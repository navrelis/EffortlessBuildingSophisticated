package sophisticated.building.smoketest.forge;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.ServerScenarios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The server smoke scenarios as vanilla game tests (runSmokeServer). Forge 1.17.1 has neither a game test server launch
 * target nor {@code @GameTestHolder} (both arrive with Forge 38 for 1.18), so runSmokeServer starts a plain dedicated
 * dev server on a fresh superflat world and this class runs the scenarios once it has started, the way the vanilla
 * game test server does: one test function per scenario, each in its own batch so they run one after the other, in the
 * empty template data/sophisticatedbuilding/structures/smoketest_empty.nbt, ticked by the vanilla GameTestTicker. Test
 * names end in the scenario method name, so {@link sophisticated.building.smoketest.server.SmokeServer}'s reporter
 * turns e.g. {@code sb_tier_cap} into the check {@code sb.tier_cap}. When every test is done the reporter writes the
 * result and the server stops.
 */
final class ForgeSmokeServerTests {

    private static final String TEMPLATE = SophisticatedBuilding.MODID + ":smoketest_empty";
    private static final String TEST_CLASS = "forgesmokeservertests";

    private static MultipleTestTracker tracker;

    private ForgeSmokeServerTests() {
    }

    private static Map<String, Consumer<GameTestHelper>> scenarios() {
        Map<String, Consumer<GameTestHelper>> scenarios = new LinkedHashMap<>();
        scenarios.put("server_place_line_survival", ServerScenarios::server_place_line_survival);
        scenarios.put("server_undo_redo", ServerScenarios::server_undo_redo);
        scenarios.put("sb_upgrade_supplies_blocks", ServerScenarios::sb_upgrade_supplies_blocks);
        scenarios.put("sb_disabled_upgrade_ignored", ServerScenarios::sb_disabled_upgrade_ignored);
        scenarios.put("sb_tier_cap", ServerScenarios::sb_tier_cap);
        scenarios.put("sb_tool_swapper_tools", ServerScenarios::sb_tool_swapper_tools);
        scenarios.put("sb_worn_backpack_chest", ServerScenarios::sb_worn_backpack_chest);
        scenarios.put("sb_worn_backpack", ServerScenarios::sb_worn_backpack);
        return scenarios;
    }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(ForgeSmokeServerTests::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(ForgeSmokeServerTests::onServerTick);
    }

    private static void onServerStarted(FMLServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        // The game rules of the vanilla game test server
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);

        List<GameTestBatch> batches = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, Consumer<GameTestHelper>> scenario : scenarios().entrySet()) {
            String batch = "smoke_" + index++;
            TestFunction function = new TestFunction(batch, TEST_CLASS + "." + scenario.getKey(), TEMPLATE,
                    ServerScenarios.TIMEOUT_TICKS, 0L, true, scenario.getValue());
            batches.add(new GameTestBatch(batch, Collections.singletonList(function), l -> {
            }, l -> {
            }));
        }
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()), spawn.getZ());
        SmokeTest.LOGGER.info("Running {} server smoke scenarios at {}", batches.size(), origin);
        tracker = new MultipleTestTracker(GameTestRunner.runTestBatches(batches, origin, Rotation.NONE, level, GameTestTicker.SINGLETON, 8));
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tracker == null) return;
        // The server ticks the game tests itself only when started from Mojang's IDE setup
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestTicker.SINGLETON.tick();
        }
        if (tracker.isDone()) {
            SmokeTest.LOGGER.info("Server smoke scenarios done: {}", tracker.getProgressBar());
            tracker = null;
            GlobalTestReporter.finish();
            ServerLifecycleHooks.getCurrentServer().halt(false);
        }
    }
}
