package sophisticated.building.smoketest.server;

import net.minecraft.server.MinecraftServer;
import sophisticated.building.smoketest.ModErrorLogCapture;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.SmokeWatchdog;
import sophisticated.building.smoketest.backpack.SmokeBackpacks;
import sophisticated.building.smoketest.servertest.ServerTestHelper;
import sophisticated.building.smoketest.servertest.ServerTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loader-neutral entry point of the headless server smoke run ({@code runSmokeServer}): a dedicated server on a fresh
 * superflat world runs the {@link ServerScenarios} as server tests (Minecraft 1.16.5 has no game test framework, see
 * {@link ServerTestRunner}) and every test becomes a check of the same JSON the client run writes. When all are done the
 * result is written and the server stops; the watchdog covers a hang. The loader glue calls {@link #init} from its mod
 * initialisation, {@link #start} once the server has started and {@link #tick} after every server tick.
 */
public final class SmokeServer {

    private static boolean started;
    private static ServerTestRunner runner;

    private SmokeServer() {
    }

    public static synchronized void init() {
        if (started || !SmokeTest.isServerMode()) return;
        started = true;
        SmokeTest.LOGGER.info("Sophisticated Building server smoke test enabled, results in {}", SmokeTest.outDir());
        ModErrorLogCapture.install();
        SmokeReport.get().installShutdownHook();
        SmokeWatchdog.start();
    }

    public static void start(MinecraftServer server) {
        if (!started || runner != null) return;
        runner = new ServerTestRunner(scenarios(), new ServerTestRunner.Listener() {
            @Override
            public void onTestDone(ServerTestRunner.Result result) {
                report(result);
            }

            @Override
            public void onAllDone(List<ServerTestRunner.Result> results) {
                SmokeTest.LOGGER.info("Server smoke scenarios done: {} of {} failed", results.stream().filter(r -> !r.passed()).count(), results.size());
                ModErrorLogCapture.report("server.no_mod_errors");
                SmokeReport.get().finish();
                server.halt(false);
            }
        });
        runner.start(server);
    }

    public static void tick() {
        if (runner != null && !runner.isFinished()) {
            runner.tick();
        }
    }

    /** One test per scenario, in this order; the Sophisticated Backpacks scenarios only where the fixture exists. */
    private static List<ServerTestRunner.TestFunction> scenarios() {
        List<ServerTestRunner.TestFunction> tests = new ArrayList<>();
        add(tests, "server_place_line_survival", ServerScenarios::server_place_line_survival);
        add(tests, "server_undo_redo", ServerScenarios::server_undo_redo);
        add(tests, "server_merge_undo_refund", ServerScenarios::server_merge_undo_refund);
        add(tests, "server_refused_place_not_charged", ServerScenarios::server_refused_place_not_charged);
        if (SmokeBackpacks.find().isPresent()) {
            add(tests, "sb_upgrade_supplies_blocks", ServerScenarios::sb_upgrade_supplies_blocks);
            add(tests, "sb_disabled_upgrade_ignored", ServerScenarios::sb_disabled_upgrade_ignored);
            add(tests, "sb_tier_cap", ServerScenarios::sb_tier_cap);
            add(tests, "sb_tool_swapper_tools", ServerScenarios::sb_tool_swapper_tools);
            add(tests, "sb_worn_backpack_chest", ServerScenarios::sb_worn_backpack_chest);
            add(tests, "sb_worn_backpack", ServerScenarios::sb_worn_backpack);
        }
        return tests;
    }

    private static void add(List<ServerTestRunner.TestFunction> tests, String name, Consumer<ServerTestHelper> body) {
        tests.add(new ServerTestRunner.TestFunction(name, ServerScenarios.TIMEOUT_TICKS, true, body));
    }

    /** "sb_tier_cap" becomes the check "sb.tier_cap". */
    static String checkName(String testName) {
        int separator = testName.indexOf('_');
        return separator < 0 ? testName : testName.substring(0, separator) + "." + testName.substring(separator + 1);
    }

    private static void report(ServerTestRunner.Result result) {
        String check = checkName(result.name());
        if (!result.passed()) {
            SmokeReport.get().fail(check, result.message());
            return;
        }
        String skipReason = ServerScenarios.SKIPPED.get(check);
        if (skipReason != null) {
            SmokeReport.get().skip(check, skipReason);
        } else {
            SmokeReport.get().pass(check, ServerScenarios.DETAILS.getOrDefault(check, "passed"));
        }
    }
}
