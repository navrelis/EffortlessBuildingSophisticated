package sophisticated.building.smoketest.client;

import net.minecraft.client.Minecraft;
import sophisticated.building.smoketest.ModErrorLogCapture;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.SmokeWatchdog;

/**
 * Loader-neutral entry point of the client smoke run. The loader glue calls {@link #init()} from its client
 * initialisation and {@link #onClientTickEnd()} at the end of every client tick; nothing happens unless the run was
 * started by {@code runSmokeClient} (see {@link SmokeTest}).
 * <p>
 * The scenarios run on their own harness thread and drive the game through {@link ClientDriver}. At the end the
 * result is written and the game is asked to stop; a fallback halts the JVM if it does not.
 */
public final class SmokeClient {

    private static boolean started;

    private SmokeClient() {
    }

    public static synchronized void init() {
        if (started || !SmokeTest.isClientMode()) return;
        started = true;
        SmokeTest.LOGGER.info("Sophisticated Building client smoke test enabled, results in {}", SmokeTest.outDir());
        ModErrorLogCapture.install();
        SmokeReport.get().installShutdownHook();
        SmokeWatchdog.start();
        Thread harness = new Thread(SmokeClient::run, "SmokeTest client harness");
        harness.setDaemon(true);
        harness.start();
    }

    public static void onClientTickEnd() {
        if (started) ClientDriver.onClientTickEnd();
    }

    private static void run() {
        SmokeReport report = SmokeReport.get();
        ClientDriver driver;
        try {
            // NeoForge 21.5+ constructs mods before Minecraft exists
            awaitMinecraft();
            driver = new ClientDriver();
        } catch (Throwable error) {
            report.fail("harness.error", error);
            report.finish();
            SmokeWatchdog.haltLater(0, 1);
            return;
        }
        try {
            // As early as the client runs tasks: no sound, and the window on a secondary monitor
            driver.waitUntilRealtime("the client to run tasks", 600, () -> {
                ClientWindow.muteAndMoveAside(Minecraft.getInstance());
                return true;
            });
            new ClientScenarios(driver).runAll();
        } catch (Throwable error) {
            report.fail("harness.error", error);
        }
        ModErrorLogCapture.report("client.no_mod_errors");
        report.finish();

        int exitCode = report.passed() ? 0 : 1;
        SmokeWatchdog.haltLater(60, exitCode);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }

    /** Waits (real time, up to 10 minutes) until the Minecraft instance exists. */
    private static void awaitMinecraft() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 600_000L;
        while (Minecraft.getInstance() == null) {
            if (System.currentTimeMillis() > deadline) throw new AssertionError("The Minecraft client was not created within 600 s");
            Thread.sleep(250);
        }
    }
}
