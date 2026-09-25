package sophisticated.building.smoketest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Switches and settings of the in-game smoke test harness. Everything is driven by system properties that only the
 * {@code runSmokeClient} / {@code runSmokeServer} Gradle tasks set, so the harness is inert in any other run even when
 * its classes happen to be on the classpath.
 *
 * <ul>
 *     <li>{@code sophisticatedbuilding.smoketest.out}: absolute directory for {@code smoketest-result.json} and the
 *     screenshots. Its presence enables the harness.</li>
 *     <li>{@code sophisticatedbuilding.smoketest.mode}: {@code client} or {@code server}.</li>
 *     <li>{@code sophisticatedbuilding.smoketest.timeoutSeconds}: internal watchdog (default 300).</li>
 * </ul>
 */
public final class SmokeTest {

    public static final String MOD_ID = "sophisticatedbuilding_smoketest";
    public static final Logger LOGGER = LogManager.getLogger("sophisticatedbuilding.smoketest");

    public static final String PROP_OUT = "sophisticatedbuilding.smoketest.out";
    public static final String PROP_MODE = "sophisticatedbuilding.smoketest.mode";
    public static final String PROP_TIMEOUT = "sophisticatedbuilding.smoketest.timeoutSeconds";

    public static final String RESULT_FILE = "smoketest-result.json";

    private SmokeTest() {
    }

    public static boolean isEnabled() {
        String out = System.getProperty(PROP_OUT);
        return out != null && !out.trim().isEmpty();
    }

    public static boolean isClientMode() {
        return isEnabled() && "client".equalsIgnoreCase(System.getProperty(PROP_MODE, "client"));
    }

    public static boolean isServerMode() {
        return isEnabled() && "server".equalsIgnoreCase(System.getProperty(PROP_MODE, ""));
    }

    public static Path outDir() {
        return Paths.get(System.getProperty(PROP_OUT)).toAbsolutePath().normalize();
    }

    /** The first implementation of the service registered in {@code META-INF/services}, or empty. */
    public static <T> Optional<T> firstService(Class<T> type) {
        Iterator<T> services = ServiceLoader.load(type, type.getClassLoader()).iterator();
        return services.hasNext() ? Optional.of(services.next()) : Optional.empty();
    }

    /** Every implementation of the service registered in {@code META-INF/services}. */
    public static <T> List<T> allServices(Class<T> type) {
        List<T> all = new ArrayList<>();
        ServiceLoader.load(type, type.getClassLoader()).forEach(all::add);
        return all;
    }

    public static int timeoutSeconds() {
        try {
            return Math.max(30, Integer.parseInt(System.getProperty(PROP_TIMEOUT, "300").trim()));
        } catch (NumberFormatException e) {
            return 300;
        }
    }
}
