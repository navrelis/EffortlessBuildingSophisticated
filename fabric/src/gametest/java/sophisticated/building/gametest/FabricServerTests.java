package sophisticated.building.gametest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.smoketest.servertest.ServerTestRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the {@code @ServerTest}s of the classes listed under the "sophisticatedbuilding-servertest" entrypoint of this
 * source set's fabric.mod.json on the dedicated server of {@code gradlew runGametest}. Fabric API 0.25 for Minecraft
 * 1.16.3 has no game test API (and the game ships its game test framework stripped), so the tests run through
 * {@link ServerTestRunner}; the result is a JUnit report like the one Fabric API writes on the newer branches
 * ({@code -Dsophisticatedbuilding.gametest.report=<file>}), which the Gradle task checks, and the server stops.
 */
public final class FabricServerTests implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("sophisticatedbuilding.gametest");
    private static final String REPORT_PROPERTY = "sophisticatedbuilding.gametest.report";

    private ServerTestRunner runner;

    @Override
    public void onInitialize() {
        String report = System.getProperty(REPORT_PROPERTY);
        if (report == null || report.trim().isEmpty()) return;
        Path reportFile = Paths.get(report).toAbsolutePath();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> start(server, reportFile));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (runner != null && !runner.isFinished()) runner.tick();
        });
    }

    private void start(MinecraftServer server, Path reportFile) {
        List<Class<?>> classes = new ArrayList<>();
        for (EntrypointContainer<Object> container : FabricLoader.getInstance().getEntrypointContainers("sophisticatedbuilding-servertest", Object.class)) {
            classes.add(container.getEntrypoint().getClass());
        }
        runner = new ServerTestRunner(ServerTestRunner.collect(classes.toArray(new Class<?>[0])), new ServerTestRunner.Listener() {
            @Override
            public void onTestDone(ServerTestRunner.Result result) {
            }

            @Override
            public void onAllDone(List<ServerTestRunner.Result> results) {
                long failedRequired = results.stream().filter(r -> !r.passed() && r.required()).count();
                if (failedRequired == 0) {
                    LOGGER.info("All {} required tests passed :)", results.stream().filter(ServerTestRunner.Result::required).count());
                } else {
                    LOGGER.error("{} required tests failed :(", failedRequired);
                }
                writeReport(reportFile, results);
                server.halt(false);
            }
        });
        runner.start(server);
    }

    private static void writeReport(Path file, List<ServerTestRunner.Result> results) {
        long failures = results.stream().filter(r -> !r.passed()).count();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"sophisticatedbuilding-servertests\" tests=\"").append(results.size())
                .append("\" failures=\"").append(failures).append("\">\n");
        for (ServerTestRunner.Result result : results) {
            xml.append("  <testcase name=\"").append(escape(result.name())).append("\" time=\"").append(result.ticks() / 20.0).append("\"");
            if (result.passed()) {
                xml.append("/>\n");
            } else {
                xml.append(">\n    <failure message=\"").append(escape(result.message())).append("\"/>\n  </testcase>\n");
            }
        }
        xml.append("</testsuite>\n");
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, xml.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Could not write the test report {}", file, e);
        }
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
