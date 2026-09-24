package sophisticated.building.smoketest;

/**
 * Keeps a smoke run from hanging: after {@link SmokeTest#timeoutSeconds()} the result is written with a failing
 * {@code harness.watchdog} check and the JVM is halted. {@link #haltLater} is the fallback for a game that does not
 * shut down by itself after the result was written.
 */
public final class SmokeWatchdog {

    private SmokeWatchdog() {
    }

    public static void start() {
        int timeout = SmokeTest.timeoutSeconds();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(timeout * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            SmokeReport report = SmokeReport.get();
            if (!report.isFinished()) {
                report.fail("harness.watchdog", "The smoke run did not finish within " + timeout + " s (internal watchdog); see the game log.");
                report.finish();
            }
            SmokeTest.LOGGER.error("Smoke test watchdog: halting the game after {} s", timeout);
            Runtime.getRuntime().halt(3);
        }, "SmokeTest watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    /** Halts the JVM after the delay unless it exited by itself before. */
    public static void haltLater(int seconds, int exitCode) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            SmokeTest.LOGGER.warn("The game did not exit {} s after the smoke test finished; halting it", seconds);
            Runtime.getRuntime().halt(exitCode);
        }, "SmokeTest exit fallback");
        thread.setDaemon(true);
        thread.start();
    }
}
