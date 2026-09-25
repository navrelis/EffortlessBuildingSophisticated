package sophisticated.building.smoketest.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.SmokeTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Runs the smoke game tests on a plain dedicated server: Forge 38 (Minecraft 1.18) has no game test server launch
 * target and no {@code RegisterGameTestsEvent}. Does what the vanilla game test server does: the {@code @GameTest}
 * methods of {@link ForgeSmokeServerTests} become test functions (named and batched as vanilla's
 * {@code GameTestRegistry} would, template {@code sophisticatedbuilding:smoketest_empty}), run batch after batch on the
 * surface at the world spawn with their own ticker, and when all are done the reporter is finished (the loader-neutral
 * {@code SmokeServer} writes the result) and the server stops. {@link ForgeSmokeTest} calls {@link #start} when the
 * server has started and {@link #tick} after every server tick.
 */
public final class SmokeServerRunner {

    private static boolean started;
    private static GameTestTicker ticker;
    private static MultipleTestTracker tracker;

    private SmokeServerRunner() {
    }

    public static void start(MinecraftServer server) {
        if (!SmokeTest.isServerMode() || started) return;
        started = true;
        List<TestFunction> tests = testFunctions(ForgeSmokeServerTests.class);
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos origin = new BlockPos(spawn.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()), spawn.getZ());
        // Its own ticker: the vanilla one (GameTestTicker.SINGLETON) is only ticked by a server running in the IDE
        ticker = new GameTestTicker();
        Collection<GameTestInfo> infos = GameTestRunner.runTestBatches(GameTestRunner.groupTestsIntoBatches(tests), origin,
                Rotation.NONE, level, ticker, 8);
        tracker = new MultipleTestTracker(infos);
        SmokeTest.LOGGER.info("Running {} smoke game tests at {} on the dedicated server", tracker.getTotalCount(), origin.toShortString());
    }

    public static void tick(MinecraftServer server) {
        if (tracker == null) return;
        ticker.tick();
        if (!tracker.isDone()) return;
        SmokeTest.LOGGER.info("Smoke game tests done: {} of {} required tests failed", tracker.getFailedRequiredCount(), tracker.getTotalCount());
        tracker = null;
        GlobalTestReporter.finish();
        server.halt(false);
    }

    /** As vanilla's GameTestRegistry: test "<class>.<method>" (lower case), the template in this mod's namespace. */
    private static List<TestFunction> testFunctions(Class<?> holder) {
        String className = holder.getSimpleName().toLowerCase(Locale.ROOT);
        List<TestFunction> tests = new ArrayList<>();
        for (Method method : holder.getDeclaredMethods()) {
            GameTest test = method.getAnnotation(GameTest.class);
            if (test == null) continue;
            tests.add(new TestFunction(test.batch(), className + "." + method.getName().toLowerCase(Locale.ROOT),
                    SophisticatedBuilding.MODID + ":" + test.template(), StructureUtils.getRotationForRotationSteps(test.rotationSteps()),
                    test.timeoutTicks(), test.setupTicks(), test.required(), test.requiredSuccesses(), test.attempts(), invoker(method)));
        }
        tests.sort(Comparator.comparing(TestFunction::getBatchName));
        return tests;
    }

    private static Consumer<GameTestHelper> invoker(Method method) {
        return helper -> {
            try {
                method.invoke(null, helper);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtime) throw runtime;
                throw new RuntimeException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
