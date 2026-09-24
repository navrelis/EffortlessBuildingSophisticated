package sophisticated.building.smoketest.server;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.smoketest.ModErrorLogCapture;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.SmokeWatchdog;

/**
 * Loader-neutral entry point of the headless server smoke run ({@code runSmokeServer}): a game test server runs the
 * {@link ServerScenarios} as game tests ({@link SmokeServerTests}); this reporter turns every test into a check of
 * the same JSON the client run writes. The game test server exits by itself when all tests are done (exit code =
 * failed required tests), the watchdog covers a hang.
 */
public final class SmokeServer {

    private static boolean started;

    private SmokeServer() {
    }

    /** Called by the loader glue from its mod initialisation. */
    public static synchronized void init() {
        if (started || !SmokeTest.isServerMode()) return;
        started = true;
        SmokeTest.LOGGER.info("Sophisticated Building server smoke test enabled, results in {}", SmokeTest.outDir());
        ModErrorLogCapture.install();
        SmokeReport.get().installShutdownHook();
        SmokeWatchdog.start();
        GlobalTestReporter.replaceWith(new Reporter());
    }

    /** The test instance "sophisticatedbuilding:sb_tier_cap" becomes the check "sb.tier_cap". */
    static String checkName(GameTestInfo info) {
        String name = info.id().getPath();
        String method = name.substring(name.lastIndexOf('.') + 1);
        int separator = method.indexOf('_');
        return separator < 0 ? method : method.substring(0, separator) + "." + method.substring(separator + 1);
    }

    /**
     * Only this mod's test instances are checks. Since 1.21.5 the game test server runs every test instance in the
     * registry, including other namespaces' (vanilla's optional minecraft:always_pass); those are logged only.
     */
    static boolean isSmokeTest(GameTestInfo info) {
        return SophisticatedBuilding.MODID.equals(info.id().getNamespace());
    }

    private static final class Reporter implements TestReporter {
        private final LogTestReporter log = new LogTestReporter();

        @Override
        public void onTestFailed(GameTestInfo info) {
            log.onTestFailed(info);
            if (!isSmokeTest(info)) return;
            Throwable error = info.getError();
            SmokeReport.get().fail(checkName(info), error == null ? "failed" : SmokeReport.describe(error));
        }

        @Override
        public void onTestSuccess(GameTestInfo info) {
            log.onTestSuccess(info);
            if (!isSmokeTest(info)) return;
            String check = checkName(info);
            String skipReason = ServerScenarios.SKIPPED.get(check);
            if (skipReason != null) {
                SmokeReport.get().skip(check, skipReason);
            } else {
                SmokeReport.get().pass(check, ServerScenarios.DETAILS.getOrDefault(check, "passed"));
            }
        }

        @Override
        public void finish() {
            log.finish();
            ModErrorLogCapture.report("server.no_mod_errors");
            SmokeReport.get().finish();
        }
    }
}
