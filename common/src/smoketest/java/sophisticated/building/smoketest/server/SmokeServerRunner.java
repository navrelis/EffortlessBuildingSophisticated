package sophisticated.building.smoketest.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.SmokeTest;

import java.util.List;

/**
 * Runs the smoke game tests on a plain dedicated server, for a loader whose game test server does not run tests (Forge
 * 55: its game test launch target starts a normal dedicated server, the game test server is commented out). Does what
 * the vanilla game test server does: every test instance of this mod, batched by environment, spawned on a grid, and
 * when all are done the reporter is finished ({@link SmokeServer} writes the result) and the server stops. The loader
 * glue calls {@link #start} when the server has started and {@link #tick} after every server tick.
 */
public final class SmokeServerRunner {

    private static boolean started;
    private static MultipleTestTracker tracker;

    private SmokeServerRunner() {
    }

    public static void start(MinecraftServer server) {
        if (!SmokeTest.isServerMode() || started) return;
        started = true;
        ServerLevel level = server.overworld();
        List<Holder.Reference<GameTestInstance>> tests = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).listElements()
                .filter(test -> test.key().location().getNamespace().equals(SophisticatedBuilding.MODID))
                .toList();
        // Well above the terrain of a normal world. Placing the (empty) test structures does not clear their space, so the
        // world must be new (the loader build deletes it before every run, as the vanilla game test server does)
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX(), level.getMaxY() - 40, spawn.getZ());
        GameTestRunner runner = GameTestRunner.Builder.fromBatches(GameTestBatchFactory.divideIntoBatches(tests, GameTestBatchFactory.DIRECT, level), level)
                .newStructureSpawner(new StructureGridSpawner(origin, 8, false))
                .build();
        tracker = new MultipleTestTracker(runner.getTestInfos());
        SmokeTest.LOGGER.info("Running {} smoke game tests at {} on the dedicated server", tracker.getTotalCount(), origin.toShortString());
        runner.start();
    }

    public static void tick(MinecraftServer server) {
        if (tracker == null || !tracker.isDone()) return;
        SmokeTest.LOGGER.info("Smoke game tests done: {} of {} required tests failed", tracker.getFailedRequiredCount(), tracker.getTotalCount());
        tracker = null;
        GlobalTestReporter.finish();
        server.halt(false);
    }
}
