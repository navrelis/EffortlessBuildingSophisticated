package sophisticated.building.smoketest.servertest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Runs server tests on a running server, the part of vanilla's game test framework that Minecraft 1.16.5 lacks (it
 * ships the framework's classes stripped, and neither loader has a game test API for it). Unlike vanilla, the tests run
 * one after the other, all in the same area near the world spawn, which is emptied before every test: air from the
 * origin layer up, stone below. A test passes when it calls {@link ServerTestHelper#succeed()} (directly or as the last
 * step of a sequence) before its timeout. The loader glue calls {@link #start} once the server has started and
 * {@link #tick} after every server tick; the listener gets every result and, at the end, the whole run.
 */
public final class ServerTestRunner {

    private static final Logger LOGGER = LogManager.getLogger("sophisticatedbuilding.smoketest.servertest");

    /** Horizontal size of the test area and the air height above its origin. */
    private static final int AREA = 16;
    private static final int HEIGHT = 12;

    private final Deque<TestFunction> pending;
    private final Listener listener;
    private final List<Result> results = new ArrayList<>();

    private ServerLevel level;
    private BlockPos origin;
    private TestFunction current;
    private ServerTestHelper helper;
    private long tick;
    private ServerTestAssertException lastWait;
    private boolean finished;

    public ServerTestRunner(List<TestFunction> tests, Listener listener) {
        this.pending = new ArrayDeque<>(tests);
        this.listener = listener;
    }

    /**
     * The {@link ServerTest} methods of the given classes (instance or static, one {@link ServerTestHelper} parameter),
     * named {@code <simple class name>.<method name>} in lower case like vanilla's game tests, sorted by name.
     */
    public static List<TestFunction> collect(Class<?>... testClasses) {
        List<TestFunction> tests = new ArrayList<>();
        for (Class<?> testClass : testClasses) {
            Object[] instance = new Object[1];
            for (Method method : testClass.getDeclaredMethods()) {
                ServerTest annotation = method.getAnnotation(ServerTest.class);
                if (annotation == null) continue;
                if (!Arrays.equals(method.getParameterTypes(), new Class<?>[]{ServerTestHelper.class})) {
                    throw new IllegalArgumentException("Server test " + method + " must take one ServerTestHelper");
                }
                String name = (testClass.getSimpleName() + "." + method.getName()).toLowerCase(Locale.ROOT);
                tests.add(new TestFunction(name, annotation.timeoutTicks(), annotation.required(), helper -> {
                    try {
                        Object target = null;
                        if (!Modifier.isStatic(method.getModifiers())) {
                            if (instance[0] == null) {
                                instance[0] = testClass.getDeclaredConstructor().newInstance();
                            }
                            target = instance[0];
                        }
                        method.invoke(target, helper);
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                        if (cause instanceof Error) throw (Error) cause;
                        throw new RuntimeException(cause);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
        }
        tests.sort(Comparator.comparing(TestFunction::name));
        return tests;
    }

    public void start(MinecraftServer server) {
        level = server.overworld();
        // Away from the world spawn (spawn protection), inside the always loaded spawn chunks, on the surface
        BlockPos spawn = level.getSharedSpawnPos();
        int x = spawn.getX() + 32;
        int z = spawn.getZ() + 32;
        level.getChunkAt(new BlockPos(x, 0, z));
        level.getChunkAt(new BlockPos(x + AREA, 0, z + AREA));
        origin = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
        LOGGER.info("Running {} server tests at {}", pending.size(), ServerTestHelper.shortString(origin));
        startNext();
    }

    public void tick() {
        if (current == null) return;
        tick++;
        try {
            helper.tick(tick);
        } catch (ServerTestAssertException e) {
            lastWait = e;
        } catch (Throwable e) {
            failWith(e);
            return;
        }
        if (helper.hasSucceeded()) {
            finish(true, null);
        } else if (tick >= current.timeoutTicks()) {
            finish(false, lastWait == null ? "Didn't succeed or fail within " + current.timeoutTicks() + " ticks"
                    : "Timed out after " + current.timeoutTicks() + " ticks: " + lastWait.getMessage());
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public List<Result> results() {
        return Collections.unmodifiableList(results);
    }

    private void startNext() {
        current = pending.poll();
        if (current == null) {
            finished = true;
            listener.onAllDone(results());
            return;
        }
        clearArea();
        helper = new ServerTestHelper(level, origin);
        tick = 0;
        lastWait = null;
        try {
            current.body().accept(helper);
        } catch (ServerTestAssertException e) {
            // Vanilla fails a test whose function throws, assertions included
            finish(false, e.getMessage());
            return;
        } catch (Throwable e) {
            failWith(e);
            return;
        }
        if (helper.hasSucceeded()) {
            finish(true, null);
        }
    }

    private void finish(boolean passed, String message) {
        Result result = new Result(current.name(), passed, current.required(), message, tick);
        results.add(result);
        if (passed) {
            LOGGER.info("{} passed after {} ticks", result.name(), tick);
        } else {
            LOGGER.error("{} failed after {} ticks: {}", result.name(), tick, message);
        }
        listener.onTestDone(result);
        current = null;
        helper = null;
        startNext();
    }

    /** Air from the origin layer up (entities other than players removed), a stone floor below, like vanilla. */
    private void clearArea() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -2; dx < AREA + 2; dx++) {
            for (int dz = -2; dz < AREA + 2; dz++) {
                for (int dy = -1; dy <= HEIGHT; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    level.setBlock(pos, dy < 0 ? stone : air, 2 /* Block.UPDATE_CLIENTS */);
                }
            }
        }
        AABB box = new AABB(origin.offset(-2, -1, -2), origin.offset(AREA + 2, HEIGHT + 1, AREA + 2));
        for (Entity entity : level.getEntities((Entity) null, box, entity -> !(entity instanceof Player))) {
            entity.remove();
        }
    }

    /** An unexpected exception (not an assertion) fails the test; its stack trace goes to the log. */
    private void failWith(Throwable error) {
        LOGGER.error("{} threw", current.name(), error);
        finish(false, describe(error));
    }

    private static String describe(Throwable error) {
        return error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage());
    }

    /** One test: its name, timeout in ticks, whether its failure fails the run, and its body. */
    public static final class TestFunction {
        private final String name;
        private final int timeoutTicks;
        private final boolean required;
        private final Consumer<ServerTestHelper> body;

        public TestFunction(String name, int timeoutTicks, boolean required, Consumer<ServerTestHelper> body) {
            this.name = name;
            this.timeoutTicks = timeoutTicks;
            this.required = required;
            this.body = body;
        }

        public String name() {
            return name;
        }

        public int timeoutTicks() {
            return timeoutTicks;
        }

        public boolean required() {
            return required;
        }

        public Consumer<ServerTestHelper> body() {
            return body;
        }
    }

    public static final class Result {
        private final String name;
        private final boolean passed;
        private final boolean required;
        private final String message;
        private final long ticks;

        Result(String name, boolean passed, boolean required, String message, long ticks) {
            this.name = name;
            this.passed = passed;
            this.required = required;
            this.message = message;
            this.ticks = ticks;
        }

        public String name() {
            return name;
        }

        public boolean passed() {
            return passed;
        }

        public boolean required() {
            return required;
        }

        /** The failure, null for a passed test. */
        public String message() {
            return message;
        }

        public long ticks() {
            return ticks;
        }
    }

    public interface Listener {
        void onTestDone(Result result);

        void onAllDone(List<Result> results);
    }
}
