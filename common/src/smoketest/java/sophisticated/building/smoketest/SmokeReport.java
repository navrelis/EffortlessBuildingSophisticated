package sophisticated.building.smoketest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of one smoke run: named checks plus screenshots, written to {@code <out>/smoketest-result.json} as
 * <pre>{ "passed": bool, "checks": [ { "name", "passed", "detail" } ], "screenshots": [ absolute paths ] }</pre>
 * A skipped check is written with {@code "passed": true}, {@code "skipped": true} and the reason as detail.
 * The file is rewritten after every check, so a crash or a watchdog kill still leaves the checks done so far.
 */
public final class SmokeReport {

    private static final SmokeReport INSTANCE = new SmokeReport();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final class Check {
        private final String name;
        private final boolean passed;
        private final boolean skipped;
        private final String detail;

        public Check(String name, boolean passed, boolean skipped, String detail) {
            this.name = name;
            this.passed = passed;
            this.skipped = skipped;
            this.detail = detail;
        }

        public String name() {
            return name;
        }

        public boolean passed() {
            return passed;
        }

        public boolean skipped() {
            return skipped;
        }

        public String detail() {
            return detail;
        }
    }

    private final List<Check> checks = new ArrayList<>();
    private final List<String> screenshots = new ArrayList<>();
    private boolean finished;
    private boolean shutdownHookInstalled;

    private SmokeReport() {
    }

    public static SmokeReport get() {
        return INSTANCE;
    }

    public synchronized void pass(String name, String detail) {
        add(new Check(name, true, false, detail));
    }

    public synchronized void fail(String name, String detail) {
        add(new Check(name, false, false, detail));
    }

    public synchronized void skip(String name, String reason) {
        add(new Check(name, true, true, "SKIPPED: " + reason));
    }

    public synchronized void fail(String name, Throwable error) {
        fail(name, describe(error));
    }

    public synchronized boolean has(String name) {
        return checks.stream().anyMatch(check -> check.name.equals(name));
    }

    public synchronized void addScreenshot(Path file) {
        screenshots.add(file.toAbsolutePath().normalize().toString());
        write();
    }

    public synchronized boolean passed() {
        return !checks.isEmpty() && checks.stream().allMatch(Check::passed);
    }

    public synchronized int failedCount() {
        return (int) checks.stream().filter(check -> !check.passed).count();
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    /** Final write; later checks (e.g. from the shutdown hook) are ignored. */
    public synchronized void finish() {
        write();
        finished = true;
        SmokeTest.LOGGER.info("Smoke test finished: {} ({} checks, {} failed) -> {}",
                passed() ? "PASSED" : "FAILED", checks.size(), failedCount(), resultFile());
    }

    /**
     * Writes a failing {@code harness.completed} check if the JVM exits (crash, window closed, System.exit by the
     * game) before {@link #finish()} ran.
     */
    public synchronized void installShutdownHook() {
        if (shutdownHookInstalled) return;
        shutdownHookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (SmokeReport.this) {
                if (!finished) {
                    fail("harness.completed", "The game exited before the smoke scenarios finished (crash or external stop); see the game log.");
                    finish();
                }
            }
        }, "SmokeReport shutdown hook"));
    }

    public Path resultFile() {
        return SmokeTest.outDir().resolve(SmokeTest.RESULT_FILE);
    }

    private void add(Check check) {
        if (finished) {
            SmokeTest.LOGGER.warn("Ignoring check {} reported after the run finished", check.name);
            return;
        }
        checks.add(check);
        if (check.passed) {
            SmokeTest.LOGGER.info("[smoketest] {} {}: {}", check.skipped ? "SKIP" : "PASS", check.name, check.detail);
        } else {
            SmokeTest.LOGGER.warn("[smoketest] FAIL {}: {}", check.name, check.detail);
        }
        write();
    }

    private synchronized void write() {
        JsonObject root = new JsonObject();
        root.addProperty("passed", passed());
        JsonArray checkArray = new JsonArray();
        for (Check check : checks) {
            JsonObject json = new JsonObject();
            json.addProperty("name", check.name);
            json.addProperty("passed", check.passed);
            json.addProperty("detail", check.detail == null ? "" : check.detail);
            if (check.skipped) json.addProperty("skipped", true);
            checkArray.add(json);
        }
        root.add("checks", checkArray);
        JsonArray shotArray = new JsonArray();
        screenshots.forEach(shotArray::add);
        root.add("screenshots", shotArray);

        try {
            Path file = resultFile();
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(SmokeTest.RESULT_FILE + ".tmp");
            Files.write(tmp, (GSON.toJson(root)).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SmokeTest.LOGGER.error("Could not write the smoke test result", e);
        }
    }

    public static String describe(Throwable error) {
        if (error instanceof AssertionError && error.getMessage() != null) {
            return error.getMessage();
        }
        StringWriter out = new StringWriter();
        error.printStackTrace(new PrintWriter(out));
        String trace = out.toString();
        return trace.length() > 4000 ? trace.substring(0, 4000) + "..." : trace;
    }
}
